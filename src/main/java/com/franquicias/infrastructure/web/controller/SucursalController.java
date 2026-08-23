package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.SucursalService;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.SucursalResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SucursalController {

    private final SucursalService service;

    public SucursalController(SucursalService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias/{franquiciaId}/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SucursalResponse> agregar(@PathVariable UUID franquiciaId, @Valid @RequestBody NombreRequest request) {
        return service.agregar(franquiciaId, request.nombre()).map(SucursalResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}")
    public Mono<SucursalResponse> renombrar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                             @Valid @RequestBody NombreRequest request) {
        return service.renombrar(franquiciaId, sucursalId, request.nombre()).map(SucursalResponse::from);
    }
}
