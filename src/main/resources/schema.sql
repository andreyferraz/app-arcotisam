CREATE TABLE IF NOT EXISTS usuarios (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL
);

-- Índice único para garantir unicidade do username
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_username ON usuarios(username);