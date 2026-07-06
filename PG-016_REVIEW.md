# PG-016 Frontend Rereview

Reviewed workspace: `C:\Users\marti\IdeaProjects\RankingGame`

Review date: 2026-06-15

Reviewed range:
- Frontend changes from `63e964f` through current workspace
- Follow-up fixes for the prior P1/P2 findings

Verification run:
- `npm.cmd run build`
- `npm.cmd test -- --watch=false --browsers=ChromeHeadless`

Result:
- Production build passed.
- Frontend tests passed: `17 SUCCESS`.

## Findings

No open frontend findings remain from this review pass.

## Resolved Items

### Resolved - Production build API configuration

Files:
- `frontend/angular.json`
- `frontend/src/environments/environment.prod.ts`

Production builds now replace `environment.ts` with `environment.prod.ts`. The production API base URL no longer points to localhost; it uses `/api`.

### Resolved - Whitespace-only player names

Files:
- `frontend/src/app/shared/validators/not-blank.validator.ts`
- `frontend/src/app/home/create-room/create-room.ts`
- `frontend/src/app/home/join-room/join-room.ts`
- `frontend/src/app/home/create-room/create-room.spec.ts`
- `frontend/src/app/home/join-room/join-room.spec.ts`

A shared trim-aware validator now rejects whitespace-only names. Create and join form specs cover this behavior.

### Resolved - RoomApiService HTTP-boundary tests

Files:
- `frontend/src/app/core/api/room-api.service.spec.ts`

The service now has HTTP tests for create and join requests, including method, URL, encoded room code, and request body.

## Notes

- `Home` remains a container component and delegates forms to `CreateRoom` and `JoinRoom`.
- Room code validation remains centralized in `ROOM_CODE_PATTERN`.
- The lobby route is still a PG-016 navigation stub; full lobby loading belongs to PG-017.

## Residual Risk

The remaining risk is outside PG-016 frontend scope: the backend room endpoints still need to exist and match the agreed DTO contract for true end-to-end behavior.
