# FundManager V2 — Product Scope

## Vision

FundManager V2 evolves the existing offline Android app into a multi-user field client backed by a server-authoritative finance platform and advanced web dashboard.

```
Android  = multi-user local-first field client
Web      = advanced finance control center
API      = identity-aware bidirectional sync bridge
Server   = authority for user, role, project assignment, device,
             approval, audit, closing, reconciliation, settlement
```

---

## Android (Field Engineer)

**Primary users:** FIELD_ENGINEER, PIC (field operations)

**Capabilities:**
- Input transactions (fund in, office expense, personal expense)
- Attach proof (camera/file)
- Full offline operation
- Sync when online
- View approval status: pending / approved / need revision / rejected
- Switch user on shared device
- View assigned projects only

**Non-goals on Android:**
- Period closing
- Bulk reconciliation
- User/role management
- Direct edit of approved transactions

---

## Web Dashboard (Finance Control Center)

**Primary users:** OWNER, ADMIN, FINANCE_MANAGER, SUPERVISOR, AUDITOR

**Modules:**
| Module | Purpose |
|--------|---------|
| Dashboard | Org-wide KPIs, pending approvals count |
| Project | Project CRUD, assignment, archive |
| Saldo Project | Balance by calculation mode |
| Transaction | Search, filter, drill-down |
| Approval Center | Approve / reject / need revision |
| Reconciliation | Match reported vs real vs bank |
| Settlement PIC | PIC settlement workflow |
| Transfer | Inter-project / inter-account transfers |
| Budget | Budget vs actual |
| Reports | Advanced reporting beyond mobile export |
| Sync Monitor | Device sync health, conflict queue |
| Audit Trail | Immutable action log |
| User Management | CRUD users, roles |
| Device Management | Register/revoke devices |
| Project Assignment | Map users to projects |

---

## Server Authority

Server is authoritative for:
- User identity and authentication
- Role and permission assignment
- Project membership
- Device registration and authorization
- Transaction approval state (post-sync)
- Financial period closing
- Reconciliation and settlement records
- Audit log for sensitive web actions

Android remains authoritative for:
- Immediate local capture while offline
- Local draft state before sync
- Local attachment files until uploaded

---

## Out of Scope (Phase 0)

- Implementation of any feature above
- Backend or web code
- Breaking changes to existing Android flows

---

## Success Criteria (V2 Complete)

1. Multiple field engineers share one Android device safely
2. Offline capture never blocked by network
3. Approved transactions immutable; corrections auditable
4. Web can close periods and reconcile without corrupting field data
5. Existing Android users upgrade without data loss
6. Summary formula backward-compatible in LOCAL_VIEW mode
