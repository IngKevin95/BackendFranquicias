package com.franquicias.infrastructure.persistence.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("usuario")
public class UsuarioEntity {

    @Id
    public UUID id;

    @Column("username")
    public String username;

    @Column("password_hash")
    public String passwordHash;

    @Column("email")
    public String email;

    @Column("role")
    public String role;

    @Column("activo")
    public boolean activo;

    @Column("created_at")
    public OffsetDateTime createdAt;
}
