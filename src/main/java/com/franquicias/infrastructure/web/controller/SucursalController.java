package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.SucursalService;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.SucursalResponse;
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
@Tag(name = "Sucursales", description = "Gestión de sucursales por franquicia")
@SecurityRequirement(name = "bearerAuth")
public class SucursalController {

    private final SucursalService service;

    public SucursalController(SucursalService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias/{franquiciaId}/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar Sucursal", description = "Agrega una nueva sucursal a una franquicia específica.")
    public Mono<SucursalResponse> agregar(@PathVariable UUID franquiciaId, @Valid @RequestBody NombreRequest request) {
        return service.agregar(franquiciaId, request.nombre()).map(SucursalResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}")
    @Operation(summary = "Renombrar Sucursal", description = "Cambia el nombre de una sucursal validando su relación con la franquicia.")
    public Mono<SucursalResponse> renombrar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                             @Valid @RequestBody NombreRequest request) {
        return service.renombrar(franquiciaId, sucursalId, request.nombre()).map(SucursalResponse::from);
    }
}
