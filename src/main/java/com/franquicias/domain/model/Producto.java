package com.franquicias.domain.model;

import java.util.UUID;

public record Producto(UUID id, UUID sucursalId, String nombre, int stock) {}
