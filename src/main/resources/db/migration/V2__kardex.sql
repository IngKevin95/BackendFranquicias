CREATE TABLE transaccion_stock (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    producto_id UUID NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    cantidad INT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index para buscar el historial de un producto mas rápido si fuese necesario
CREATE INDEX idx_transaccion_stock_producto_id ON transaccion_stock(producto_id);
