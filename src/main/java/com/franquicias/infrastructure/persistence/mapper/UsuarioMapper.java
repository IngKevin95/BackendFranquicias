package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import com.franquicias.infrastructure.persistence.entity.UsuarioEntity;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    public Usuario toDomain(UsuarioEntity entity) {
        return new Usuario(
            entity.id,
            entity.username,
            entity.passwordHash,
            entity.email,
            RolUsuario.valueOf(entity.role),
            entity.activo,
            entity.createdAt
        );
    }

    public UsuarioEntity toEntity(Usuario usuario) {
        UsuarioEntity entity = new UsuarioEntity();
        entity.id = usuario.id();
        entity.username = usuario.username();
        entity.passwordHash = usuario.passwordHash();
        entity.email = usuario.email();
        entity.role = usuario.role().name();
        entity.activo = usuario.activo();
        entity.createdAt = usuario.createdAt();
        return entity;
    }
}
