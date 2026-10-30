# Student App API

Written for: developers who are new to this API and want to understand it and test it in Postman.

This document covers how to start the app, how login and roles work, every endpoint with examples, the error codes, and a step-by-step Postman guide. The Postman collection is in [postman/student-app.postman_collection.json](postman/student-app.postman_collection.json).

---

## 1. Quick start

1. Set the three seed passwords as environment variables. Each must be at least 8 characters, and they are used only the first time the accounts are created:
   ```
   SEED_ADMIN_PASSWORD=AdminPass123
   SEED_HR_PASSWORD=HrPass1234
   SEED_EMPLOYEE_PASSWORD=EmpPass1234
   ```
   In PowerShell: `$env:SEED_ADMIN_PASSWORD = "AdminPass123"`. In Git Bash: `export SEED_ADMIN_PASSWORD=AdminPass123`.
2. Start the app from the project folder:
   ```
   mvn spring-boot:run -Dspring-boot.run.arguments=--app.jwt.cookie-secure=false
   ```
   The app listens on **http://localhost:8080** and creates `student.db` on the first run.
3. The first start creates three accounts: `admin` (ADMIN), `hr_user` (HR), and `employee` (EMPLOYEE). A variable left empty means that account is skipped, with a warning in the log.
4. Why `cookie-secure=false`: the login cookie is marked `Secure`, so browsers and Postman store it only over HTTPS. Without this setting, login works but later requests return 401 over plain HTTP.
5. If port 8080 is busy, add `--server.port=8090` to the same arguments and change `baseUrl` in Postman to match.

---

## 2. Roles and who can do what

Every user has one role. The role decides what the user can call.

| Role | Can do | Cannot do |
|---|---|---|
| **ADMIN** | Everything: students, users, courses, tech stacks, tech-stack links, enrollment | Nothing is blocked |
| **HR** | List all students (`GET /students`), read any student (`GET /students/{id}`), read own profile (`/auth/me`) | Create, update, or delete students; anything about users, courses, or tech stacks |
| **EMPLOYEE** | Read a student (`GET /students/{id}`) **only if that student is linked to their own account**; read own profile | List all students; read another user's student; anything about users, courses, or tech stacks |

### Endpoint access at a glance

| Endpoint | ADMIN | HR | EMPLOYEE | Not logged in |
|---|---|---|---|---|
| `POST /auth/register` | yes | yes | yes | yes |
| `POST /auth/login`, `POST /auth/logout` | yes | yes | yes | yes |
| `GET /auth/me` | yes | yes | yes | 401 |
| `GET /students` | yes | yes | 403 | 401 |
| `GET /students/{id}` | yes | yes | only own students (else 403) | 401 |
| `POST`, `PUT`, `DELETE /students/...`, `PUT /students/{id}/owner` | yes | 403 | 403 | 401 |
| `/users/**` (all methods) | yes | 403 | 403 | 401 |
| `/courses/**` (all methods) | yes | 403 | 403 | 401 |
| `/tech-stacks/**` (all methods) | yes | 403 | 403 | 401 |

### Notes on roles

- **Public registration always creates an EMPLOYEE.** Only an admin can create HR or ADMIN users, through `POST /api/v1/users`.
- **Employee access comes from ownership.** An admin links a student to the employee account with `PUT /api/v1/students/{id}/owner`. The employee can then read that student. A student with no owner is visible to ADMIN and HR only.
- **A missing student returns 403 for an employee**, not 404. This stops an employee from finding out which IDs exist. Admin and HR get 404 as usual.
- **Role changes apply at the next login.** The role is stored in the login token, so a user whose role changes keeps the old role until their token expires (30 minutes) or they log in again.
- **Deleting a user does not delete their students.** Those students stay in the system, with no owner.

---

## 3. Base URL and versioning

| Item | Value |
|---|---|
| Base URL | `http://localhost:8080` |
| API prefix | `/api/v1` |
| Body format | `application/json` for every request that has a body |
| Times | ISO 8601 in UTC, for example `2026-10-30T05:10:01.276909800Z` |

---

## 4. Authentication: how login works

1. **Log in** with `POST /api/v1/auth/login`. The server responds with `Set-Cookie: access_token=...`. The token is **not** in the JSON body.
2. The browser or Postman stores the cookie and sends it with every later request.
3. The token carries the username and the role. Every protected endpoint checks both.
4. **Log out** with `POST /api/v1/auth/logout`. The cookie is cleared in the client.

### Cookie details

| Attribute | Value | Meaning |
|---|---|---|
| Name | `access_token` | Holds the JWT |
| `HttpOnly` | yes | JavaScript on a page cannot read it |
| `Secure` | yes (configurable) | Sent only over HTTPS |
| `SameSite` | `Strict` | Not sent on requests started from other websites |
| `Path` | `/` | Sent to every endpoint |
| `Max-Age` | `1800` | Valid for 30 minutes |

### Token rules

- A token lasts **30 minutes**. After that the server returns 401 and you must log in again.
- Tokens are signed with RS256. A changed or invented token returns 401.
- The signing key is generated at startup. **Restarting the app invalidates every token.**
- Logout clears the cookie in the client. The server keeps no record of tokens, so a copied token still works until it expires.

---

## 5. Response format

Every response, success or error, has the same shape:

```json
{
  "success": true,
  "data": { },
  "message": "Student created successfully",
  "errorCode": null,
  "timestamp": "2026-10-30T05:10:01.276909800Z"
}
```

| Field | Meaning |
|---|---|
| `success` | `true` or `false` |
| `data` | The result. `null` for errors, login, and logout |
| `message` | Human-readable. The wording may change, so do not write code that depends on it |
| `errorCode` | Stable code for errors, such as `STUDENT_NOT_FOUND`. Check this in code |
| `timestamp` | When the response was created |

**Exceptions:** `DELETE` endpoints return `204 No Content` with an empty body, and so does logout.

---

## 6. Endpoints

### 6.1 Auth

#### Register

`POST /api/v1/auth/register` (public)

Creates an EMPLOYEE account. It does not log you in.

```json
{ "username": "kiran", "password": "password123" }
```

| Rule | Requirement |
|---|---|
| `username` | Required, 3 to 50 characters, unique |
| `password` | Required, 8 to 72 characters |

| Status | When | `errorCode` |
|---|---|---|
| `201 Created` | Account created. `data` has `id`, `username`, `role` (always `EMPLOYEE`). | |
| `400` | A rule is broken | `VALIDATION_ERROR` |
| `409` | Username taken | `USERNAME_TAKEN` |

#### Login

`POST /api/v1/auth/login` (public)

Body: same as register. On success it sets the `access_token` cookie.

| Status | When | `errorCode` |
|---|---|---|
| `200 OK` | Correct credentials. `data` is `null`. | |
| `401` | Wrong username or password. The message is the same for both, so usernames cannot be guessed. | `INVALID_CREDENTIALS` |
| `400` | Missing or invalid fields | `VALIDATION_ERROR` |

#### Logout

`POST /api/v1/auth/logout` (public). Returns `204`.

#### Current user

`GET /api/v1/auth/me` (any logged-in role)

```json
{
  "success": true,
  "data": { "id": 4, "username": "employee", "role": "EMPLOYEE" },
  "message": "Current user retrieved successfully",
  "errorCode": null,
  "timestamp": "..."
}
```

---

### 6.2 Students

#### Create a student (ADMIN)

`POST /api/v1/students`

```json
{ "name": "Kiran", "subject": "Java", "address": "India", "age": 28 }
```

| Field | Rule |
|---|---|
| `name` | Required, not blank, at most 100 characters |
| `subject` | Required, not blank, at most 100 characters |
| `address` | Optional, at most 255 characters |
| `age` | Required, whole number from 1 to 120 |

Do not send `id`. The database creates it. The new student has no owner. Link one with `PUT /students/{id}/owner`.

Response `201 Created` with a `Location` header. `data` includes `ownerUserId`, which is `null` for a new student.

#### List all students (ADMIN, HR)

`GET /api/v1/students`. Returns a list. An empty table gives an empty list.

#### Get one student (ADMIN, HR, and EMPLOYEE for own students)

`GET /api/v1/students/{id}`

| Status | When |
|---|---|
| `200 OK` | Found, and the caller may see it |
| `400` | The ID is not a number, such as `/students/abc` (`INVALID_PARAMETER`) |
| `403` | Employee asking for a student they do not own (`FORBIDDEN`) |
| `404` | Admin or HR asking for a missing student (`STUDENT_NOT_FOUND`) |

#### Update a student (ADMIN)

`PUT /api/v1/students/{id}`

Send all four fields. This replaces the student, so send everything, not only the fields you change. The owner link is not changed by this call.

| Status | When |
|---|---|
| `200 OK` | Updated |
| `400` | Validation failed |
| `404` | Missing student. **No new student is created.** |

#### Delete a student (ADMIN)

`DELETE /api/v1/students/{id}`. Returns `204`, or `404` if missing. The student is also removed from any course they were enrolled in.

#### Link a student to a user (ADMIN)

`PUT /api/v1/students/{id}/owner`

```json
{ "userId": 4 }
```

Send `{"userId": null}` to remove the link. Returns `200` with the updated student. Returns `404 USER_NOT_FOUND` if the user does not exist.

---

### 6.3 Users (ADMIN only)

#### List users

`GET /api/v1/users`. Returns every user with `id`, `username`, and `role`. Password hashes are never returned.

#### Get one user

`GET /api/v1/users/{id}`. Returns `200`, or `404 USER_NOT_FOUND`.

#### Create a user

`POST /api/v1/users`

```json
{ "username": "new-hire", "password": "password123", "role": "HR" }
```

| Field | Rule |
|---|---|
| `username` | Required, 3 to 50 characters, unique |
| `password` | Required, 8 to 72 characters. Stored as a BCrypt hash. |
| `role` | Required: `ADMIN`, `HR`, or `EMPLOYEE` |

Returns `201` with a `Location` header, or `409 USERNAME_TAKEN`, or `400 VALIDATION_ERROR`.

#### Update a user

`PUT /api/v1/users/{id}`

```json
{ "username": "new-hire", "role": "EMPLOYEE" }
```

`password` is optional. Leave it out to keep the current password. Role changes apply at the next login. Returns `200`, `409`, or `404`.

#### Delete a user

`DELETE /api/v1/users/{id}`. Returns `204`. The user's students are kept, with `ownerUserId` set to `null`.

---

### 6.4 Tech stacks (ADMIN only)

| Method | Path | Success | Notes |
|---|---|---|---|
| GET | `/api/v1/tech-stacks` | 200 | List |
| GET | `/api/v1/tech-stacks/{id}` | 200 | One tech stack |
| POST | `/api/v1/tech-stacks` | 201 | Body `{"name": "Java"}`. Name must be unique (`409 NAME_TAKEN`). |
| PUT | `/api/v1/tech-stacks/{id}` | 200 | Body `{"name": "..."}` |
| DELETE | `/api/v1/tech-stacks/{id}` | 204 | Also removes the tech stack from every course that uses it |

Response shape for one tech stack: `{"id": 1, "name": "Java"}`.

---

### 6.5 Courses (ADMIN only)

A course can be linked to many tech stacks and many students. Each link is stored in a mapping table:
- `course_tech_stack` holds the course-to-tech-stack links.
- `course_student` holds the course-to-student enrollments.

| Method | Path | Success | Notes |
|---|---|---|---|
| GET | `/api/v1/courses` | 200 | List |
| GET | `/api/v1/courses/{id}` | 200 | One course |
| POST | `/api/v1/courses` | 201 | Body `{"name": "Backend", "description": "..."}`. Name must be unique. |
| PUT | `/api/v1/courses/{id}` | 200 | Same body as POST |
| DELETE | `/api/v1/courses/{id}` | 204 | Removes the course and its mapping rows |
| POST | `/api/v1/courses/{id}/tech-stacks/{techStackId}` | 200 | Links a tech stack. Linking twice does nothing. |
| DELETE | `/api/v1/courses/{id}/tech-stacks/{techStackId}` | 200 | Unlinks it |
| POST | `/api/v1/courses/{id}/students/{studentId}` | 200 | Enrolls a student |
| DELETE | `/api/v1/courses/{id}/students/{studentId}` | 200 | Removes the enrollment |

Course response shape:

```json
{
  "id": 2,
  "name": "Backend",
  "description": "Server side development",
  "techStacks": [ { "id": 1, "name": "Java" } ],
  "studentIds": [ 3, 5 ]
}
```

Mapping endpoints return the whole course, so the links are visible in one response. Errors: `404 COURSE_NOT_FOUND`, `404 TECH_STACK_NOT_FOUND`, `404 STUDENT_NOT_FOUND`, `409 NAME_TAKEN`.

---

## 7. Error codes

| HTTP | `errorCode` | Cause |
|---|---|---|
| 400 | `VALIDATION_ERROR` | A field broke a rule. The message lists each field. |
| 400 | `MALFORMED_REQUEST` | Body missing or not valid JSON |
| 400 | `INVALID_PARAMETER` | A path value has the wrong type, such as `/students/abc` |
| 401 | `UNAUTHORIZED` | No cookie, expired token, or tampered token |
| 401 | `INVALID_CREDENTIALS` | Wrong username or password at login |
| 403 | `FORBIDDEN` | Logged in, but this role may not do this action, or an employee asked for a student they do not own |
| 404 | `STUDENT_NOT_FOUND` | No student with that ID |
| 404 | `USER_NOT_FOUND` | No user with that ID |
| 404 | `COURSE_NOT_FOUND` | No course with that ID |
| 404 | `TECH_STACK_NOT_FOUND` | No tech stack with that ID |
| 409 | `USERNAME_TAKEN` | Username already registered |
| 409 | `NAME_TAKEN` | Course or tech-stack name already used |
| 500 | `INTERNAL_ERROR` | Unexpected server problem. Details are in the server log only. |

The difference between 401 and 403: **401** means the server does not know who you are (no or bad cookie). **403** means it knows who you are but your role does not allow the action.

---

## 8. Postman guide

### 8.1 Import and set up

1. In Postman, click **Import** and choose `docs/postman/student-app.postman_collection.json`.
2. Open the collection, then **Variables**. Fill in:
   - `adminPassword`, `hrPassword`, `employeePassword`: the same values you set in the `SEED_*` environment variables.
   - `baseUrl`: `http://localhost:8080`, or the port you chose.
3. Leave `suffix`, `studentId`, `courseId`, and the other ID variables empty. The requests fill them in.

### 8.2 How the folders work

Postman stores one login cookie at a time. So each role folder starts with its own **Login as ...** request. The folders must run in order:

| Folder | Who is logged in | What it checks |
|---|---|---|
| 1. Public and admin login | public, then admin | Register, login, `/me` |
| 2. Admin: users | admin | Create, list, read, update, and delete a user |
| 3. Admin: tech stacks and courses | admin | Tech stack and course CRUD, linking and unlinking |
| 4. Admin: students | admin | Student CRUD, owner link, enrollment |
| 5. HR (hr_user) | HR | Can list and read; cannot create or see users and courses |
| 6. Employee (employee) | employee | Can read own student; cannot read others or list all |
| 7. Negative cases and cleanup (admin) | admin | Error codes, then removes the test students |
| 8. Logout | none | Logout clears the cookie, and the next request returns 401 |

Run the whole collection with **Collection Runner**. Each request has a Tests tab, and the **Test Results** panel shows pass or fail.

### 8.3 Try one request by hand

1. Run folder 1 first, so you are logged in as admin.
2. Open **Students / Create student A**, change the body, and click **Send**.
3. To try the employee rules, log in as employee first (folder 6 does this for you), then send a `GET` to a student.

---

## 9. curl examples

```bash
# Log in as admin and save the cookie
curl -c cookies.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"AdminPass123"}'

# Create a student (ADMIN)
curl -b cookies.txt -X POST http://localhost:8080/api/v1/students \
  -H "Content-Type: application/json" \
  -d '{"name":"Kiran","subject":"Java","address":"India","age":28}'

# Create a user with the HR role (ADMIN)
curl -b cookies.txt -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"username":"recruiter1","password":"Recruit123","role":"HR"}'

# Log in as HR, then list students
curl -c hr.txt -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"hr_user","password":"HrPass1234"}'
curl -b hr.txt http://localhost:8080/api/v1/students

# Log out
curl -b cookies.txt -c cookies.txt -X POST http://localhost:8080/api/v1/auth/logout
```

---

## 10. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| Login returns 401 for `admin`, `hr_user`, or `employee` | That seed account was not created, usually because its `SEED_*` variable was empty | Set the variable, then restart the app. Check the log for "Seed user ... not created". |
| Login returns 400 for `hr_user` | The username was typed as `hr` by an older run | Usernames need 3 characters. The seeded HR account is `hr_user`. |
| Login returns 200 but later requests return 401 | The cookie was not stored, usually because the app runs over HTTP with `cookie-secure=true` | Start with `--app.jwt.cookie-secure=false` for local testing |
| 401 on everything after a restart | The signing key changes at each start, so old tokens are invalid | Log in again |
| 403 for an action you expected to work | Your role does not allow it. See section 2. | Log in with a role that has the access |
| 403 on a student you created | Employees can read only students linked to them | An admin links the student with `PUT /students/{id}/owner` |
| Role change has no effect | The old token still has the old role | Log in again |
| `409 NAME_TAKEN` on a course or tech stack | Name already exists | Use a different name, or update the existing record |
| Port already in use | Another program is using 8080 | Start with `--server.port=8090` and update `baseUrl` |
| `400 MALFORMED_REQUEST` | Body is not valid JSON | Check commas and quotes, and set `Content-Type` to `application/json` |

---

## 11. Limits and notes

- **Roles are coarse.** HR can read every student but cannot change anything. Finer rules would need new roles or permissions.
- **Logout does not revoke tokens** on the server. A copied token works until it expires.
- **Foreign keys are not enforced by SQLite** here. The services remove mapping rows themselves when a student, tech stack, or user is deleted.
- **CSRF protection is off.** The cookie is `SameSite=Strict`, which protects the app for now.
- **The database is SQLite** (`student.db`). Data stays between restarts. Delete the file to start fresh, then set the seed passwords again.
