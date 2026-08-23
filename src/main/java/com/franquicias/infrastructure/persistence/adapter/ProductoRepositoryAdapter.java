package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.port.ProductoRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.ProductoMapper;
import com.franquicias.infrastructure.persistence.repository.ProductoR2dbcRepository;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductoRepositoryAdapter implements ProductoRepositoryPort {

    private static final String MAX_STOCK_QUERY = """
        SELECT DISTINCT ON (p.sucursal_id)
               p.sucursal_id AS sucursal_id,
               s.nombre AS sucursal_nombre,
               p.id AS producto_id,
               p.nombre AS producto_nombre,
               p.stock AS stock
        FROM producto p
        JOIN sucursal s ON s.id = p.sucursal_id
        WHERE s.franquicia_id = :franquiciaId
        ORDER BY p.sucursal_id, p.stock DESC
        """;

    private final ProductoR2dbcRepository repository;
    private final ProductoMapper mapper;
    private final DatabaseClient databaseClient;

    public ProductoRepositoryAdapter(
            ProductoR2dbcRepository repository, ProductoMapper mapper, DatabaseClient databaseClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Producto> save(Producto producto) {
        return repository.save(mapper.toEntity(producto)).map(mapper::toDomain);
    }

    @Override
    public Mono<Producto> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return repository.deleteById(id);
    }

    @Override
    public Flux<ProductoMaxStock> findMaxStockPorFranquicia(UUID franquiciaId) {
        return databaseClient.sql(MAX_STOCK_QUERY)
            .bind("franquiciaId", franquiciaId)
            .map((row, metadata) -> new ProductoMaxStock(
                row.get("sucursal_id", UUID.class),
                row.get("sucursal_nombre", String.class),
                new Producto(
                    row.get("producto_id", UUID.class),
                    row.get("sucursal_id", UUID.class),
                    row.get("producto_nombre", String.class),
                    row.get("stock", Integer.class))))
            .all();
    }
}
