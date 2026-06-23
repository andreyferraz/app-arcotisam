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

-- Galerias de fotos exibidas na aba Sobre
CREATE TABLE IF NOT EXISTS galerias (
    id TEXT PRIMARY KEY,
    titulo TEXT NOT NULL
);

-- Configurações globais do site
CREATE TABLE IF NOT EXISTS configuracoes_site (
    chave TEXT PRIMARY KEY,
    valor TEXT
);

-- Postagens exibidas na aba Blog
CREATE TABLE IF NOT EXISTS blog_posts (
    id TEXT PRIMARY KEY,
    titulo TEXT NOT NULL,
    data_publicacao TEXT NOT NULL,
    foto_url TEXT NOT NULL,
    conteudo_html TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS blog_post_fotos (
    id TEXT PRIMARY KEY,
    blog_post_id TEXT NOT NULL,
    arquivo_url TEXT NOT NULL,
    capa INTEGER NOT NULL DEFAULT 0,
    ordem INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_blog_post_fotos_post FOREIGN KEY (blog_post_id) REFERENCES blog_posts(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_blog_post_fotos_blog_post_id ON blog_post_fotos(blog_post_id);

CREATE TABLE IF NOT EXISTS galeria_fotos (
    id TEXT PRIMARY KEY,
    galeria_id TEXT NOT NULL,
    arquivo_url TEXT NOT NULL,
    ordem INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_galeria_fotos_galeria FOREIGN KEY (galeria_id) REFERENCES galerias(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS ix_galeria_fotos_galeria_id ON galeria_fotos(galeria_id);

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