package com.franquicias.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record TransaccionStock(UUID id, UUID productoId, String tipo, int cantidad, LocalDateTime fechaCreacion) {}
