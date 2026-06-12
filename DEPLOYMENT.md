# PlannerApp — Coolify Deployment Guide

**Server:** 167.233.29.115  
**Coolify UI:** http://167.233.29.115:8000  
**Domains:**
- Frontend: `planner.sebastian-mueller.li`
- Backend API: `api.planner.sebastian-mueller.li`

---

## 1. DNS — A Records

Add two A records at your DNS provider before starting (Coolify needs them for TLS cert provisioning):

| Hostname | Type | Value |
|---|---|---|
| `planner.sebastian-mueller.li` | A | `167.233.29.115` |
| `api.planner.sebastian-mueller.li` | A | `167.233.29.115` |

Propagation can take up to 30 minutes. Verify with:
```bash
dig +short planner.sebastian-mueller.li
dig +short api.planner.sebastian-mueller.li
```

---

## 2. Create a New Coolify Project

1. Log in to Coolify at http://167.233.29.115:8000
2. Click **Projects** → **+ New Project**
3. Name it `PlannerApp`
4. Select your server (`167.233.29.115`)

> Do NOT reuse the existing `HikePlanner` project (`trak7sup1yi4rzfrdzlpn3yo`).

---

## 3. Create PostgreSQL Resource

1. Inside the `PlannerApp` project click **+ New Resource** → **Database** → **PostgreSQL**
2. Set:
   - Image tag: `16-alpine`
   - Database name: `planner`
   - Username: `planner`
   - Password: *(generate a strong random password)*
3. Save and **Start** the database
4. Copy the internal connection string — you'll need it for the backend env vars

---

## 4. Create Backend Application Resource

1. **+ New Resource** → **Application** → **GitHub** (repository: `sebastian-mueller-netcetera/PlannerApp`)
2. Branch: `main`
3. **Build Pack**: `Dockerfile`
4. **Dockerfile location**: `planner-backend/Dockerfile`
5. **Port**: `8080`
6. **Health check path**: `/api/v1/actuator/health`

### Backend Environment Variables

Set all of the following in the **Environment Variables** tab:

```
SPRING_DATASOURCE_URL=jdbc:postgresql://<coolify-postgres-host>:<port>/planner
SPRING_DATASOURCE_USERNAME=planner
SPRING_DATASOURCE_PASSWORD=<strong-password-from-step-3>
SPRING_JPA_HIBERNATE_DDL_AUTO=validate
SPRING_FLYWAY_ENABLED=true
JWT_SECRET=<openssl rand -base64 48>
JWT_EXPIRATION_MS=86400000
CORS_ALLOWED_ORIGINS=https://planner.sebastian-mueller.li
GOOGLE_CLIENT_ID=<from Google Cloud Console>
GOOGLE_CLIENT_SECRET=<from Google Cloud Console>
GOOGLE_REDIRECT_URI=https://api.planner.sebastian-mueller.li/api/v1/google/callback
APP_BASE_URL=https://planner.sebastian-mueller.li
SERVER_PORT=8080
```

> **Important:** `SPRING_DATASOURCE_URL` must use the *internal* hostname Coolify assigns to the  
> PostgreSQL container, not `localhost`. Find it in the database resource details page.

6. **Domain**: `api.planner.sebastian-mueller.li`
7. Click **Deploy**

---

## 5. Create Frontend Application Resource

1. **+ New Resource** → **Application** → **GitHub** (same repository)
2. Branch: `main`
3. **Build Pack**: `Dockerfile`
4. **Dockerfile location**: `planner-frontend/Dockerfile`
5. **Port**: `80`
6. **Health check path**: `/`

### Frontend Environment Variables

The Angular app is compiled at build time; runtime env vars are baked in via `environment.ts`.  
No runtime env vars needed for the nginx runner itself unless you add a config injection step.

If you add `NGINX_BACKEND_URL` injection in the future, set it here.

7. **Domain**: `planner.sebastian-mueller.li`
8. Click **Deploy**

> **Note:** The `nginx.conf` shipped in this repo includes a `/api/` proxy_pass block for  
> local docker-compose use. In Coolify production the frontend and backend have separate  
> domains — the Angular app calls `https://api.planner.sebastian-mueller.li` directly,  
> so the proxy_pass block is harmless but unused.

---

## 6. Google OAuth — Authorized Redirect URIs

In **Google Cloud Console → APIs & Services → Credentials → OAuth 2.0 Client**:

Add authorized redirect URIs:
```
https://api.planner.sebastian-mueller.li/api/v1/google/callback
http://localhost:8080/api/v1/google/callback   ← for local dev
```

---

## 7. Verify Deployment

```bash
# Backend health
curl https://api.planner.sebastian-mueller.li/api/v1/actuator/health

# Frontend reachable
curl -I https://planner.sebastian-mueller.li

# SPA routing (should return 200, not 404)
curl -I https://planner.sebastian-mueller.li/some/deep/route
```

---

## 8. Known Issues & Lessons from HikePlanner

| Issue | Fix |
|---|---|
| `npm install` fails on alpine with `ENOENT openssl` | `apk add --no-cache openssl libc6-compat` added to frontend Dockerfile builder stage |
| Docker COPY fails on empty directories | Add `.gitkeep` files to empty dirs (e.g. `src/assets/.gitkeep`) |
| Platform-specific lockfile errors with `npm ci` | Use `npm install` instead of `npm ci` |
| Native node binaries break in runner | Copy full `node_modules` from builder to runner if needed |
| Coolify shows build success but container crashes | Check that the jar wildcard `target/*.jar` matches exactly one file |
| `mvnw` not executable | Commit with `git update-index --chmod=+x planner-backend/mvnw` |

---

## 9. Local Development

```bash
# 1. Copy env file
cp .env.example .env
# Edit .env with your values

# 2. Start all services
docker compose up --build

# Services:
#   Frontend:  http://localhost:4200
#   Backend:   http://localhost:8080
#   Postgres:  localhost:5432
```

To rebuild a single service:
```bash
docker compose up --build planner-backend
```

To reset the database volume:
```bash
docker compose down -v
```
