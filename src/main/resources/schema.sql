CREATE TABLE IF NOT EXISTS usuarios (
    id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    password TEXT NOT NULL,
    role TEXT NOT NULL,
    foto_url TEXT
);

-- Índice único para garantir unicidade do username
CREATE UNIQUE INDEX IF NOT EXISTS ux_usuarios_username ON usuarios(username);

-- Tabela artesaos com referência única ao usuário (one-to-one)
CREATE TABLE IF NOT EXISTS artesaos (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    descricao TEXT,
    whatsapp TEXT,
    foto_url TEXT,
    usuario_id TEXT UNIQUE,
    CONSTRAINT fk_artesaos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

-- Tabela produtos ligada a artesaos (One-to-Many). ON DELETE CASCADE garante remoção ao excluir o artesão.
CREATE TABLE IF NOT EXISTS produtos (
    id TEXT PRIMARY KEY,
    nome TEXT NOT NULL,
    descricao TEXT,
    preco NUMERIC,
    imagem_url TEXT,
    ativo INTEGER DEFAULT 1,
    quantidade_vendida INTEGER DEFAULT 0,
    artesao_id TEXT NOT NULL,
    CONSTRAINT fk_produtos_artesao FOREIGN KEY (artesao_id) REFERENCES artesaos(id) ON DELETE CASCADE
);

-- Tabela de movimentações (entradas/saídas) para controle de caixa dos artesãos
CREATE TABLE IF NOT EXISTS movimentacoes (
    id TEXT PRIMARY KEY,
    artesao_id TEXT NOT NULL,
    tipo TEXT NOT NULL,
    descricao TEXT,
    valor NUMERIC NOT NULL,
    data_hora TEXT NOT NULL,
    CONSTRAINT fk_movimentacoes_artesao FOREIGN KEY (artesao_id) REFERENCES artesaos(id) ON DELETE CASCADE
);