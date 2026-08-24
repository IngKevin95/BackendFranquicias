package com.franquicias.infrastructure.web.controller;

import com.franquicias.domain.port.UsuarioRepositoryPort;
import com.franquicias.infrastructure.config.security.JwtUtil;
import com.franquicias.infrastructure.web.dto.auth.AuthRequest;
import com.franquicias.infrastructure.web.dto.auth.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Autenticacion", description = "Endpoints para la gestion de tokens JWT")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UsuarioRepositoryPort usuarioPort;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(JwtUtil jwtUtil, UsuarioRepositoryPort usuarioPort, BCryptPasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.usuarioPort = usuarioPort;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "Generar Token JWT", description = "Autentica con username y password. Retorna un JWT con el rol del usuario incluido en el claim 'role'.")
    public Mono<AuthResponse> login(@RequestBody AuthRequest request) {
        return usuarioPort.findByUsername(request.username())
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas")))
            .flatMap(usuario -> {
                if (!usuario.activo()) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario inactivo"));
                }
                if (!passwordEncoder.matches(request.password(), usuario.passwordHash())) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
                }
                String token = jwtUtil.generateToken(usuario.username(), usuario.role().name());
                return Mono.just(new AuthResponse(token));
            });
    }
}
