# MapStruct migration plan (MClubClone)

This document is a **production-safe** refactoring plan to eliminate manual Entity ↔ DTO mapping and standardize on **MapStruct**.

It is tailored to the current codebase (Spring Boot + JPA + Flyway + JWT + Thymeleaf) whose structure is:

- Controller → Service → Repository → Domain
- DTO package exists
- Mapper package exists but is partially implemented

## Goals (as enforced rules)

- **No mapping in services** (no `new XxxDto()`, no setters building DTOs, no field-by-field mapping loops)
- **MapStruct is the only mapping mechanism**
- DTOs contain no business logic, no JPA annotations
- Entities do not depend on DTOs or MapStruct
- No API behavior changes

---

## Current status in this repo (as of this change)

Already present MapStruct mappers:

- `ClubMapper`
- `EventMapper`
- `MembershipMapper`
- `RegistrationMapper`

Manual mapping still exists in services:

- `EventRatingService` had a `private EventRatingDto toDto(EventRating r)` method (now removed and replaced with MapStruct via `EventRatingMapper`).
- `CommentService` still contains a private `toDto(...)` plus in-memory tree building. The tree building is domain logic, but the *DTO conversion* must move to a mapper.
- `AttendanceService` has a manual `toDto(EventAttendance)` conversion.

---

## Required structural design

### 1) Central MapStruct config

Shared config:

- `io.droidevs.mclub.mapper.CentralMapperConfig`
  - `componentModel = "spring"`
  - `unmappedTargetPolicy = ReportingPolicy.ERROR` (forces completeness)

All mappers must be:

```java
@Mapper(componentModel = "spring", config = CentralMapperConfig.class)
```

(Each mapper also imports `CentralMapperConfig` from `io.droidevs.mclub.mapper`.)

### 2) Mapper package layout

Keep all mappers in:

- `io.droidevs.mclub.mapper`

Mappers by domain:

- `ClubMapper`
- `EventMapper`
- `MembershipMapper`
- `CommentMapper`
- `ActivityMapper`
- `UserMapper`
- `EventRatingMapper`
- `ClubApplicationMapper`
- `AttendanceMapper` (or `EventAttendanceMapper` + `AttendanceWindowMapper`)

### 3) DTO strategy (minimal changes, backward compatible)

Because this is a production migration, do DTO changes gradually:

**Phase 1 (now):** keep existing DTOs as-is and provide mappers for them.

**Phase 2 (optional enhancement):** introduce *summary vs details* DTOs to avoid cycles and over-fetching:

- `ClubSummaryDto`, `ClubDetailsDto`
- `EventSummaryDto`, `EventDetailsDto`

But only once current endpoints are stable and covered by tests.

---

## How services should look after migration

- Services continue to load/validate entities and enforce business rules.
- Services call repositories.
- Services return DTOs only by delegating to mappers:

```java
return eventMapper.toDto(event);
```

Allowed:

- building entities from request DTOs (prefer MapStruct `toEntity(...)` if you introduce request DTOs)
- business decisions / authorization / orchestration

Forbidden:

- dto setters in services
- `stream().map(x -> new Dto(...))`
- private `toDto(...)` helpers in services

---

## Step-by-step migration strategy (safe)

### Step 0 — Make build safe

- Ensure Gradle has MapStruct + processor configured (already present).
- Ensure all mappers use `CentralMapperConfig`.
- Build + test after each step.

### Step 1 — Replace the easiest manual mappings first

1. Add mapper interface
2. Inject into service
3. Remove manual `toDto(...)`
4. Run tests

Completed in this repo:

- `EventRatingService` → now uses `EventRatingMapper`

### Step 2 — Attendance mapping

- Create `AttendanceMapper` that maps `EventAttendance -> AttendanceRecordDto`.
- Replace `AttendanceService.toDto(...)` with mapper call.

### Step 3 — Comments mapping (threaded)

This needs special care:

- Keep tree-building logic in service (domain-ish shaping for UI)
- Move per-comment mapping to `CommentMapper`
- Approach:
  - MapStruct maps `Comment -> CommentDto` for base fields
  - service sets dynamic fields not present on entity (like `likeCount`, `likedByMe`, replies list) **only if those are not part of mapping**

However your rule says “no mapping in services” — to comply strictly, make the mapper accept those dynamic values via a wrapper mapping method:

- `CommentMapper.toDto(Comment c, long likeCount, boolean likedByMe)` using `@Context` or an explicit method signature (MapStruct supports multiple source parameters).

Then service can call:

```java
commentMapper.toDto(c, likeCount, likedByMe);
```

Service is still orchestrating, not mapping field-by-field.

### Step 4 — Activities, applications, user

For each domain:

- Introduce mapper
- Replace any dto building in services/controllers

### Step 5 — Optional DTO split (summary/details)

Only once mapping is centralized:

- add summary DTOs for list endpoints
- adjust controllers to return summary where it does not break API

---

## Tests required

### Mapper unit tests

Add JUnit tests for:

- `EventMapper`
- `ClubMapper`
- `CommentMapper`

Validation points:

- Null handling
- Nested mapping correctness
- List mapping correctness
- Ensure unmapped fields fail compilation (ReportingPolicy.ERROR)

Implementation suggestion:

- Use Spring test context to autowire mappers (componentModel=spring)
- OR use `Mappers.getMapper(...)` for pure unit tests (but you’re using Spring component model; either is fine)

---

## Notes / pitfalls

- With `unmappedTargetPolicy = ERROR`, every DTO change will force mapper updates (this is intentional).
- For `open-in-view: false`, repositories must eagerly fetch relationships needed by mappers (e.g., `createdBy`, `club`, `author`). If they don’t, mapping may trigger lazy-loading exceptions inside service @Transactional boundaries.



