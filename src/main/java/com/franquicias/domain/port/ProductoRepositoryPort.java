package com.franquicias.domain.port;

import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductoRepositoryPort {
    Mono<Producto> save(Producto producto);
    Mono<Producto> findById(UUID id);
    Mono<Void> deleteById(UUID id);
    Flux<ProductoMaxStock> findMaxStockPorFranquicia(UUID franquiciaId, int limit, int offset);
    Mono<Long> updateStockNativo(UUID id, int cantidadCambio);
    Mono<Void> updateNombre(UUID id, String nombre);
    Mono<Void> registrarTransaccionStock(UUID productoId, String tipo, int cantidad);
}
