# Android Local-First Multi-User Design

## Problem Statement

Many field engineers will use the same Android device. Current app assumes a single implicit user (`userId = 1L` hardcoded in several ViewModels). FMv2 must support secure multi-user operation without data leakage between sessions.

---

## Core Principle

**Never assume 1 device = 1 user.**

Each local operation must be attributable to:
- `localUserId` — Room `users.id`
- `serverUserId` — server auth subject
- `userUuid` — global user identity
- `deviceId` — registered device UUID
- `sessionId` — login session UUID
- `projectUuid` — for transaction/project sync scope

---

## Session Model

```
┌─────────────────────────────────────┐
│ SessionState (DataStore + Room)     │
├─────────────────────────────────────┤
│ activeSessionId                     │
│ activeLocalUserId                   │
│ activeServerUserId                  │
│ activeUserUuid                      │
│ deviceId                            │
│ loginAt / expiresAt                 │
│ permissionSnapshotVersion           │
│ pendingSyncCount                    │
└─────────────────────────────────────┘
```

### Login Flow (Phase 2)
1. User enters credentials (or selects cached profile)
2. Server returns token + userUuid + serverUserId + permissions + project assignments
3. Create/update local `UserEntity` mapped to server identity
4. Start session; set DataStore active user
5. Cache project assignments and permission snapshot

### Switch User Flow
1. Check pending sync for current session
2. If pending > 0: warn user (allow force switch with audit log)
3. End current session (do not delete outbox)
4. Activate new session scope

### Logout Flow
1. Warn if pending sync or draft transactions
2. Clear active session from DataStore
3. Retain local data and outbox for re-login

---

## Data Isolation Rules

| Data | Scope |
|------|-------|
| Projects | Filter by assignment cache + localUserId |
| Transactions | Must carry sessionId + userUuid on create |
| Sync outbox | Filtered by localUserId + deviceId + sessionId |
| Attachment queue | Same as outbox scope |
| Audit logs | Tag with deviceId + sessionId |

**Rule:** Never send User A's outbox while User B is active.

---

## Existing Foundation

Already present:
- `UserEntity` + `UserDao`
- `UserPreferencesRepository.activeUserId` (DataStore)
- `userId` FK on `ProjectEntity` and `TransactionEntity`
- Seed user `id=1, name='Local User'`

Gaps:
- ViewModels default `userId = 1L` ~~RESOLVED~~ Removed from `ProjectListViewModel` and `TransactionFormViewModel`; `UserPreferencesRepository` wired
- No login UI (deferred to Phase 2)
- No device registration (deferred to Phase 4+ backend)
- No session entity (deferred to Phase 2)
- Accounts/categories not user-scoped

---

## Migration Path from Single-User

1. Phase 1: Add uuid columns; backfill uuid for existing rows
2. Phase 2: Wire `UserPreferencesRepository` to all ViewModels via `ActiveSessionProvider`
3. Map existing `Local User` to first registered server user on first login
4. Do not delete local user id=1 data — reassign ownership if needed via migration script

---

## UI Requirements (Phase 2)

- Profile selector on Settings or pre-dashboard gate
- Session indicator in app bar (user name + sync status)
- Logout with pending sync dialog
- Assigned projects only in project list

---

## Security Notes

- Tokens stored in EncryptedSharedPreferences or Android Keystore
- No password stored locally
- Permission snapshot refreshed on login and periodic pull
- Failed auth clears session but keeps offline data
