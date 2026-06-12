# ── Stage 1: Build ────────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

# Native-dep support (e.g. node-gyp, openssl bindings)
RUN apk add --no-cache openssl libc6-compat

WORKDIR /app

COPY package*.json ./
# Use npm install (not npm ci) for cross-platform lockfile compatibility
RUN npm install

COPY . .
RUN npm run build -- --configuration production

# ── Stage 2: Serve ────────────────────────────────────────────────────────────
FROM nginx:alpine AS runner

# Copy compiled Angular app (Angular 17+ outputs to browser/ subfolder)
COPY --from=builder /app/dist/planner-frontend/browser /usr/share/nginx/html

# SPA routing + optional local-compose /api/ proxy
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
