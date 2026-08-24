package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.ProductoService;
import com.franquicias.infrastructure.web.dto.CrearProductoRequest;
import com.franquicias.infrastructure.web.dto.ModificarStockRequest;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse;
import com.franquicias.infrastructure.web.dto.ProductoResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductoResponse> agregar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                           @Valid @RequestBody CrearProductoRequest request) {
        return service.agregar(franquiciaId, sucursalId, request.nombre())
            .map(ProductoResponse::from);
    }

    @DeleteMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                @PathVariable UUID productoId) {
        return service.eliminar(franquiciaId, sucursalId, productoId);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/stock")
    public Mono<ProductoResponse> modificarStock(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                                  @PathVariable UUID productoId,
                                                  @Valid @RequestBody ModificarStockRequest request) {
        return service.modificarStock(franquiciaId, sucursalId, productoId, request.tipo(), request.cantidad())
            .map(ProductoResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    public Mono<ProductoResponse> renombrar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                             @PathVariable UUID productoId,
                                             @Valid @RequestBody NombreRequest request) {
        return service.renombrar(franquiciaId, sucursalId, productoId, request.nombre())
            .map(ProductoResponse::from);
    }

    @GetMapping("/api/v1/franquicias/{franquiciaId}/productos/max-stock")
    public Flux<ProductoMaxStockResponse> maxStockPorSucursal(
            @PathVariable UUID franquiciaId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int offset) {
        return service.obtenerMaxStockPorFranquicia(franquiciaId, limit, offset)
            .map(ProductoMaxStockResponse::from);
    }
}
