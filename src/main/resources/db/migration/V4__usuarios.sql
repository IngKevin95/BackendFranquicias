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

-- El usuario admin inicial ya no se siembra aca (hardcodeado en SQL, igual
-- para cualquier entorno). Lo siembra AdminUserSeeder al arrancar la app,
-- leyendo credenciales de variables de entorno (ADMIN_USERNAME/
-- ADMIN_PASSWORD/ADMIN_EMAIL), para que cada despliegue tenga las suyas.
