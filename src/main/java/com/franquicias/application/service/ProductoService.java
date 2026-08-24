package com.franquicias.application.service;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.ProductoNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.ProductoRepositoryPort;
import com.franquicias.domain.port.SucursalRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import java.time.Duration;

@Service
public class ProductoService {

    private final SucursalRepositoryPort sucursalPort;
    private final ProductoRepositoryPort productoPort;
    private final FranquiciaRepositoryPort franquiciaPort;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    public ProductoService(SucursalRepositoryPort sucursalPort, ProductoRepositoryPort productoPort,
                            FranquiciaRepositoryPort franquiciaPort, ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.sucursalPort = sucursalPort;
        this.productoPort = productoPort;
        this.franquiciaPort = franquiciaPort;
        this.redisTemplate = redisTemplate;
    }

    public Mono<Producto> agregar(UUID franquiciaId, UUID sucursalId, String nombre) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .flatMap(sucursal -> productoPort.save(new Producto(null, sucursalId, nombre, 0)))
            .flatMap(producto -> invalidarCacheMaxStock(franquiciaId).thenReturn(producto));
    }

    public Mono<Void> eliminar(UUID franquiciaId, UUID sucursalId, UUID productoId) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .then(productoDeSucursal(sucursalId, productoId))
            .flatMap(producto -> productoPort.deleteById(producto.id()))
            .then(invalidarCacheMaxStock(franquiciaId));
    }

    @Transactional
    public Mono<Producto> modificarStock(UUID franquiciaId, UUID sucursalId, UUID productoId, String tipo, int cantidad) {
        int cantidadCambio = "ENTRADA".equals(tipo) ? cantidad : -cantidad;
        
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .then(productoDeSucursal(sucursalId, productoId))
            .flatMap(producto -> 
                productoPort.updateStockNativo(productoId, cantidadCambio)
                    .flatMap(filasActualizadas -> {
                        if (filasActualizadas == 0) {
                            return Mono.error(new IllegalArgumentException("Stock insuficiente para realizar la salida"));
                        }
                        return productoPort.registrarTransaccionStock(productoId, tipo, cantidad);
                    })
                    .then(invalidarCacheMaxStock(franquiciaId))
                    .then(productoPort.findById(productoId))
            );
    }

    public Mono<Producto> renombrar(UUID franquiciaId, UUID sucursalId, UUID productoId, String nuevoNombre) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .then(productoDeSucursal(sucursalId, productoId))
            .flatMap(producto -> productoPort.updateNombre(productoId, nuevoNombre)
                .then(invalidarCacheMaxStock(franquiciaId))
                .then(productoPort.findById(productoId)));
    }

    public Flux<ProductoMaxStock> obtenerMaxStockPorFranquicia(UUID franquiciaId, int limit, int offset) {
        String cacheKey = "max-stock:franquicia:" + franquiciaId + ":l:" + limit + ":o:" + offset;
        
        return redisTemplate.opsForList().range(cacheKey, 0, -1)
            .cast(ProductoMaxStock.class)
            .switchIfEmpty(Flux.defer(() -> 
                franquiciaPort.findById(franquiciaId)
                    .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(franquiciaId)))
                    .flatMapMany(franquicia -> productoPort.findMaxStockPorFranquicia(franquiciaId, limit, offset))
                    .collectList()
                    .flatMapMany(lista -> {
                        if (!lista.isEmpty()) {
                            return redisTemplate.opsForList().rightPushAll(cacheKey, lista.toArray())
                                .then(redisTemplate.expire(cacheKey, Duration.ofMinutes(5)))
                                .thenMany(Flux.fromIterable(lista));
                        }
                        return Flux.empty();
                    })
            ));
    }
    
    private Mono<Void> invalidarCacheMaxStock(UUID franquiciaId) {
        return Mono.defer(() -> 
            redisTemplate.keys("max-stock:franquicia:" + franquiciaId + "*")
                .flatMap(redisTemplate::delete)
                .then()
        );
    }

    private Mono<Sucursal> sucursalDeFranquicia(UUID franquiciaId, UUID sucursalId) {
        return sucursalPort.findById(sucursalId)
            .switchIfEmpty(Mono.error(new SucursalNotFoundException(sucursalId)))
            .flatMap(sucursal -> {
                if (!sucursal.franquiciaId().equals(franquiciaId)) {
                    return Mono.error(new com.franquicias.domain.exception.ConflictoRelacionException("La sucursal no pertenece a esta franquicia"));
                }
                return Mono.just(sucursal);
            });
    }

    private Mono<Producto> productoDeSucursal(UUID sucursalId, UUID productoId) {
        return productoPort.findById(productoId)
            .switchIfEmpty(Mono.error(new ProductoNotFoundException(productoId)))
            .flatMap(producto -> {
                if (!producto.sucursalId().equals(sucursalId)) {
                    return Mono.error(new com.franquicias.domain.exception.ConflictoRelacionException("El producto no pertenece a esta sucursal"));
                }
                return Mono.just(producto);
            });
    }
}
