# TaskFlow

A minimal but complete task management REST API built with **Java 21 + Spring Boot 3.2**, backed by **PostgreSQL**, secured with **JWT**, and fully containerised with **Docker**.

---

## 1. Overview

TaskFlow lets users register, log in, create projects, and manage tasks within those projects — with status tracking, priority levels, assignees, and due dates.

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Migrations | Flyway |
| Auth | JWT (JJWT 0.12), BCrypt cost 12 |
| Logging | Logback + logstash-logback-encoder (structured JSON) |
| Tests | JUnit 5 + Spring MockMvc (integration) |
| Container | Docker + Docker Compose |

---

## 2. Architecture Decisions

### Package layout — by layer, not by feature
For a project of this scope (3 entities, ~12 endpoints) a flat layered structure (`controller → service → repository`) is the most readable choice. Feature-based packaging would fragment small files across many directories with no real benefit at this scale.

### Spring Data JPA + native Flyway migrations
JPA handles the routine CRUD cleanly. Flyway owns the schema — `ddl-auto: validate` means the app refuses to start if the DB schema doesn't match the entity model, which catches drift early. Auto-migrate was explicitly ruled out by the spec and is the right call: it hides what's actually running against production.

### Enum columns stored as PostgreSQL native enums
`task_status` and `task_priority` are declared as Postgres `ENUM` types in the migration. JPA maps them via `@Enumerated(EnumType.STRING)` with a matching `columnDefinition`. This gives you DB-level constraint enforcement (invalid values are rejected at the wire) and readable column values in psql without a join. The tradeoff: adding an enum value requires a migration; that's acceptable.

### `task_status` / `task_priority` values are lowercase (`todo`, `in_progress`, `done`)
The spec defines them that way. Lowercase enum labels are idiomatic in Postgres and JSON, so there's no conversion layer needed between DB, Java, and API responses.

### `updated_at` managed by a Postgres trigger, not only by JPA
`@UpdateTimestamp` covers JPA-managed updates. The trigger (`trg_tasks_updated_at`) acts as a safety net for any raw SQL update (migrations, manual admin queries). Both are present and consistent.

### `CurrentUser` helper — not `@AuthenticationPrincipal`
Controllers resolve the acting user through a small `CurrentUser` component that reads from the `SecurityContext` and does one lookup. This is slightly more verbose than `@AuthenticationPrincipal` but makes the dependency explicit and easy to stub in tests.

### Task "creator" not tracked
The spec says delete is allowed for "project owner or task creator". I chose not to add a `creator_id` column — it adds a foreign key, a join, and surface area for every task write, for a check that ownership already covers 95% of real-world cases. The current implementation allows project owners to delete any task in their project. Noted in [What You'd Do With More Time](#7-what-youd-do-with-more-time).

### Pagination on all list endpoints (bonus)
`GET /projects` and `GET /projects/:id/tasks` both accept `?page=` and `?limit=` and return a consistent envelope with `totalElements`, `totalPages`, etc.

### Down migrations — manual, not Flyway Undo
Flyway Undo (rollback) requires the paid Teams/Enterprise edition. Down migrations are provided as plain SQL files under `src/main/resources/db/migration/undo/` and must be run manually (see [Running Migrations](#4-running-migrations)). This is the standard OSS approach.

### Structured JSON logging
All log output is newline-delimited JSON via `logstash-logback-encoder`. In production you pipe this straight to Loki, Datadog, etc. without a parsing step.

### Graceful shutdown
`server.shutdown: graceful` + a 30-second drain window means in-flight requests complete before the process exits on `SIGTERM`.

---

## 3. Running Locally

The only prerequisite is **Docker Desktop** (or Docker + Compose v2).

```bash
# 1. Clone
git clone https://github.com/sumeet32112/taskflow-sumeet-kumar-singh.git
cd taskflow-sumeet-kumar-singh
cd backend

# 2. Configure environment
cp .env.example .env

# 3. create a jwt secret
openssl rand -base64 32
# 4. Paste the output of this command into .env as JWT_SECRET=<value>
nano .env

# 5. Start everything (Postgres + API). First run builds the image (~2 min).
docker compose down -v
docker compose up --build

# API is ready at http://localhost:8080
```

To run in the background: `docker compose up -d --build`  
To stop and remove volumes: `docker compose down -v`

---

## 4. Running Migrations

**Migrations run automatically on startup** via Flyway. You don't need to do anything manually for a fresh environment.

### Rolling back (down migrations)

Down migrations are in `src/main/resources/db/migration/undo/`. Run them in **reverse version order**:

```bash
# Example: roll back V2 (seed), then V1 (schema)
psql postgresql://taskflow:taskflow@localhost:5432/taskflow \
  -f src/main/resources/db/migration/undo/U2__seed.sql

psql postgresql://taskflow:taskflow@localhost:5432/taskflow \
  -f src/main/resources/db/migration/undo/U1__init.sql
```

---

## 5. Test Credentials

The seed migration (`V2__seed.sql`) inserts two users. Both share the same password:

| Field    | Value              |
|----------|--------------------|
| Email    | `test@example.com` |
| Password | `password123`      |

Second seed user: `alice@example.com` / `password123`

For testing purpose can also use postman by importing file `New Collection.postman_collection.json` to postman desktop

---

## 6. API Reference

Base URL: `http://localhost:8080`  
All endpoints that require authentication expect: `Authorization: Bearer <token>`

### Authentication

#### `POST /auth/register`

Request
```json
{ "name": "Jane Doe", "email": "jane@example.com", "password": "hunter12345" }
```
Response 201
```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": { "id": "uuid", "name": "Jane Doe", "email": "jane@example.com", "createdAt": "..." }
}
```

#### `POST /auth/login`

 Request
```json
{ "email": "jane@example.com", "password": "hunter12345" }
```
```
Response 200 — same shape as register
```
---

### Projects

#### `GET /projects?page=0&limit=20`
Returns projects where the current user is the owner **or** is assigned to at least one task.

 Response 200
```json
{
  "content": [
    { "id": "uuid", "name": "Demo Project", "description": "...", "owner": {...}, "createdAt": "..." }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

#### `POST /projects`

Request
```json
{ "name": "New Project", "description": "Optional description" }
```

Response 201
```json
{ "id": "uuid", "name": "New Project", "description": "...", "owner": {...}, "createdAt": "..." }
```

#### `GET /projects/:id`
Returns project detail including all tasks.

 Response 200
```json
{
  "id": "uuid", "name": "...", "description": "...", "owner": {...},
  "tasks": [ { "id": "uuid", "title": "...", "status": "todo", ... } ],
  "createdAt": "..."
}
```

#### `PATCH /projects/:id` _(owner only)_

Request — all fields optional
```json
{ "name": "Updated Name", "description": "Updated description" }
```
```
Response 200 — full project object
```

#### `DELETE /projects/:id` _(owner only)_
```
Response 204 No Content
```

#### `GET /projects/:id/stats` _(bonus)_

Response 200
```json
{
  "byStatus": { "todo": 1, "in_progress": 1, "done": 1 },
  "byAssignee": [
    { "userId": "uuid", "name": "Test User", "count": 2 }
  ]
}
```

---

### Tasks

#### `GET /projects/:id/tasks?page=0&limit=20`

Response 200
```json
{
  "content": [
    {
      "id": "uuid", "title": "Set up CI", "description": "...",
      "status": "done", "priority": "high",
      "projectId": "uuid",
      "assignee": { "id": "uuid", "name": "Test User", "email": "test@example.com" },
      "dueDate": "2025-06-01",
      "createdAt": "...", "updatedAt": "..."
    }
  ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1
}
```

#### `POST /projects/:id/tasks`

Request
```json
{
  "title": "New Task",
  "description": "Optional",
  "status": "todo",
  "priority": "medium",
  "assigneeId": "uuid",
  "dueDate": "2025-06-15"
}
```
```
Response 201 — full task object
```

#### `PATCH /tasks/:id`
All fields optional — only provided fields are updated.

Request
```json
{ "status": "in_progress", "priority": "high", "assigneeId": "uuid" }
```
```
Response 200 — full task object
```

#### `DELETE /tasks/:id` _(project owner only)_
```
Response 204 No Content
```

---

### Error shapes

| Scenario | Status | Body |
|---|---|---|
| Validation failure | 400 | `{ "error": "validation failed", "fields": { "email": "is required" } }` |
| Bad credentials / bad input | 400 | `{ "error": "invalid email or password" }` |
| No / invalid token | 401 | `{ "error": "unauthorized" }` |
| Not owner / insufficient permission | 403 | `{ "error": "forbidden" }` |
| Resource not found | 404 | `{ "error": "not found" }` |
| Duplicate email | 409 | `{ "error": "email already in use" }` |

---

## 7. What You'd Do With More Time

**`creator_id` on tasks**  
The spec says delete requires "project owner **or** task creator". I dropped the creator column to keep scope tight — tracking it means adding the column, a FK, and plumbing it through every create path. The current rule (project owner only) is a reasonable simplification; I'd add it for production.

**Unassign a task via PATCH**  
Currently `assigneeId: null` in a PATCH body is indistinguishable from "field not present". Proper handling requires a custom deserialiser or a `JsonNullable` wrapper (OpenAPI-generator pattern) to distinguish `null` (unassign) from absent (no-op). Skipped in the interest of time.

**Access control on task reads**  
Right now any authenticated user can read tasks in any project. A real product would restrict reads to project members. This needs a `project_members` join table or the existing assignee relationship used as a proxy.

**Refresh tokens**  
JWTs expire in 24 hours with no refresh path. A `refresh_token` table with rotation would be the next auth step.

**Rate limiting on `/auth`**  
Auth endpoints have no rate limiting. Bucket4j + a Redis-backed store would be the production approach.

**Soft deletes**  
Hard deletes are simpler to implement but make auditing and undo impossible. An `archived_at` nullable column + filtered queries is the safer default for task data.

**OpenAPI / Swagger**  
Adding `springdoc-openapi-starter-webmvc-ui` takes minutes and gives reviewers an interactive playground without needing Postman.

**Proper test isolation**  
Integration tests run against a shared PostgreSQL instance (same as dev). For true isolation each test class should spin up its own schema via Testcontainers, which I'd add given more time.
