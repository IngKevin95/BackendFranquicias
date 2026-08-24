CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE franquicia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL
);

CREATE TABLE sucursal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    franquicia_id UUID NOT NULL REFERENCES franquicia(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL
);

CREATE TABLE producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sucursal_id UUID NOT NULL REFERENCES sucursal(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0)
);

CREATE INDEX idx_sucursal_franquicia_id ON sucursal(franquicia_id);
CREATE INDEX idx_producto_sucursal_id ON producto(sucursal_id);
