# MClubClone – Project Architecture & Logic (as-understood)

> Scope: This document summarizes the **current** architecture and business logic of the MClubClone Spring Boot application, based on:
> - project structure under `src/main/java/io/droidevs/mclub`
> - runtime configuration (`application.yml`)
> - DB baseline migration (`src/main/resources/db/migration/V1__schema.sql`)
> - existing project docs (`docs/*`, `README-api.md`, `UI-ATTENDANCE-RATING.md`)
>
> Note: A few existing docs contain older notes (for example: rating “only after event ends”). When code differs, this doc treats **code as source of truth**.

---

## Table of contents

1. [What the system does](#what-the-system-does)
2. [Tech stack & runtime](#tech-stack--runtime)
3. [High-level architecture (layers)](#high-level-architecture-layers)
4. [Packages / modules map](#packages--modules-map)
5. [Request flows (Web UI, REST API, Webhooks)](#request-flows-web-ui-rest-api-webhooks)
6. [Core domain features & rules](#core-domain-features--rules)
7. [Database schema & data model](#database-schema--data-model)
8. [Security model](#security-model)
9. [Server-rendered UI (Thymeleaf)](#server-rendered-ui-thymeleaf)
10. [AI / RAG WhatsApp assistant](#ai--rag-whatsapp-assistant)
11. [Configuration & operations](#configuration--operations)
12. [Known gaps / next improvements](#known-gaps--next-improvements)

---

## What the system does

MClub is a club management system with:

- Clubs (create/manage)
- Memberships (join request + approval; club-scoped roles)
- Events (create, register)
- Attendance / check-in (QR token windows + manual organizer check-in)
- Event ratings (students rate attended events)
- Comments (for events and activities; threaded replies + likes)
- Club applications (students apply to create a club; platform admin review)
- Optional: WhatsApp conversational assistant (RAG + tools, currently stubbed LLM)

The application exposes:

- A **server-rendered** web UI (Thymeleaf templates; URLs like `/clubs`, `/events/{id}`, `/club-admin/**`).
- A **REST API** under `/api/**` used by UI JavaScript and external clients.
- A **webhook entrypoint** under `/webhooks/whatsapp` for the conversational assistant.

---

## Tech stack & runtime

- **Java** (Gradle toolchain set to **Java 25**) – see `build.gradle`
- **Spring Boot 4.0.5**
- Spring MVC (REST + view controllers)
- Spring Data JPA + Hibernate
- Flyway migrations (baseline `V1__schema.sql`)
- Spring Security with **JWT** (token from header or `jwtToken` cookie)
- Thymeleaf + Spring Security extras
- PostgreSQL (compose file uses postgres:16-alpine)
- MapStruct (mapper layer)
- Lombok
- Spring Modulith dependency BOM present (modularization support)

---

## High-level architecture (layers)

Typical request path is:

1. **Controller layer** (`io.droidevs.mclub.controller`)
   - Web: `@Controller` returns template names.
   - API: `@RestController` returns JSON.
2. **Service layer** (`io.droidevs.mclub.service`)
   - Business rules, authorization, transactional boundaries.
3. **Repository layer** (`io.droidevs.mclub.repository`)
   - JPA repositories + custom fetch queries (important because `open-in-view: false`).
4. **Domain layer** (`io.droidevs.mclub.domain`)
   - Entities + enums (Role, membership roles/status, comment target type, etc.).
5. **DTO + mapping** (`io.droidevs.mclub.dto`, `io.droidevs.mclub.mapper`)
   - API and UI use DTOs to avoid leaking entities.
6. **Cross-cutting**
   - `io.droidevs.mclub.security` for JWT and route protection.
   - Exceptions (`io.droidevs.mclub.exception`) for 403/404 semantics.

Important design choice:

- `spring.jpa.open-in-view: false` (in `application.yml`).
  - Templates must not lazily navigate unloaded relations.
  - Services/repositories are expected to eagerly fetch what templates/APIs need.

---

## Packages / modules map

Top level packages under `io.droidevs.mclub`:

- `controller/` – web + api controllers
- `service/` – business logic
- `repository/` – persistence
- `domain/` – JPA entities & enums
- `dto/` – request/response/data transfer objects
- `mapper/` – MapStruct mappers
- `security/` – JWT, SecurityFilterChain config, UserDetailsService
- `exception/` – consistent error types

AI module (`io.droidevs.mclub.ai`):

- `ai/webhook/whatsapp` – inbound webhook controller + sender adapter
- `ai/conversation` – session memory, identity context, async processing
- `ai/rag` – retrieval + prompt building + LLM decision + tool execution
- `ai/retrieval` – current structured retrieval (no embeddings)
- `ai/tools` – tool interfaces and implementations calling existing services

---

## Request flows (Web UI, REST API, Webhooks)

### 1) Web UI (Thymeleaf)

Examples:

- `GET /clubs` → `WebController.clubs(...)` → `ClubService` → renders `clubs.html`
- `GET /events/{id}` → `WebController.eventDetail(...)` → `EventService`, plus rating summary + comment preview → renders `event-detail.html`
- `POST /events/{eventId}/register` → web controller (`WebEventRegistrationController`) → `RegistrationService.register(...)` → redirect

Club admin area:

- URLs prefix `/club-admin/**`
- UI controllers enforce **club-scoped** authorization (membership role ADMIN/STAFF) either directly or via services (e.g., `eventService.requireCanManageEvent`).

### 2) REST API

Common patterns:

- `/api/auth/**` endpoints are public for login/register.
- Event ratings:
  - `POST /api/events/{eventId}/ratings` requires `STUDENT`
  - `GET /api/events/{eventId}/ratings/summary` is public
- Comments:
  - `/api/comments/**` is public for reads
  - writes are typically restricted to `STUDENT` (enforced in controller and again in service)

### 3) WhatsApp webhook flow

- Provider calls: `POST /webhooks/whatsapp`
- `WhatsAppWebhookController.receive(...)` forwards request to `ConversationService.handleIncomingMessage` (async)
- `ConversationService`:
  - loads session from `ConversationStore`
  - builds `ConversationContext` via `WhatsappIdentityService`
  - delegates to `RagService`
  - stores assistant response
  - sends response using `WhatsAppSender` (current default is logging sender)

---

## Core domain features & rules

This section summarizes the key rules that are explicitly enforced in services (so they apply regardless of UI/REST client).

### Clubs & memberships

Concepts:

- A user has a **global** role (platform-wide) and a **per-club** membership role.
- Membership has status (pending/approved/rejected).

Authorization style:

- “club admin pages” require authentication at route level.
- “club admin actions” are usually enforced via `ClubAuthorizationService` and/or membership checks.

### Events & registrations

- Users register for events via a registration record (`event_registrations`).
- Some actions are only available to registered users (attendance, rating).

### Attendance (QR check-in windows)

Backed by:

- `event_attendance_windows`:
  - one window per event (`unique(event_id)`)
  - `token_hash` is stored (raw token is never stored)
  - `active` + time bounds relative to event start
- `event_attendance`:
  - unique `(event_id, user_id)` (idempotent check-in)

Key rules (from `AttendanceService`):

- Organizer actions (`openOrUpdateWindow`, `closeWindow`, `organizerCheckInStudent`, `listAttendance`) require:
  - platform admin OR club membership role ADMIN/STAFF for the event’s club.
- Student check-in:
  - must be global role `STUDENT`
  - must be registered for the event
  - token must match a window hash
  - window must be active
  - request time must be within configured window `(start - opensBefore) <= now <= (start + closesAfter)`
  - optional: window should not exceed event end time beyond ~5 min grace

### Event ratings

Backed by table:

- `event_ratings` unique `(event_id, student_id)` → “upsert semantics” (latest rating overwrites previous)

Key rules (from `EventRatingService`):

- Only global role `STUDENT` can rate.
- Student must be registered for event.
- Student must have attended (attendance record exists).
- **Timing**: the current service explicitly says rating is allowed at any time (before/during/after). If you want “only after event ends”, this rule would need to be re-added.

### Comments & likes

Backed by:

- `comments` (threaded with `parent_id` and polymorphic target `(target_type, target_id)`)
- `comment_likes` unique `(comment_id, user_id)`

Key rules (from `CommentService`):

- Read thread:
  - loads all target comments with author eagerly fetched (`findThreadWithAuthor`)
  - computes `likeCount` and `likedByMe`
  - builds a comment tree in-memory
- Posting:
  - only `STUDENT` can comment
  - target must exist
  - if replying, parent must belong to the same target
- Likes toggle:
  - only `STUDENT` can like
  - toggle behavior (insert if missing; delete if exists)

UI helpers:

- `getRootPreview(...)` for event/activity detail pages (preview limited list)
- `getThreadWithReplyPreview(...)` to keep UI compact (first N replies only)
- `getDirectReplies(...)` for “see more replies” expansion

---

## Database schema & data model

Source of truth: `src/main/resources/db/migration/V1__schema.sql`.

### Main tables

- `users`
- `clubs`
- `memberships`
- `club_applications`
- `events`
- `activities`
- `event_registrations`
- `event_attendance`
- `event_attendance_windows`
- `comments`
- `comment_likes`
- `event_ratings`

### Key constraints

- Unique: users.email
- Attendance:
  - `event_attendance` unique (event_id, user_id)
  - `event_attendance_windows` unique (event_id) and unique (token_hash)
- Comment likes: unique (comment_id, user_id)
- Ratings: unique (event_id, student_id)

---

## Security model

### Route protection (SecurityFilterChain)

`SecurityConfig` sets:

- CSRF disabled
- Stateless sessions
- Public pages:
  - `/`, `/clubs`, `/events`, `/login`, `/register`, static assets
- `/events/*` requires authentication (event detail requires login)
- Web action: `POST /events/*/register` requires `ROLE_STUDENT`
- Public API reads:
  - `GET /api/events/*/ratings/summary`
  - `GET /api/comments/**`
- Auth endpoints:
  - `/api/auth/**` public
- Club admin pages:
  - `/club-admin/**` authenticated (deeper membership enforcement in controller/service)
- Club applications:
  - apply endpoints authenticated
  - review endpoints require `ROLE_PLATFORM_ADMIN`
- `/api/admin/**` require `ROLE_PLATFORM_ADMIN`

### JWT authentication

`JwtAuthenticationFilter`:

- Accepts JWT from:
  - cookie `jwtToken`, otherwise
  - `Authorization: Bearer <token>` header
- Validates and loads user details, sets SecurityContext.
- Special case: if user was deleted but cookie remains, `UsernameNotFoundException` is treated as “unauthenticated” (clears context; no stack trace).

Global roles:

- Appears to use `Role` enum (`PLATFORM_ADMIN`, `STUDENT`) with Spring Security roles via `hasRole(...)`.

---

## Server-rendered UI (Thymeleaf)

Templates live in `src/main/resources/templates/`.

Pages cover:

- Public browsing: clubs, events
- Auth: login/register
- Club details & members listing
- Event details + comments preview + rating summary
- Club admin:
  - club applications management
  - member management (approve/reject/kick)
  - attendance management (open/rotate QR, fullscreen QR, print QR, manual check-in)

Assets:

- CSS: `src/main/resources/static/css/styles.css`
- JS: `src/main/resources/static/js/qrcode.min.js` (qrcodejs)

Notable UI-to-backend integration (from `UI-ATTENDANCE-RATING.md` and `docs/qr-and-attendance.md`):

- Manage attendance renders QR locally using `qrcode.min.js`.
- Print QR uses a separate window and carefully avoids embedding a literal `</script>` sequence inside template literals.

---

## AI / RAG WhatsApp assistant

Documentation source: `docs/ai-rag-whatsapp.md`.

### Components

- `WhatsAppWebhookController`: provider-agnostic inbound endpoint
- `ConversationService`: session + history + context + async processing
- `RagService`: orchestrates retrieval + LLM decision + tool execution
- `RetrievalService`: structured retrieval
- `ToolRegistry` + tools: action execution through existing services
- `WhatsAppSender`: outbound adapter (currently logging)

### Current behavior

- Retrieval and prompt building exist.
- LLM is abstracted; currently uses a deterministic stub.
- Identity linking is **not implemented** yet (`ctx.linked()` is expected to be false), therefore tool calls are blocked with a “please link first” message.

---

## Configuration & operations

- `compose.yaml` runs PostgreSQL with:
  - db: `mclub_db`
  - user: `mclub_user`
  - password: `mclub_password`
- `application.yml`:
  - datasource points to local postgres
  - JPA `ddl-auto: validate` (schema must match Flyway)
  - `open-in-view: false`
  - Hikari keepalive/maxLifetime tuned
  - JWT secret + expiration under `app.jwt.*`

---

## Known gaps / next improvements

1. **Synchronize docs with code**
   - Example: rating “only after event ends” appears in older docs, but current `EventRatingService` allows rating at any time.
2. **AI assistant identity linking**
   - recommended table: `user_whatsapp_links(user_id, phone_e164, verified_at)`
   - implement OTP or web-based linking flow
3. **Performance improvements**
   - `CommentService.getThread` counts likes with per-comment queries (N+1 style). Consider aggregate queries when data grows.
4. **Security hardening**
   - consider enabling CSRF for cookie-based auth flows (or ensure API usage is strictly token-in-header + same-site cookies).

---

## Appendix A — REST API endpoint inventory (from controllers)

> This is a **quick index** of API endpoints as implemented in controller classes. Authorization is shown where it is explicit in annotations or obvious from `SecurityConfig`.

### Auth (`AuthController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Returns `AuthResponse` (JWT token). |
| POST | `/api/auth/register` | Public | Creates user. |

### Clubs (`ClubController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/clubs` | `ROLE_PLATFORM_ADMIN` | Create club. |
| GET | `/api/clubs` | Public* | Lists clubs (paged). (*Not explicitly permitted; depends on `SecurityConfig` defaults / UI usage.) |
| GET | `/api/clubs/{id}` | Public* | Club detail. (*Depends on security defaults.) |

### Memberships (`MembershipController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/memberships/club/{clubId}/join` | Authenticated | Creates join request (pending). |
| PUT | `/api/memberships/{membershipId}/status?status=...` | `ROLE_PLATFORM_ADMIN` | Update membership status globally. |
| PUT | `/api/memberships/{membershipId}/role?role=...` | `ROLE_PLATFORM_ADMIN` | Update membership role globally. |
| GET | `/api/memberships/club/{clubId}` | Public* | List members. (*Depends on security defaults.) |

### Events & registrations (`EventController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/events` | Authenticated | Creates event (service enforces can-manage). |
| GET | `/api/events/club/{clubId}` | Public* | List events for club. (*Depends on security defaults.) |
| POST | `/api/events/{eventId}/register` | Authenticated | Registers current user. |
| GET | `/api/events/{eventId}/registrations/summary` | Public* | Count registrations. (*Depends on security defaults.) |
| GET | `/api/events/{eventId}/participants` | Authenticated + can-manage-event | Full participant list. |

### Attendance (`AttendanceController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/events/{eventId}/attendance/window` | Authenticated + organizer | Opens/rotates attendance window; returns raw token. |
| POST | `/api/events/{eventId}/attendance/window/close` | Authenticated + organizer | Closes window. |
| GET | `/api/events/{eventId}/attendance` | Authenticated + can-manage-event | List attendance. |
| POST | `/api/attendance/check-in` | `ROLE_STUDENT` | Student check-in by raw token. |
| POST | `/api/events/{eventId}/attendance/check-in/{studentId}` | Authenticated + organizer | Manual organizer check-in. |

### Event ratings (`EventRatingController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/events/{eventId}/ratings` | `ROLE_STUDENT` | Create/update rating. |
| GET | `/api/events/{eventId}/ratings/me` | `ROLE_STUDENT` | Get my rating. |
| GET | `/api/events/{eventId}/ratings/summary` | Public (explicit in `SecurityConfig`) | Average + count. |
| GET | `/api/events/{eventId}/ratings` | Authenticated + can-manage-event | Full rating list. |

### Comments (`CommentController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/comments/{targetType}/{targetId}` | Public (explicit in `SecurityConfig`) | Full thread (tree). |
| GET | `/api/comments/{commentId}/replies` | Public (explicit in `SecurityConfig`) | Direct replies only. |
| POST | `/api/comments/{targetType}/{targetId}` | `ROLE_STUDENT` | Create root or reply (via `parentId`). |
| POST | `/api/comments/{commentId}/reply` | `ROLE_STUDENT` | Convenience reply endpoint. |
| POST | `/api/comments/{commentId}/like` | `ROLE_STUDENT` | Toggle like. |

### Activities (`ActivityController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/activities` | Authenticated | Create activity. |
| GET | `/api/activities/club/{clubId}` | Public* | List activities for club. (*Depends on security defaults.) |

### WhatsApp webhook (`WhatsAppWebhookController`)

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/webhooks/whatsapp` | Public (not in filter chain allowlist, so may require adjustment) | Async: enqueue and return 200 quickly. |

---

## Appendix B — Web UI routes & templates (high level)

> Web routes are implemented in `io.droidevs.mclub.controller` web controllers and render Thymeleaf templates under `src/main/resources/templates/`.

Common routes (non-exhaustive):

- Auth:
  - `GET /login` + `POST /login` → `login.html` (sets `jwtToken` cookie)
  - `GET /register` + `POST /register` → `register.html`
  - `POST /logout` → clears cookie
- Main browsing:
  - `GET /` → `dashboard.html` (requires login; redirects to `/login` if anonymous)
  - `GET /clubs` → `clubs.html`
  - `GET /clubs/{id}` → `club-detail.html`
  - `GET /clubs/{id}/members` → `club-members.html`
  - `GET /clubs/{id}/events` → `club-events.html`
  - `GET /clubs/{id}/activities` → `club-activities.html`
  - `GET /events` → `events.html`
  - `GET /events/{id}` → `event-detail.html` (requires login by `SecurityConfig`)
- Student-only actions:
  - `POST /events/{eventId}/register` (guarded in `SecurityConfig`)
  - `GET/POST /events/{eventId}/check-in` → `event-checkin.html`
  - `GET/POST /events/{eventId}/rate` → `event-rate.html`
- Comments page:
  - `GET /comments/{targetType}/{targetId}` → `comments.html`
- Club admin area:
  - `GET /club-admin/clubs` → `my-managed-clubs.html`
  - `GET /club-admin/clubs/{clubId}/members` → `manage-members.html`
  - `GET /club-admin/clubs/{clubId}/attendance` → `club-attendance-events.html`
  - `GET /club-admin/events/{eventId}/attendance` → `manage-attendance.html`
  - `GET /club-admin/events/{eventId}/admin` → `event-admin.html` (admin page relying on REST calls)

---

## Pointers (files to start reading)

- Entry point: `src/main/java/io/droidevs/mclub/MClubApplication.java`
- Security:
  - `src/main/java/io/droidevs/mclub/security/SecurityConfig.java`
  - `src/main/java/io/droidevs/mclub/security/JwtAuthenticationFilter.java`
  - `src/main/java/io/droidevs/mclub/security/JwtTokenProvider.java`
- Attendance: `src/main/java/io/droidevs/mclub/service/AttendanceService.java`
- Ratings: `src/main/java/io/droidevs/mclub/service/EventRatingService.java`
- Comments: `src/main/java/io/droidevs/mclub/service/CommentService.java`
- AI assistant:
  - `src/main/java/io/droidevs/mclub/ai/webhook/whatsapp/WhatsAppWebhookController.java`
  - `src/main/java/io/droidevs/mclub/ai/conversation/ConversationService.java`
  - `src/main/java/io/droidevs/mclub/ai/rag/RagService.java`


