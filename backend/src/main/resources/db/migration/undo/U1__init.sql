-- undo/V1__init.sql : tear down the initial schema
-- Run manually: psql $DATABASE_URL -f src/main/resources/db/migration/undo/V1__init.sql

DROP TRIGGER IF EXISTS trg_tasks_updated_at ON tasks;
DROP FUNCTION IF EXISTS set_updated_at();

DROP TABLE IF EXISTS tasks;
DROP TABLE IF EXISTS projects;
DROP TABLE IF EXISTS users;
