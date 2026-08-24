ALTER TABLE transaccion_stock
ADD COLUMN idempotency_key UUID;

CREATE UNIQUE INDEX idx_transaccion_stock_idempotency 
ON transaccion_stock(idempotency_key) 
WHERE idempotency_key IS NOT NULL;
