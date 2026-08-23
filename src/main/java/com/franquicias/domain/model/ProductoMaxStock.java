package com.franquicias.domain.model;

import java.util.UUID;

public record ProductoMaxStock(UUID sucursalId, String sucursalNombre, Producto producto) {}
