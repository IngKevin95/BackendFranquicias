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

@Service
public class ProductoService {

    private final SucursalRepositoryPort sucursalPort;
    private final ProductoRepositoryPort productoPort;
    private final FranquiciaRepositoryPort franquiciaPort;

    public ProductoService(SucursalRepositoryPort sucursalPort, ProductoRepositoryPort productoPort,
                            FranquiciaRepositoryPort franquiciaPort) {
        this.sucursalPort = sucursalPort;
        this.productoPort = productoPort;
        this.franquiciaPort = franquiciaPort;
    }

    public Mono<Producto> agregar(UUID franquiciaId, UUID sucursalId, String nombre, int stock) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .flatMap(sucursal -> productoPort.save(new Producto(null, sucursalId, nombre, stock)));
    }

    public Mono<Void> eliminar(UUID franquiciaId, UUID sucursalId, UUID productoId) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .then(productoDeSucursal(sucursalId, productoId))
            .flatMap(producto -> productoPort.deleteById(producto.id()));
    }

    public Mono<Producto> modificarStock(UUID franquiciaId, UUID sucursalId, UUID productoId, int nuevoStock) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .then(productoDeSucursal(sucursalId, productoId))
            .flatMap(producto -> productoPort.save(
                new Producto(producto.id(), producto.sucursalId(), producto.nombre(), nuevoStock)));
    }

    public Mono<Producto> renombrar(UUID franquiciaId, UUID sucursalId, UUID productoId, String nuevoNombre) {
        return sucursalDeFranquicia(franquiciaId, sucursalId)
            .then(productoDeSucursal(sucursalId, productoId))
            .flatMap(producto -> productoPort.save(
                new Producto(producto.id(), producto.sucursalId(), nuevoNombre, producto.stock())));
    }

    public Flux<ProductoMaxStock> obtenerMaxStockPorFranquicia(UUID franquiciaId) {
        return franquiciaPort.findById(franquiciaId)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(franquiciaId)))
            .flatMapMany(franquicia -> productoPort.findMaxStockPorFranquicia(franquiciaId));
    }

    private Mono<Sucursal> sucursalDeFranquicia(UUID franquiciaId, UUID sucursalId) {
        return sucursalPort.findById(sucursalId)
            .switchIfEmpty(Mono.error(new SucursalNotFoundException(sucursalId)))
            .filter(sucursal -> sucursal.franquiciaId().equals(franquiciaId))
            .switchIfEmpty(Mono.error(new SucursalNotFoundException(sucursalId)));
    }

    private Mono<Producto> productoDeSucursal(UUID sucursalId, UUID productoId) {
        return productoPort.findById(productoId)
            .switchIfEmpty(Mono.error(new ProductoNotFoundException(productoId)))
            .filter(producto -> producto.sucursalId().equals(sucursalId))
            .switchIfEmpty(Mono.error(new ProductoNotFoundException(productoId)));
    }
}
