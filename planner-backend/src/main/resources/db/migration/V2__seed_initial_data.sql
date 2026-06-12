-- V2__seed_initial_data.sql
-- Seeds 2 users (Martina + Sebastian) and 8 initial labels.
-- Password hash is bcrypt cost-10 of 'hikeplanner2024'.

-- ─────────────────────────────────────────────────────────────────────────────
-- Users
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO users (id, email, display_name, password_hash)
VALUES
    (gen_random_uuid(), 'martina@planner.local',  'Martina',  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'),
    (gen_random_uuid(), 'sebastian@planner.local', 'Sebastian', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy');

-- ─────────────────────────────────────────────────────────────────────────────
-- Labels
-- ─────────────────────────────────────────────────────────────────────────────
INSERT INTO labels (id, name, color)
VALUES
    (gen_random_uuid(), 'Hausbau',  '#E57373'),
    (gen_random_uuid(), 'Sizilien', '#64B5F6'),
    (gen_random_uuid(), 'Theresa',  '#81C784'),
    (gen_random_uuid(), 'Martina',  '#CE93D8'),
    (gen_random_uuid(), 'Sebi',     '#4DB6AC'),
    (gen_random_uuid(), 'Galina',   '#FFB74D'),
    (gen_random_uuid(), 'Mercedes', '#90A4AE'),
    (gen_random_uuid(), 'Haushalt', '#A1887F');
