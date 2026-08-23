package com.franquicias.domain.model;

import java.util.UUID;

public record Sucursal(UUID id, UUID franquiciaId, String nombre) {}
