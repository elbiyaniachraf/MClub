# MClub UI – Attendance (QR) + Rating

This document explains how the **web UI** integrates the new **event attendance** and **event rating** features.

## 1) Student flow (Events page)

Template: `src/main/resources/templates/events.html`

### A) Register
- Button: **Register**
- Web route:
  - `POST /events/{eventId}/register`
- Controller:
  - `WebEventRegistrationController.registerForEvent(...)`
- Service:
  - `RegistrationService.register(eventId, studentEmail)`

### B) Check-in (QR)
- Button: **Check-in** opens a modal.
- Student pastes/scans the QR token into the modal field.
- JS sends:
  - `POST /api/attendance/check-in`
  - Body: `{ "token": "<rawTokenFromQr>" }`

Backend enforces:
- user must be global role `STUDENT`
- student must be registered for the event
- attendance window must be active
- request time must be inside the event’s configured window

When successful:
- an `EventAttendance` row is created (unique per `{eventId, userId}`)

### C) Rate the event
- Button: **Rate** opens a modal.
- JS sends:
  - `POST /api/events/{eventId}/ratings`
  - Body: `{ "rating": 1..5, "comment": "optional" }`

Backend enforces:
- user is `STUDENT`
- student is registered
- student attended (must have `EventAttendance` for this event)
- event has ended (B option)

Additionally, each event card loads rating summary:
- `GET /api/events/{eventId}/ratings/summary`
- shown as average + count.

## 2) Organizer flow (Club admin attendance management)

Templates:
- `src/main/resources/templates/my-managed-clubs.html`
- `src/main/resources/templates/club-attendance-events.html`
- `src/main/resources/templates/manage-attendance.html`

Controllers:
- `WebClubAdminController` – member management
- `WebAttendanceController` – attendance management pages

### A) Navigate to attendance
1. From **Club Admin → My Managed Clubs** (`/club-admin/clubs`)
2. Click **Attendance** on a club → `/club-admin/clubs/{clubId}/attendance`
3. Choose an event → `/club-admin/events/{eventId}/attendance`

Authorization:
- only club `ADMIN` or `STAFF` can manage attendance for that club

### B) Open/rotate QR token + configure window
Page: `manage-attendance.html`

Form posts:
- `POST /club-admin/events/{eventId}/attendance/window`

This calls:
- `AttendanceService.openOrUpdateWindow(eventId, request, organizerEmail)`

Result:
- backend generates a new random token
- stores only the token hash (SHA-256) in `EventAttendanceWindow`
- returns the raw token to show to the organizer

### C) QR rendering in the UI
The page includes a **local** QR library:
- `/js/qrcode.min.js`

And generates QR in JS:
- `new QRCode(target, { text: token, width: 160, height: 160, correctLevel: QRCode.CorrectLevel.H })`

So the organizer sees a scannable QR immediately.

### D) Close window
Form posts:
- `POST /club-admin/events/{eventId}/attendance/window/close`

Calls:
- `AttendanceService.closeWindow(eventId, organizerEmail)`

### E) View attendance list
The page loads:
- `attendanceService.listAttendance(eventId, organizerEmail)`

And renders:
- list of students who checked in
- check-in time and method

### F) Manual check-in
Form posts:
- `POST /club-admin/events/{eventId}/attendance/check-in`

Calls:
- `AttendanceService.organizerCheckInStudent(eventId, studentId, organizerEmail)`

This is used when the organizer scans/checks a student in person.

## 3) Club admin approving memberships

Template: `src/main/resources/templates/manage-members.html`

Page:
- `/club-admin/clubs/{clubId}/members`

Actions:
- **Approve** → `POST /club-admin/memberships/{membershipId}/status` with `status=APPROVED`
- **Reject**  → `POST /club-admin/memberships/{membershipId}/status` with `status=REJECTED`

Controller:
- `WebClubAdminController.updateMembershipStatus(...)`

Authorization:
- only club `ADMIN/STAFF` (for that club) can approve/reject.

## 4) Fix included in this change set

### A) Thymeleaf crash on `club-detail.html`
Problem:
- template referenced `member.userFullName`, but `MembershipDto` didn’t define it.

Fix:
- Added fields to `MembershipDto`:
  - `userFullName`, `userEmail`
- Updated `MembershipMapper` to map:
  - `user.fullName -> userFullName`
  - `user.email -> userEmail`

### B) favicon warning noise
Added an empty `src/main/resources/static/favicon.ico` to prevent `/favicon.ico` warnings.

---

## Quick troubleshooting

- If QR does not render on `manage-attendance`:
  - Confirm `/js/qrcode.min.js` is served and loads in browser devtools.
  - Confirm it defines `window.QRCode`.
  - Confirm `qrToken` is non-empty.

- If student cannot rate:
  - they must: register → check in (attendance record exists) → event must have ended.

