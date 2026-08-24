package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String username,
    String email,
    RolUsuario role,
    boolean activo,
    OffsetDateTime createdAt
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.id(), u.username(), u.email(), u.role(), u.activo(), u.createdAt());
    }
}
