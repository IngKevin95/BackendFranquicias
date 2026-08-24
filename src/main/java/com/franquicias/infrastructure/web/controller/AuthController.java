package com.franquicias.infrastructure.web.controller;

import com.franquicias.infrastructure.config.security.JwtUtil;
import com.franquicias.infrastructure.web.dto.auth.AuthRequest;
import com.franquicias.infrastructure.web.dto.auth.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Autenticación", description = "Endpoints para la gestión de tokens JWT")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "Generar Token JWT", description = "Autentica al usuario (admin/admin123) y retorna un token JWT válido para consumir los demás endpoints.")
    public Mono<AuthResponse> login(@RequestBody AuthRequest request) {
        // Mock authentication para propósitos de la prueba técnica
        if ("admin".equals(request.username()) && "admin123".equals(request.password())) {
            return Mono.just(new AuthResponse(jwtUtil.generateToken(request.username(), "ADMIN")));
        }
        return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
    }
}
