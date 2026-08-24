package com.franquicias.infrastructure.web.controller;

import com.franquicias.domain.model.Usuario;
import com.franquicias.domain.port.UsuarioRepositoryPort;
import com.franquicias.infrastructure.web.dto.UsuarioResponse;
import com.franquicias.infrastructure.web.dto.auth.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Usuarios", description = "Gestion de usuarios del sistema (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioRepositoryPort usuarioPort;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepositoryPort usuarioPort, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioPort = usuarioPort;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/v1/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear Usuario", description = "Crea un nuevo usuario con el rol especificado. Solo accesible para usuarios con rol ADMIN.")
    public Mono<UsuarioResponse> crear(@Valid @RequestBody RegisterRequest request) {
        String hash = passwordEncoder.encode(request.password());
        Usuario nuevo = new Usuario(null, request.username(), hash, request.email(), request.role(), true, null);
        return usuarioPort.save(nuevo)
            .map(UsuarioResponse::from)
            .onErrorResume(DataIntegrityViolationException.class, e -> {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("uq_usuario_username")) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe"));
                }
                if (msg.contains("uq_usuario_email")) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "El email ya existe"));
                }
                return Mono.error(e);
            });
    }
}
