CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'WRITE', 'READ');

CREATE TABLE usuario (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    role          rol_usuario NOT NULL,
    activo        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_email    UNIQUE (email)
);

-- Seed: admin / admin123 con BCrypt factor 10
INSERT INTO usuario (username, password_hash, email, role)
VALUES (
    'admin',
    '$2b$10$ySC.bb/zB2Jm6765sq1Y6O3xXKPeBJ4tbOMIMD/fO9vgjuVfaFBxe',
    'admin@franquicias.com',
    'ADMIN'
);
