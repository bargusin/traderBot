CREATE TABLE IF NOT EXISTS users (
    chat_id BIGINT PRIMARY KEY,
    user_name TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    chat_id BIGINT NOT NULL,
    mode TEXT NOT NULL,
    encrypted_token TEXT NOT NULL,

    -- Внешний ключ связывает настройку с пользователем
    FOREIGN KEY (chat_id) REFERENCES users(chat_id) ON DELETE CASCADE,

    -- Уникальность: у одного юзера не может быть двух настроек для одного режима
    UNIQUE(chat_id, mode)
);