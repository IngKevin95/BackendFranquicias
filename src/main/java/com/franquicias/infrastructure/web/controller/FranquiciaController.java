package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.FranquiciaService;
import com.franquicias.infrastructure.web.dto.FranquiciaResponse;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Franquicias", description = "Gestión de franquicias principales")
@SecurityRequirement(name = "bearerAuth")
public class FranquiciaController {

    private final FranquiciaService service;

    public FranquiciaController(FranquiciaService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear Franquicia", description = "Crea una nueva franquicia con el nombre proporcionado.")
    public Mono<FranquiciaResponse> crear(@Valid @RequestBody NombreRequest request) {
        return service.crear(request.nombre()).map(FranquiciaResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{id}")
    @Operation(summary = "Renombrar Franquicia", description = "Actualiza el nombre de una franquicia existente.")
    public Mono<FranquiciaResponse> renombrar(@PathVariable UUID id, @Valid @RequestBody NombreRequest request) {
        return service.renombrar(id, request.nombre()).map(FranquiciaResponse::from);
    }
}
