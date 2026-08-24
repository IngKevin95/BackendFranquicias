package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.model.TransaccionStock;
import com.franquicias.domain.port.ProductoRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.ProductoMapper;
import com.franquicias.infrastructure.persistence.repository.ProductoR2dbcRepository;
import java.time.LocalDateTime;
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
        LIMIT :limit OFFSET :offset
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
    public Flux<ProductoMaxStock> findMaxStockPorFranquicia(UUID franquiciaId, int limit, int offset) {
        return databaseClient.sql(MAX_STOCK_QUERY)
            .bind("franquiciaId", franquiciaId)
            .bind("limit", limit)
            .bind("offset", offset)
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

    @Override
    public Mono<Long> updateStockNativo(UUID id, int cantidadCambio) {
        return databaseClient.sql("UPDATE producto SET stock = stock + :cambio WHERE id = :id AND (stock + :cambio) >= 0")
            .bind("cambio", cantidadCambio)
            .bind("id", id)
            .fetch()
            .rowsUpdated();
    }

    @Override
    public Mono<Void> updateNombre(UUID id, String nombre) {
        return databaseClient.sql("UPDATE producto SET nombre = :nombre WHERE id = :id")
            .bind("nombre", nombre)
            .bind("id", id)
            .fetch()
            .rowsUpdated()
            .then();
    }

    @Override
    public Mono<Void> registrarTransaccionStock(UUID productoId, String tipo, int cantidad, String idempotencyKey) {
        DatabaseClient.GenericExecuteSpec sql = databaseClient.sql(
            "INSERT INTO transaccion_stock (producto_id, tipo, cantidad, idempotency_key) VALUES (:productoId, :tipo, :cantidad, :idempotencyKey)"
        )
        .bind("productoId", productoId)
        .bind("tipo", tipo)
        .bind("cantidad", cantidad);
        
        if (idempotencyKey != null) {
            sql = sql.bind("idempotencyKey", UUID.fromString(idempotencyKey));
        } else {
            sql = sql.bindNull("idempotencyKey", UUID.class);
        }

        return sql.then();
    }

    @Override
    public Flux<Producto> findBySucursalId(UUID sucursalId) {
        return repository.findBySucursalId(sucursalId).map(mapper::toDomain);
    }

    @Override
    public Flux<TransaccionStock> findKardex(UUID productoId, LocalDateTime desde, LocalDateTime hasta) {
        String query = """
            SELECT id, producto_id, tipo, cantidad, fecha_creacion
            FROM transaccion_stock
            WHERE producto_id = :productoId
              AND (:desde::timestamp IS NULL OR fecha_creacion >= :desde)
              AND (:hasta::timestamp IS NULL OR fecha_creacion <= :hasta)
            ORDER BY fecha_creacion DESC
            """;
        DatabaseClient.GenericExecuteSpec spec = databaseClient.sql(query)
            .bind("productoId", productoId);
        spec = (desde != null) ? spec.bind("desde", desde) : spec.bindNull("desde", LocalDateTime.class);
        spec = (hasta != null) ? spec.bind("hasta", hasta) : spec.bindNull("hasta", LocalDateTime.class);

        return spec
            .map((row, metadata) -> new TransaccionStock(
                row.get("id", UUID.class),
                row.get("producto_id", UUID.class),
                row.get("tipo", String.class),
                row.get("cantidad", Integer.class),
                row.get("fecha_creacion", LocalDateTime.class)))
            .all();
    }
}
