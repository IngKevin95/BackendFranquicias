package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.ProductoService;
import com.franquicias.infrastructure.web.dto.CrearProductoRequest;
import com.franquicias.infrastructure.web.dto.ModificarStockRequest;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse;
import com.franquicias.infrastructure.web.dto.ProductoResponse;
import com.franquicias.infrastructure.web.dto.TransaccionStockResponse;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
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

import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@Tag(name = "Productos y Kardex", description = "Gestión de productos, inventario y consultas de stock")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Agregar Producto", description = "Registra un producto en una sucursal con stock inicial de 0.")
    public Mono<ProductoResponse> agregar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                           @Valid @RequestBody CrearProductoRequest request) {
        return service.agregar(franquiciaId, sucursalId, request.nombre())
            .map(ProductoResponse::from);
    }

    @DeleteMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar Producto", description = "Elimina un producto y todo su historial de stock (Cascada).")
    public Mono<Void> eliminar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                @PathVariable UUID productoId) {
        return service.eliminar(franquiciaId, sucursalId, productoId);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/stock")
    @Operation(summary = "Transacción de Stock (Kardex)", description = "Registra una entrada o salida atómica de inventario. Opcionalmente acepta Idempotency-Key.")
    public Mono<ProductoResponse> modificarStock(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                                  @PathVariable UUID productoId,
                                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
                                                  @Valid @RequestBody ModificarStockRequest request) {
        return service.modificarStock(franquiciaId, sucursalId, productoId, request.tipo(), request.cantidad(), idempotencyKey)
            .map(ProductoResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    @Operation(summary = "Renombrar Producto", description = "Cambia el nombre de un producto validando su relación jerárquica.")
    public Mono<ProductoResponse> renombrar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                             @PathVariable UUID productoId,
                                             @Valid @RequestBody NombreRequest request) {
        return service.renombrar(franquiciaId, sucursalId, productoId, request.nombre())
            .map(ProductoResponse::from);
    }

    @GetMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos")
    @Operation(summary = "Listar Productos", description = "Lista los productos de una sucursal, con su stock actual.")
    public Flux<ProductoResponse> listar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId) {
        return service.listarPorSucursal(franquiciaId, sucursalId)
            .map(ProductoResponse::from);
    }

    @GetMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/kardex")
    @Operation(summary = "Kardex de Producto", description = "Historial de transacciones de stock de un producto, filtrable por rango de fechas (desde/hasta, ISO-8601).")
    public Flux<TransaccionStockResponse> kardex(
            @PathVariable UUID franquiciaId, @PathVariable UUID sucursalId, @PathVariable UUID productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return service.obtenerKardex(franquiciaId, sucursalId, productoId, desde, hasta)
            .map(TransaccionStockResponse::from);
    }

    @GetMapping("/api/v1/franquicias/{franquiciaId}/productos/max-stock")
    @Operation(summary = "Obtener Max Stock", description = "Retorna el producto con más stock de cada sucursal de una franquicia. Resultados cacheados en Redis.")
    public Flux<ProductoMaxStockResponse> maxStockPorSucursal(
            @PathVariable UUID franquiciaId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int limit,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int offset) {
        return service.obtenerMaxStockPorFranquicia(franquiciaId, limit, offset)
            .map(ProductoMaxStockResponse::from);
    }
}
