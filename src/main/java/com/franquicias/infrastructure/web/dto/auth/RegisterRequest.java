package com.franquicias.infrastructure.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.franquicias.domain.model.RolUsuario;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank String password,
    @Email @NotBlank String email,
    @NotNull RolUsuario role
) {}
