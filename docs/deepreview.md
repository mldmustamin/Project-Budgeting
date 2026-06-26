# Deep Code Review - Funds Manager

Scope reviewed:
- Repository and Room layer
- Import/export use cases
- Summary and reporting use cases
- Core ViewModels for project, report, and transaction screens

Validation note:
- I could not run Gradle tests in this environment because no Java runtime is installed (`Unable to locate a Java Runtime`), so this review is static-analysis based.

## Findings

### 1. [High] Project and account lookups ignore `userId`, so data can collide across users

The repository API accepts `userId` for project/account creation, but the actual lookup queries are not user-scoped. `getOrCreateProject()` searches by name only, and `getOrCreateAccount()` / `getDefaultCashAccount()` do the same for accounts. That means two users with the same project or account name will share the same row, and imported data can land in the wrong logical tenant.

Relevant code:
- [FundsRepositoryImpl.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/data/repository/FundsRepositoryImpl.kt#L50)
- [ProjectDao.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/data/local/dao/ProjectDao.kt#L15)
- [AccountDao.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/data/local/dao/AccountDao.kt#L12)

Why this matters:
- It breaks isolation between users even though `UserEntity` and `userId` fields are present throughout the model.
- It also makes the "Cash in Hand" default account globally shared.

Suggested fix:
- Add `userId` to account/category ownership, or remove the multi-user surface entirely if the app is intentionally single-user.
- Filter `getProjectByName()` and `getAccountByName()` by `userId`, and make `getDefaultCashAccount()` user-specific.

### 2. [High] The tracker-duit import path drops legacy metadata

The legacy import code parses `LegacyProject.description` and expense `category` fields, but `executeImport()` never persists them. Imported projects are created with only a name, and every imported transaction is stored with `categoryId = null`.

Relevant code:
- [ImportTrackerDuitUseCase.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/domain/usecase/ImportTrackerDuitUseCase.kt#L38)
- [ImportTrackerDuitUseCase.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/domain/usecase/ImportTrackerDuitUseCase.kt#L82)
- [LegacyModels.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/domain/model/LegacyModels.kt#L14)

Why this matters:
- Data that exists in the legacy backup is silently lost during import.
- Users will not see imported project descriptions or categories in reports, so the import is not a faithful migration.

Suggested fix:
- Persist `LegacyProject.description` when creating or updating projects.
- Map legacy expense categories into `CategoryEntity` / `categoryId` during import instead of dropping them.
- If category mapping is intentionally deferred, surface it clearly in the preview so the user knows data will be lost.

### 3. [Medium] Report and transaction screens read once and then go stale

Several viewmodels snapshot the database once with `first()` and never observe the underlying flows again. `ReportHomeViewModel`, `GlobalTransactionViewModel`, and `TransactionListViewModel` all follow this pattern, so totals and lists can become stale after edits unless the screen is recreated or manually reloaded.

Relevant code:
- [ReportHomeViewModel.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/ui/screen/home/ReportHomeViewModel.kt#L35)
- [GlobalTransactionViewModel.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/ui/screen/home/GlobalTransactionViewModel.kt#L88)
- [TransactionListViewModel.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/ui/screen/transaction/TransactionListViewModel.kt#L112)

Why this matters:
- A user can edit, import, or delete data in one screen and then navigate back to a stale dashboard or report view.
- This is especially visible in a finance app, where users expect totals to update immediately.

Suggested fix:
- Prefer collecting Room flows directly for project and transaction lists.
- Recompute summaries in response to those emissions rather than taking one-time snapshots.
- If one-shot loading is intentional, add explicit refresh triggers on return navigation and after all mutating actions.

### 4. [Medium] Project list refresh work can race and overwrite newer state

`ProjectListViewModel.observeProjects()` uses `collectLatest`, but `refreshProjectItems()` immediately launches a new coroutine instead of doing the refresh work in the same coroutine. If the project flow emits again before the previous summary calculations finish, the earlier job is not cancelled and can write stale UI state after the newer one.

Relevant code:
- [ProjectListViewModel.kt](/Users/fiyyalisanna/AndroidStudioProjects/FundsManager/app/src/main/java/com/example/fundsmanager/ui/screen/project/ProjectListViewModel.kt#L137)

Why this matters:
- The race is subtle, but it can show the wrong project counts or summaries when the dataset is large or emits frequently.
- Because each refresh calculates summaries sequentially per project, the window for stale overwrite is real.

Suggested fix:
- Make `refreshProjectItems()` suspend work inside the `collectLatest` block, or keep a reference to the active refresh job and cancel it before starting a new one.
- Consider computing summaries in parallel if the dataset grows, but only after the cancellation behavior is correct.

## Overall assessment

The codebase is structurally solid and the domain/use-case split is clean, but the review surfaced a few correctness issues that are worth fixing before relying on the app for real migration/reporting work. The highest-risk problems are tenant leakage through user-agnostic lookups and silent data loss in the legacy import path.
