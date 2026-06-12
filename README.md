# PlannerApp

Lightweight planning application with Google Calendar integration.

## Monorepo Layout

```
PlannerApp/
├── planner-frontend/   Angular 19 (standalone, Angular Material)
├── planner-backend/    Spring Boot 3 / Java 21 (Maven)
├── deployment/         Coolify / Docker Compose files
├── .gitignore
└── README.md
```

## Tech Stack

| Layer     | Technology                              |
|-----------|-----------------------------------------|
| Frontend  | Angular 19, Angular Material 19, SCSS   |
| Backend   | Spring Boot 3.3, Java 21, Maven         |
| Database  | PostgreSQL + Flyway migrations          |
| Auth      | JWT (JJWT 0.12.x) + Spring Security     |
| Calendar  | Google Calendar API v3                  |
| Deploy    | Coolify (Docker)                        |

## Prerequisites

- Node.js ≥ 22 & npm ≥ 10
- Java 21
- Docker (for local PostgreSQL)
- Angular CLI 19: `npm install -g @angular/cli@19`

## Quick Start

### Frontend

```bash
cd planner-frontend
npm install
ng serve
# → http://localhost:4200
```

### Backend

```bash
cd planner-backend
# Start local DB
docker run -d --name planner-pg \
  -e POSTGRES_DB=planner \
  -e POSTGRES_USER=planner \
  -e POSTGRES_PASSWORD=planner \
  -p 5432:5432 postgres:16-alpine

# Run
./mvnw spring-boot:run
# → http://localhost:8080
```

## Environment Variables (Backend)

| Variable                  | Default (dev)                              | Description            |
|---------------------------|--------------------------------------------|------------------------|
| `SPRING_DATASOURCE_URL`   | `jdbc:postgresql://localhost:5432/planner` | JDBC URL               |
| `SPRING_DATASOURCE_USERNAME` | `planner`                               | DB user                |
| `SPRING_DATASOURCE_PASSWORD` | `planner`                               | DB password            |
| `JWT_SECRET`              | `dev-secret-change-me`                     | JWT signing secret     |
| `CORS_ALLOWED_ORIGINS`    | `http://localhost:4200`                    | Allowed CORS origins   |

> **Never commit real secrets.** Override via environment or a `.env` file (git-ignored).
