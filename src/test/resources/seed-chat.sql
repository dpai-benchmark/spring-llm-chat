TRUNCATE TABLE chat RESTART IDENTITY CASCADE;

INSERT INTO chat (id, title, created_at) VALUES (1, 'test_chat', now());