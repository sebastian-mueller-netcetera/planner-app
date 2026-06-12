-- V1__create_schema.sql
-- PlannerApp initial schema — all 7 tables
-- Requires PostgreSQL 13+ (gen_random_uuid() built-in via pgcrypto / pg13 core)

-- ─────────────────────────────────────────────────────────────────────────────
-- Enable pgcrypto so gen_random_uuid() is available on PG < 13
-- On PG 13+ this is a no-op safe to keep.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. users
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    email                           TEXT        UNIQUE NOT NULL,
    display_name                    TEXT        NOT NULL,
    password_hash                   TEXT        NOT NULL,
    google_refresh_token_encrypted  TEXT        NULL,
    google_calendar_a_id            TEXT        NULL,
    google_calendar_b_id            TEXT        NULL,
    created_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at                      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. sprints  (created before tasks because tasks.sprint_id → sprints.id)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE sprints (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        TEXT        UNIQUE NOT NULL,
    iso_year    INT         NOT NULL,
    iso_week    INT         NOT NULL,
    start_date  DATE        NOT NULL,
    end_date    DATE        NOT NULL,
    status      TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. tasks
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE tasks (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    title                   TEXT        NOT NULL,
    description             TEXT        NULL,
    status                  TEXT        NOT NULL DEFAULT 'BACKLOG',
    assignee_id             UUID        NULL REFERENCES users(id) ON DELETE SET NULL,
    due_date                DATE        NULL,
    sprint_id               UUID        NULL REFERENCES sprints(id) ON DELETE SET NULL,
    sync_google_calendar_a  BOOLEAN     NOT NULL DEFAULT FALSE,
    sync_google_calendar_b  BOOLEAN     NOT NULL DEFAULT FALSE,
    archived_at             TIMESTAMPTZ NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Scalar indexes
CREATE INDEX idx_tasks_status      ON tasks (status);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
CREATE INDEX idx_tasks_sprint_id   ON tasks (sprint_id);
CREATE INDEX idx_tasks_due_date    ON tasks (due_date);
CREATE INDEX idx_tasks_archived_at ON tasks (archived_at);

-- Full-text search index (German dictionary)
CREATE INDEX idx_tasks_fts ON tasks
    USING GIN (to_tsvector('german', title || ' ' || COALESCE(description, '')));

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. labels
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE labels (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name       TEXT        UNIQUE NOT NULL,
    color      TEXT        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. task_labels  (join table — composite PK, cascade deletes)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE task_labels (
    task_id   UUID NOT NULL REFERENCES tasks(id)  ON DELETE CASCADE,
    label_id  UUID NOT NULL REFERENCES labels(id) ON DELETE CASCADE,
    PRIMARY KEY (task_id, label_id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. comments
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE comments (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id    UUID        NOT NULL REFERENCES tasks(id)  ON DELETE CASCADE,
    author_id  UUID        NOT NULL REFERENCES users(id)  ON DELETE RESTRICT,
    body       TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. task_calendar_syncs
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE task_calendar_syncs (
    id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id           UUID        NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    calendar_key      TEXT        NOT NULL,
    external_event_id TEXT        NOT NULL,
    sync_status       TEXT        NOT NULL,
    last_synced_at    TIMESTAMPTZ NULL,
    last_error        TEXT        NULL,
    CONSTRAINT uq_task_calendar_syncs_task_calendar UNIQUE (task_id, calendar_key)
);
