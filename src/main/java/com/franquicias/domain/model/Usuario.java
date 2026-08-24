package com.franquicias.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Usuario(
    UUID id,
    String username,
    String passwordHash,
    String email,
    RolUsuario role,
    boolean activo,
    OffsetDateTime createdAt
) {}
