CREATE TABLE IF NOT EXISTS user (
    chat_id INTEGER PRIMARY KEY,
    user_name TEXT,
    encrypted_token TEXT NOT NULL,
    mode TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);