# HireHub — Job Portal & Authentication System

A complete Spring Boot application combining **HireHub** job workflows and **AuthApp** authentication into one project.

**Java 17 · Spring Boot 3.5 · Spring MVC · Spring Security · Hibernate/JPA · DAO · MySQL · Thymeleaf · JUnit 5**

## Features

- Registration with normalized unique emails and BCrypt password hashing.
- Session login/logout, session fixation protection, 30-minute idle timeout, HttpOnly cookies, CSRF protection, and security headers.
- Admin/User authorization through Spring Security's servlet filter chain plus service-level role checks.
- Searchable, paginated job listings and individual role pages.
- Admin job creation, editing, closing, and reopening through edit.
- User applications with cover letters, duplicate prevention, status tracking, and withdrawal.
- Admin applicant review and registered-user listing.
- JSON API sharing the same business logic as the server-rendered UI.
- Flyway schema migrations, database constraints, integration tests, Postman collection, Docker Compose, and GitHub CI.

## Quick start: temporary demo database

Install JDK 17 or newer (through Java 25). Maven Wrapper scripts are included, so a separate Maven installation is optional. From this directory:

```powershell
$env:ADMIN_EMAIL = 'admin@example.test'
$env:ADMIN_PASSWORD = 'Choose-your-own-password-123'
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=demo'
```

On macOS/Linux:

```sh
ADMIN_EMAIL=admin@example.test ADMIN_PASSWORD='Choose-your-own-password-123' ./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Open http://localhost:8080. Sign in as the configured admin, post a job, then log out and register a User to apply. Demo mode uses an empty in-memory H2 database and loses data on shutdown. There are no built-in accounts or sample jobs. Admin credentials are only required when creating an admin.

## Persistent MySQL: Docker Compose

Install Docker with Compose. Copy `.env.example` to `.env` and replace all password placeholders with your own values. Then:

```sh
docker compose up --build -d
docker compose logs -f app
```

Open http://localhost:8080. MySQL data persists in the `mysql_data` volume. `docker compose down` stops the services while retaining the data. The app binds to localhost; MySQL is not exposed on the host.

## Existing MySQL server

Create a database and a dedicated account with access to it. Set `DB_URL` (for example `jdbc:mysql://localhost:3306/hirehub`), `DB_USERNAME`, and `DB_PASSWORD` in your shell. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` for initial admin creation, then run `mvn spring-boot:run` without the demo profile. Flyway creates the tables automatically; Hibernate validates their structure.

An existing admin's password is never reset on startup. Bootstrap refuses to elevate an existing User with the same email. Remove the bootstrap environment variables after initial persistent setup. Public registration always creates USER accounts.

## Build and test

Use `./mvnw` on macOS/Linux or `.\mvnw.cmd` in PowerShell in place of `mvn` below if Maven is not installed.

```sh
mvn verify
java -jar target/hirehub-1.0.0.jar --spring.profiles.active=demo
```

JUnit/MockMvc integration tests use the actual Spring context, security filters, services, repositories, migrations, and H2. They verify password hashing, duplicate registration, invalid inputs, CSRF, login/logout, session rotation, authorization, job editing/closing, application ownership and state transitions, and Thymeleaf rendering.

To run the same tests against a **dedicated disposable MySQL database**, set the DB variables and run:

```sh
mvn test -Dspring.profiles.active=mysqltest
```

Tests delete users, jobs, and applications before each case. Never point test commands at a database containing data you want to keep. GitHub CI runs both H2 and a dedicated MySQL 8.4 service.

## Architecture

```text
Browser / Postman
  → Spring Security servlet filter chain (session + CSRF + roles)
  → PageController / ApiController (validation + presentation)
  → PortalService (transactions + authorization + business rules)
  → UserDao / JobDao / ApplicationDao (Spring Data JPA DAO interfaces)
  → Hibernate → MySQL
```

`users` has unique email and BCrypt password hash; `jobs` stores job details and open/closed state; `applications` references a user and job with a unique `(user_id, job_id)` constraint. Closing jobs preserves application history. Pessimistic job locks serialize closing and submitting; optimistic versions prevent lost application updates.

Application transitions: `SUBMITTED → REVIEWING / ACCEPTED / REJECTED / WITHDRAWN`; `REVIEWING → ACCEPTED / REJECTED / WITHDRAWN`. Users can only withdraw their own pending applications. Admins cannot change final applications. One application per user per job remains enforced after withdrawal. Admins do not apply for jobs.

## Pages

| Access | Routes |
|---|---|
| Public | `/jobs`, `/jobs/{id}`, `/login`, `/register` |
| User | `/applications` |
| Admin | `/admin/jobs`, `/admin/jobs/new`, `/admin/jobs/{id}/edit`, `/admin/applications`, `/admin/users` |

## REST API

All writes require a CSRF token, including registration and login. Fetch `GET /api/csrf`, retain the session cookie, and send its token using the returned header name (`X-CSRF-TOKEN`). Login uses `POST /login` with **form URL-encoded** fields `username` (email) and `password`. It returns a redirect on success or failure. Fetch a **new CSRF token after login/logout** because authentication changes the token. `GET /api/me` confirms the authenticated identity. Logout is `POST /logout`.

| Method | Route | Access |
|---|---|---|
| GET | `/api/csrf` | Public |
| POST | `/api/auth/register` | Public |
| GET | `/api/me` | Signed in |
| GET | `/api/jobs?q=&page=0` | Public |
| GET | `/api/jobs/{id}` | Public |
| POST | `/api/applications/job/{id}` | User |
| GET | `/api/applications?page=0` | User |
| POST | `/api/applications/{id}/withdraw` | User, owner only |
| GET/POST | `/api/admin/jobs` | Admin |
| PUT | `/api/admin/jobs/{id}` | Admin |
| DELETE | `/api/admin/jobs/{id}` | Admin; closes instead of deleting history |
| GET | `/api/admin/applications?page=0` | Admin |
| PATCH | `/api/admin/applications/{id}/status` | Admin |
| GET | `/api/admin/users?page=0` | Admin |

Job JSON: `{"title":"Java Developer","company":"Example","location":"Remote","description":"Build Java services","active":true}`.

Registration JSON: `{"name":"Candidate","email":"candidate@example.test","password":"Your-password-123"}`.

Application JSON: `{"coverLetter":"My relevant experience..."}`. Status JSON: `{"status":"REVIEWING"}`.

Lists are paginated at 20 records per page. API errors use HTTP 400 (validation), 401 (not signed in), 403 (role/CSRF), 404 (missing or non-owned resource), or 409 (duplicate/final/closed/conflicting update). Responses never include password hashes.

## Postman

Import `postman/HireHub.postman_collection.json`. Set collection variables `baseUrl`, `email`, and `password` locally. Do not export real passwords into source control. Run **Get CSRF**, optionally **Register**, **Login**, then **Get CSRF** again. Keep Postman's cookie jar enabled. Admin and User requests require the corresponding signed-in role. Create/apply requests capture IDs into collection variables. Switch accounts by logging out and repeating token/login/token steps. The collection includes response assertions; running it requires a live app and your chosen credentials.

## Operational scope

This is a portfolio application with end-to-end core workflows. Before internet deployment, configure HTTPS and `COOKIE_SECURE=true`, external secret management, database backups, monitoring, and gateway login rate limiting. Password reset, email verification, MFA, resume uploads, and email notifications are outside this project's scope. Passwords use 12–72 printable ASCII characters to keep BCrypt's byte limit explicit. The frontend escapes user text and does not render submitted HTML.

## Combined resume entry

**HireHub — Secure Job Portal & Authentication System | Java · Spring Boot · Spring Security · Hibernate/JPA · MySQL · Thymeleaf · JUnit**

- Built an integrated job portal with registration, BCrypt authentication, session management, and Admin/User authorization using Spring Security servlet filters.
- Implemented job posting, search, applications, applicant review, and ownership-protected withdrawals using Spring MVC, a service layer, and JPA DAO interfaces.
- Designed a versioned relational schema and added automated integration coverage, a Postman collection, Docker setup, and GitHub CI for repeatable validation.

Only claim Postman execution or CI success after actually running those checks in your environment.

## References

- [Spring Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
