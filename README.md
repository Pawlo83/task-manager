# Task Manager

![Project Status](https://img.shields.io/badge/status-completed-blue)
![Backend](https://img.shields.io/badge/backend-Spring_Boot_4-blue)
![Frontend](https://img.shields.io/badge/frontend-React_19-blue)
![Database](https://img.shields.io/badge/database-PostgreSQL-blue)
![License](https://img.shields.io/badge/license-MIT-blue)

A full-stack kanban-style task manager. The backend started as a recruitment task, a basic Spring Boot REST API for task CRUD, and has since grown into a modern application with JWT authentication, CSRF protection and a React frontend with a comprehensive test suite.

![GIF](TaskManager.gif)

## Key features
* **Authentication:** Cookie-based JWT authentication with BCrypt password hashing and per-user task isolation enforced at the repository level.
* **Kanban board:** Three-column board (To Do / In Progress / Done) with inline taks editing and sorting.
* **Input validation:** Server-side constraints on all DTOs with structured error responses, client-side length hints in the form.
* **Error handling:** `GlobalExceptionHandler` maps every failure to a typed JSON shape and the frontend translates status codes into human-readable messages.
* **API documentation:** Swagger UI available at `/swagger-ui/index.html`, disabled in production.
* **Deployment-ready:** Supports both same-host and split-host deployments via two property profiles and a single env-var toggle.
* **Test coverage:** Unit tests (service, JWT), controller slice tests (`@WebMvcTest`) and full-stack integration tests (`@SpringBootTest`).

## Architecture and technologies

Three-tier client-server architecture. All communication is stateless over HTTP/HTTPS with JSON as the exchange format. The frontend communicates with the backend either through the Vite dev proxy when hosted locally or directly via `VITE_API_BASE_URL` in production environment.

### Backend

* **Language:** Java 21
* **Framework:** Spring Boot 4.0.4
* **Auth:** JWT in `HttpOnly` cookie + CSRF token in `XSRF-TOKEN` cookie
* **Dev DB:** H2 in-memory
* **Prod DB:** PostgreSQL via Supabase

The backend is a stateless REST API. Authentication is handled entirely through cookies, the JWT lives in an HttpOnly cookie and the CSRF token in a XSRF-TOKEN cookie. Stateless JWT and CSRF don't play well together by default - Spring Security's CsrfAuthenticationStrategy rotates the token on every authenticated request, which causes a race condition when the frontend fires concurrent GETs and each response overwrites the XSRF-TOKEN cookie with a different value and the next mutation fails with 403 state. This is solved with a custom CsrfTokenRepository wrapper that makes saveToken() function a no-op when a valid cookie already exists, keeping the token stable for the lifetime of the cookie.
Task ownership is enforced at the repository level rather than the service layer, so every query includes AND user_id = ? clause. User requesting another user's task ID gets a 404 rather than a 403, which avoids confirming the resource exists.

### Frontend

* **Language:** TypeScript
* **Framework:** React 19 + Vite 8
* **Styling:** Tailwind CSS v4
* **Routing:** React Router v7

The frontend is a single-page app with cookie-based auth. On startup it calls GET /ping to initialise the XSRF-TOKEN cookie and get the status of backend, then GET /me to restore the session if a JWT cookie already exists. All API calls go through a shared apiFetch wrapper that converts network failures into a typed ApiError(status: null), distinct from HTTP errors, so the UI can show user friendly messages rather than a generic failure.
The CSRF token is always read live from document.cookie rather than cached in memory, this avoids stale-token bugs if the cookie is ever refreshed between requests.

## Setup

### Prerequisites

* Java 21+
* Node.js 20+ (or Bun)
* Maven (or use the included `mvnw` wrapper)

### Backend — local dev

```bash
cd task-manager-backend
./mvnw spring-boot:run          # Linux / Mac
mvnw.cmd spring-boot:run        # Windows
```

The H2 dev database is created in-memory on startup. H2 console: `http://localhost:8080/dbconsole`  
Swagger UI: `http://localhost:8080/swagger-ui/index.html`

### Frontend — local dev

```bash
cd task-manager-frontend
npm install       # or: bun install
npm run dev       # or: bun dev
```

App: `http://localhost:5173` — the Vite proxy forwards `/api` to `localhost:8080`.

### Running tests

```bash
cd task-manager-backend
./mvnw test
```

## Deployment

### Same-host backend
| Variable | Description |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>:5432/postgres` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Random string, minimum 64 characters |
| `CORS_ALLOWED_ORIGINS` | `https://yourdomain.com` |
| `APP_SECURE_COOKIES` | `true` |
| `APP_CROSS_ORIGIN` | `false` |
| `SPRING_PROFILES_ACTIVE` | `prod` |

### Split-host backend

Same backend variables as above, plus:

| Variable | Value |
|---|---|
| `APP_CROSS_ORIGIN` | `true` — enables `SameSite=None; Secure` cookies |
| `CORS_ALLOWED_ORIGINS` | `https://your-frontend.address` |

### Split-host frontend
| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://your-backend.render.com` |
