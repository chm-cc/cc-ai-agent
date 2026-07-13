-- pgvector 扩展（brew install pgvector + 源码编译）
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS agents (
    id            VARCHAR(64)   PRIMARY KEY,
    name          VARCHAR(100)  NOT NULL,
    description   VARCHAR(500),
    icon          VARCHAR(10)   DEFAULT '🤖',
    category      VARCHAR(32)   DEFAULT 'general',
    tags          TEXT,
    system_prompt TEXT,
    model         VARCHAR(64)   DEFAULT '',
    temperature   DOUBLE PRECISION DEFAULT 0.7,
    max_tokens    INT           DEFAULT 2000,
    tools         TEXT,
    status        VARCHAR(16)   DEFAULT 'ACTIVE',
    sort_order    INT           DEFAULT 0,
    created_at    TIMESTAMP     DEFAULT now(),
    updated_at    TIMESTAMP     DEFAULT now()
);

CREATE TABLE IF NOT EXISTS conversations (
    id           VARCHAR(36)  PRIMARY KEY,
    agent_id     VARCHAR(64)  NOT NULL,
    user_id      VARCHAR(64)  NOT NULL DEFAULT 'admin',
    title        VARCHAR(200),
    last_message VARCHAR(500),
    message_count INT         DEFAULT 0,
    created_at   TIMESTAMP    DEFAULT now(),
    updated_at   TIMESTAMP    DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_conv_user_agent ON conversations(user_id, agent_id);
CREATE INDEX IF NOT EXISTS idx_conv_updated ON conversations(user_id, updated_at DESC);

CREATE TABLE IF NOT EXISTS messages (
    id               BIGSERIAL    PRIMARY KEY,
    conversation_id  VARCHAR(36)  NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    role             VARCHAR(16)  NOT NULL,
    content          TEXT         NOT NULL,
    feedback         VARCHAR(16),
    created_at       TIMESTAMP    DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_msg_conv ON messages(conversation_id, created_at);

-- 兼容已有数据库，补充 feedback 列
ALTER TABLE messages ADD COLUMN IF NOT EXISTS feedback VARCHAR(16);

CREATE TABLE IF NOT EXISTS users (
    id          VARCHAR(64)   PRIMARY KEY,
    username    VARCHAR(64)   NOT NULL UNIQUE,
    password    VARCHAR(256)  NOT NULL,
    role        VARCHAR(16)   DEFAULT 'USER',
    enabled     BOOLEAN       DEFAULT true,
    created_at  TIMESTAMP     DEFAULT now(),
    updated_at  TIMESTAMP     DEFAULT now()
);
