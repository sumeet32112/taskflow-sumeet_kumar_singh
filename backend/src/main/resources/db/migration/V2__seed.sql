-- V2__seed.sql : test data
-- Password for all seed users is: password123
-- bcrypt hash generated at cost 12

INSERT INTO users (id, name, email, password) VALUES
    ('00000000-0000-0000-0000-000000000001',
     'Test User',
     'test@example.com',
     '$2a$12$c9dxCw.AU4TVl4C8QCLgE.KcpMPKMK14T8V9qOG/CpbNy9t3KdBOi'),

    ('00000000-0000-0000-0000-000000000002',
     'Alice Johnson',
     'alice@example.com',
     '$2a$12$c9dxCw.AU4TVl4C8QCLgE.KcpMPKMK14T8V9qOG/CpbNy9t3KdBOi');

INSERT INTO projects (id, name, description, owner_id) VALUES
    ('00000000-0000-0000-0000-000000000010',
     'Demo Project',
     'A seeded project for manual testing',
     '00000000-0000-0000-0000-000000000001');

INSERT INTO tasks (id, title, description, status, priority, project_id, assignee_id) VALUES
    ('00000000-0000-0000-0000-000000000020',
     'Set up CI pipeline',
     'Configure GitHub Actions for build and test',
     'done',
     'high',
     '00000000-0000-0000-0000-000000000010',
     '00000000-0000-0000-0000-000000000001'),

    ('00000000-0000-0000-0000-000000000021',
     'Write API documentation',
     'Document all endpoints in README',
     'in_progress',
     'medium',
     '00000000-0000-0000-0000-000000000010',
     '00000000-0000-0000-0000-000000000002'),

    ('00000000-0000-0000-0000-000000000022',
     'Add rate limiting',
     'Protect auth endpoints with rate limiting middleware',
     'todo',
     'low',
     '00000000-0000-0000-0000-000000000010',
     NULL);
