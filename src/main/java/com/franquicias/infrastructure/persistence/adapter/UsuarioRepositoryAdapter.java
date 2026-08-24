package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Usuario;
import com.franquicias.domain.port.UsuarioRepositoryPort;
import com.franquicias.infrastructure.persistence.entity.UsuarioEntity;
import com.franquicias.infrastructure.persistence.mapper.UsuarioMapper;
import com.franquicias.infrastructure.persistence.repository.UsuarioR2dbcRepository;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioR2dbcRepository repository;
    private final UsuarioMapper mapper;
    private final DatabaseClient databaseClient;

    public UsuarioRepositoryAdapter(UsuarioR2dbcRepository repository, UsuarioMapper mapper, DatabaseClient databaseClient) {
        this.repository = repository;
        this.mapper = mapper;
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Usuario> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Mono<Usuario> save(Usuario usuario) {
        // Insercion via DatabaseClient con cast explicito a rol_usuario: el driver R2DBC
        // de PostgreSQL no convierte automaticamente un parametro character varying al
        // tipo ENUM nativo de la columna "role".
        UsuarioEntity entity = mapper.toEntity(usuario);
        return databaseClient.sql("""
                INSERT INTO usuario (username, password_hash, email, role, activo)
                VALUES (:username, :passwordHash, :email, CAST(:role AS rol_usuario), :activo)
                RETURNING id, username, password_hash, email, role, activo, created_at
                """)
            .bind("username", entity.username)
            .bind("passwordHash", entity.passwordHash)
            .bind("email", entity.email)
            .bind("role", entity.role)
            .bind("activo", entity.activo)
            .map(row -> {
                UsuarioEntity saved = new UsuarioEntity();
                saved.id = row.get("id", UUID.class);
                saved.username = row.get("username", String.class);
                saved.passwordHash = row.get("password_hash", String.class);
                saved.email = row.get("email", String.class);
                saved.role = row.get("role", String.class);
                saved.activo = Boolean.TRUE.equals(row.get("activo", Boolean.class));
                saved.createdAt = row.get("created_at", OffsetDateTime.class);
                return saved;
            })
            .one()
            .map(mapper::toDomain);
    }
}
