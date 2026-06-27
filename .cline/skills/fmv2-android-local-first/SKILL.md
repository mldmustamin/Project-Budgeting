# FMv2 Android Local-First

Use this skill for Android session, user scope, and local-first behavior.

## Read first
- docs/01_EXISTING_ANDROID_COMPATIBILITY.md
- docs/04_ANDROID_LOCAL_FIRST_MULTI_USER.md

## Rules
- Never assume a single implicit user.
- Treat session scope, device scope, and user scope separately.
- Keep offline data durable across logout unless docs say otherwise.
- Do not leak another user's cached scope into the active session.

## Preferred changes
- Session-aware ViewModel wiring
- DataStore-backed active session handling
- User filtering in repositories and UI
- Additive fields and migrations only
