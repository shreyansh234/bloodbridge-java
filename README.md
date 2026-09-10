# BloodBridge — Full Stack Java mini project

BloodBridge connects voluntary blood donors and blood seekers through blood-group and location search. This is the Java project described in the supplied FSJP presentation. The compiled responsive frontend is included, so Node.js is not needed to run it.

**Stack:** Java 17, Spring Boot 3.5.16, Spring Security, REST API, Hibernate/JPA, MySQL 8, Flyway migrations, HTML/CSS/JavaScript (React frontend).

## Run with Docker Desktop (easiest)

1. Install and open Docker Desktop on your computer.
2. Extract this project ZIP and open a terminal in the folder containing `compose.yaml`.
3. Copy `.env.example` to `.env`.
4. Open `.env` and replace `DB_PASSWORD` and `DB_ROOT_PASSWORD` with two different passwords of your own. These are local database passwords, not your website login.
5. Run `docker compose up --build` and wait until Spring Boot has started. The first run downloads Java, Maven and MySQL images and dependencies, so an internet connection is needed.
6. Open **http://localhost:8080**.
7. Click **Sign in → Create an account**. Then click **Become a donor** and complete the donor form.
8. Stop with `docker compose down`. Your database stays in the Docker volume. Do not add `-v` unless you intentionally want to erase it.

## Run with Java + Maven + MySQL

Install JDK 17 or newer, Maven 3.6.3 or newer, and MySQL 8. Open MySQL as an administrator and run the following, choosing your own password:

```sql
CREATE DATABASE bloodbridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'bloodbridge'@'localhost' IDENTIFIED BY 'REPLACE_WITH_YOUR_PASSWORD';
GRANT ALL PRIVILEGES ON bloodbridge.* TO 'bloodbridge'@'localhost';
```

In Windows PowerShell, from this project folder:

```powershell
$env:DB_PASSWORD="REPLACE_WITH_YOUR_PASSWORD"
mvn clean verify
mvn spring-boot:run
```

On macOS/Linux:

```bash
export DB_PASSWORD='REPLACE_WITH_YOUR_PASSWORD'
mvn clean verify
mvn spring-boot:run
```

Open **http://localhost:8080**. MySQL tables are created through the included Flyway migration. Defaults: database `bloodbridge`, database user `bloodbridge`, MySQL on `localhost:3306`. Override `DB_URL` and `DB_USER` if your setup differs.

## Optional demo without MySQL

For a quick local demonstration with a persistent H2 file database, run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

This demo writes to `data/` on your computer. It is an optional demo mode; use MySQL for the full-stack project submission. No test users or fake contact numbers are seeded. The optional example profiles in the interface are visibly fictional and cannot be contacted.

## Features

- Account creation, login and logout; BCrypt password hashes; server-side sessions.
- Donor registration, reading, update and deletion.
- Exact blood group and city/locality filtering, availability filter and pagination.
- Authenticated contact access. Phone numbers do not appear in donor-list responses.
- Paused donors cannot have their contact details retrieved.
- Each signed-in account can edit only its own donor profile.
- Consent before publication; full account email is never public.
- Real database counts; clear loading, empty, success and error states.
- CSRF protection and bounded rate limits for login, registration and contact lookup.
- Mobile layout, accessible dialogs, labeled fields and validation.

## API

| Method | Endpoint | Access |
|---|---|---|
| GET | `/api/session` | Session + CSRF token; anonymous allowed |
| POST | `/api/auth/register` | Create account; CSRF token required |
| POST | `/api/auth/login` | Login; CSRF token required |
| POST | `/api/auth/logout` | Signed-in user + CSRF token |
| GET | `/api/donors?group=O%2B&city=Thane&available=true&page=1` | Public; no phone numbers |
| GET | `/api/donors/{id}/contact` | Signed-in user; available donors only |
| GET | `/api/profile` | Own profile |
| PUT | `/api/profile` | Create/update own profile; CSRF token |
| PATCH | `/api/profile/availability` | Update own availability; CSRF token |
| DELETE | `/api/profile` | Delete own donor profile; CSRF token |

Use the `csrf` value from `GET /api/session` as `X-CSRF-TOKEN` on state-changing requests and keep the session cookie. Fetch a fresh token after login/logout. The included frontend handles this automatically.

## Code layout

- `src/main/java/com/bloodbridge/`: Java application, entities, repositories, security and controllers.
- `src/main/resources/db/migration/mysql/`: MySQL schema migrations.
- `src/main/resources/static/`: complete compiled frontend, served by Spring Boot.
- `src/test/`: Spring MVC integration flow for authentication, contact privacy and donor lifecycle.
- `frontend/` (in the downloadable ZIP): editable frontend source and its build script. Run `npm ci`, then `npm run build` inside `frontend/` after making changes. The output replaces the Spring Boot static resources.

## Validation status and scope

The shared frontend production build and hosted database API integration checks passed. They cover registration, filtering, update, ownership, contact protection, availability and deletion. Java integration tests are included, but could not be executed in the creation environment because Maven, a JDK compiler and MySQL/Docker were unavailable and Maven Central could not be reached. Run `mvn clean verify` on your machine before submission; this runs the Java tests with H2. Test the MySQL configuration as well for your final demonstration.

The separately published private Site uses the same frontend with the platform's sign-in and persistent D1 database. It is **not** a Java/MySQL deployment. This folder is the separate Java/MySQL implementation. Their user accounts and donor records are separate.

This is a college connection-platform project. It does not verify phone ownership or provide email verification/recovery, automated compatibility decisions, notifications, or blood-bank inventory. Eligibility, blood compatibility and collection arrangements belong to the treating hospital or blood bank. Do not treat sample profiles as available donors.

For a later internet deployment of this Java project, use HTTPS, set `COOKIE_SECURE=true`, keep database credentials outside source control, and place any multi-instance rate limiting in a shared store. Current rate limits are process-local; MySQL application records are persistent. The Docker setup intentionally listens on your computer only.

## Project team

K. C. College of Engineering & Management Studies & Research, Thane.
FSJP Mini Project, Second Year Computer Engineering, 2026–27.
Shaikh Yaseen · Shreyansh Singh · Sparsh Patel · Soham Kanase.
Guide: Dr. Nita Patil.

## References

- [Spring Boot 3.5 system requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html)
- [Spring Security: persisting authentication](https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html)
- [Spring Security: CSRF protection](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [WHO: blood donor selection and assessment](https://www.who.int/publications/i/item/9789241548519)
