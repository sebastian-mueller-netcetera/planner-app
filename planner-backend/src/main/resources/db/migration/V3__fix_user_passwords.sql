-- V3__fix_user_passwords.sql
-- Fixes the wrong bcrypt hashes seeded in V2.
-- Correct hash is bcrypt cost-10 of 'hikeplanner2024'.

UPDATE users
SET password_hash = '$2a$10$iYzfYk7vpVAft8ZJwzqyQOBsOqIM5etT2OrimeM8oN26EGpCGaJBG'
WHERE email IN ('martina@planner.local', 'sebastian@planner.local');
