package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.TransaccionStock;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransaccionStockResponse(UUID id, UUID productoId, String tipo, int cantidad, LocalDateTime fechaCreacion) {
    public static TransaccionStockResponse from(TransaccionStock t) {
        return new TransaccionStockResponse(t.id(), t.productoId(), t.tipo(), t.cantidad(), t.fechaCreacion());
    }
}
