# Usuarios, Roles y Autenticacion Real con JWT ÔÇö Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reemplazar la autenticacion mock hardcoded por una real con tabla `usuario` en PostgreSQL, BCrypt, roles (`ADMIN`, `WRITE`, `READ`) en el claim JWT, y control de acceso por endpoint en `SecurityConfig`.

**Architecture:** Arquitectura hexagonal ÔÇö dominio define modelos y puertos, infraestructura implementa persistencia R2DBC y seguridad Spring WebFlux. Los roles viajan en el JWT como claim `role` y se extraen en el `WebFilter` existente para construir la `GrantedAuthority` de Spring Security. Las reglas de acceso se centralizan en `SecurityConfig` via `pathMatchers` por metodo HTTP, sin tocar los services.

**Tech Stack:** Spring Boot WebFlux, Spring Security WebFlux, R2DBC + PostgreSQL, Flyway, JJWT 0.12.3, BCrypt (`spring-security-crypto` ya incluido en `spring-boot-starter-security`), Testcontainers, JUnit 5.

**Spec:** `docs/superpowers/specs/2026-08-24-usuarios-roles-jwt.md`

## Global Constraints

- Java 17+, Spring Boot 3.x
- R2DBC para todas las consultas reactivas a BD (no JDBC salvo Flyway)
- BCrypt factor de costo 10 para todos los passwords
- El claim del JWT para el rol se llama exactamente `role` (minusculas)
- `GrantedAuthority` se construye como `"ROLE_" + role` (ej: `"ROLE_ADMIN"`)
- Flyway migracion `V4__usuarios.sql` ÔÇö no modificar V1, V2, V3
- Todos los tests de integracion extienden `AbstractIntegrationTest` (Testcontainers)
- Branch de trabajo: crear `feature/usuarios-roles` desde `develop`

---

### Task 1: Branch y migracion Flyway V4

**Files:**
- Create: `src/main/resources/db/migration/V4__usuarios.sql`

**Interfaces:**
- Produces: tabla `usuario` con columnas `id UUID`, `username VARCHAR(100)`, `password_hash VARCHAR(255)`, `email VARCHAR(255)`, `role rol_usuario`, `activo BOOLEAN`, `created_at TIMESTAMPTZ`; tipo enum `rol_usuario` con valores `ADMIN`, `WRITE`, `READ`

- [ ] **Step 1: Crear branch**

```bash
git checkout develop
git pull origin develop
git checkout -b feature/usuarios-roles
```

- [ ] **Step 2: Crear el archivo de migracion**

Contenido exacto de `src/main/resources/db/migration/V4__usuarios.sql`:

```sql
CREATE TYPE rol_usuario AS ENUM ('ADMIN', 'WRITE', 'READ');

CREATE TABLE usuario (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    role          rol_usuario NOT NULL,
    activo        BOOLEAN NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_usuario_username UNIQUE (username),
    CONSTRAINT uq_usuario_email    UNIQUE (email)
);

-- Seed: admin / admin123 con BCrypt factor 10
INSERT INTO usuario (username, password_hash, email, role)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'admin@franquicias.com',
    'ADMIN'
);
```

- [ ] **Step 3: Verificar que Flyway aplica la migracion sin errores**

```bash
docker compose up -d postgres
mvn flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5433/franquicias -Dflyway.user=franquicias -Dflyway.password=franquicias
```

Resultado esperado: `Successfully applied 1 migration to schema "public"` (V4).

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V4__usuarios.sql
git commit -m "feat(usuarios): add V4 Flyway migration for usuario table and seed admin"
```

---

### Task 2: Dominio ÔÇö modelo y puerto

**Files:**
- Create: `src/main/java/com/franquicias/domain/model/RolUsuario.java`
- Create: `src/main/java/com/franquicias/domain/model/Usuario.java`
- Create: `src/main/java/com/franquicias/domain/port/UsuarioRepositoryPort.java`

**Interfaces:**
- Produces:
  - `RolUsuario`: enum con valores `ADMIN`, `WRITE`, `READ`
  - `Usuario`: record `(UUID id, String username, String passwordHash, String email, RolUsuario role, boolean activo, java.time.OffsetDateTime createdAt)`
  - `UsuarioRepositoryPort`: interface con `Mono<Usuario> findByUsername(String username)` y `Mono<Usuario> save(Usuario usuario)`

- [ ] **Step 1: Crear `RolUsuario.java`**

```java
package com.franquicias.domain.model;

public enum RolUsuario {
    ADMIN, WRITE, READ
}
```

- [ ] **Step 2: Crear `Usuario.java`**

```java
package com.franquicias.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Usuario(
    UUID id,
    String username,
    String passwordHash,
    String email,
    RolUsuario role,
    boolean activo,
    OffsetDateTime createdAt
) {}
```

- [ ] **Step 3: Crear `UsuarioRepositoryPort.java`**

```java
package com.franquicias.domain.port;

import com.franquicias.domain.model.Usuario;
import reactor.core.publisher.Mono;

public interface UsuarioRepositoryPort {
    Mono<Usuario> findByUsername(String username);
    Mono<Usuario> save(Usuario usuario);
}
```

- [ ] **Step 4: Compilar para verificar**

```bash
mvn compile -q
```

Resultado esperado: BUILD SUCCESS sin errores.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/domain/model/RolUsuario.java
git add src/main/java/com/franquicias/domain/model/Usuario.java
git add src/main/java/com/franquicias/domain/port/UsuarioRepositoryPort.java
git commit -m "feat(usuarios): add Usuario domain model, RolUsuario enum and repository port"
```

---

### Task 3: Infraestructura ÔÇö persistencia R2DBC

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/persistence/entity/UsuarioEntity.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/mapper/UsuarioMapper.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/repository/UsuarioR2dbcRepository.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/adapter/UsuarioRepositoryAdapter.java`
- Create: `src/test/java/com/franquicias/infrastructure/persistence/adapter/UsuarioRepositoryAdapterTest.java`

**Interfaces:**
- Consumes: `Usuario`, `RolUsuario`, `UsuarioRepositoryPort` (Task 2)
- Produces: `UsuarioRepositoryAdapter` que implementa `UsuarioRepositoryPort`; `UsuarioMapper` con metodos `toEntity(Usuario): UsuarioEntity` y `toDomain(UsuarioEntity): Usuario`

- [ ] **Step 1: Escribir el test de integracion primero**

Contenido de `src/test/java/com/franquicias/infrastructure/persistence/adapter/UsuarioRepositoryAdapterTest.java`:

```java
package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.test.StepVerifier;

class UsuarioRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private UsuarioRepositoryAdapter adapter;

    @Test
    void findByUsername_adminSeedExists() {
        StepVerifier.create(adapter.findByUsername("admin"))
            .assertNext(u -> {
                assert u.username().equals("admin");
                assert u.role() == RolUsuario.ADMIN;
                assert u.activo();
            })
            .verifyComplete();
    }

    @Test
    void save_and_findByUsername() {
        Usuario nuevo = new Usuario(null, "testuser", "$2a$10$hash", "test@test.com", RolUsuario.READ, true, null);
        StepVerifier.create(adapter.save(nuevo).flatMap(saved -> adapter.findByUsername("testuser")))
            .assertNext(u -> {
                assert u.username().equals("testuser");
                assert u.role() == RolUsuario.READ;
            })
            .verifyComplete();
    }
}
```

- [ ] **Step 2: Ejecutar el test para verificar que falla**

```bash
mvn test -pl . -Dtest=UsuarioRepositoryAdapterTest -q
```

Resultado esperado: FAIL ÔÇö `UsuarioRepositoryAdapter` no existe aun.

- [ ] **Step 3: Crear `UsuarioEntity.java`**

```java
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
```

Nota: `role` se mapea como `String` porque R2DBC no convierte enums PostgreSQL automaticamente. La conversion se hace en el mapper.

- [ ] **Step 4: Crear `UsuarioMapper.java`**

```java
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
```

- [ ] **Step 5: Crear `UsuarioR2dbcRepository.java`**

```java
package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.UsuarioEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UsuarioR2dbcRepository extends ReactiveCrudRepository<UsuarioEntity, UUID> {
    Mono<UsuarioEntity> findByUsername(String username);
}
```

- [ ] **Step 6: Crear `UsuarioRepositoryAdapter.java`**

```java
package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Usuario;
import com.franquicias.domain.port.UsuarioRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.UsuarioMapper;
import com.franquicias.infrastructure.persistence.repository.UsuarioR2dbcRepository;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class UsuarioRepositoryAdapter implements UsuarioRepositoryPort {

    private final UsuarioR2dbcRepository repository;
    private final UsuarioMapper mapper;

    public UsuarioRepositoryAdapter(UsuarioR2dbcRepository repository, UsuarioMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Usuario> findByUsername(String username) {
        return repository.findByUsername(username).map(mapper::toDomain);
    }

    @Override
    public Mono<Usuario> save(Usuario usuario) {
        return repository.save(mapper.toEntity(usuario)).map(mapper::toDomain);
    }
}
```

- [ ] **Step 7: Ejecutar el test para verificar que pasa**

```bash
mvn test -pl . -Dtest=UsuarioRepositoryAdapterTest -q
```

Resultado esperado: BUILD SUCCESS, 2 tests passed.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/persistence/entity/UsuarioEntity.java
git add src/main/java/com/franquicias/infrastructure/persistence/mapper/UsuarioMapper.java
git add src/main/java/com/franquicias/infrastructure/persistence/repository/UsuarioR2dbcRepository.java
git add src/main/java/com/franquicias/infrastructure/persistence/adapter/UsuarioRepositoryAdapter.java
git add src/test/java/com/franquicias/infrastructure/persistence/adapter/UsuarioRepositoryAdapterTest.java
git commit -m "feat(usuarios): add R2DBC persistence layer for usuario (entity, mapper, repo, adapter)"
```

---

### Task 4: BCryptPasswordEncoder bean y JwtUtil actualizado

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/config/security/PasswordEncoderConfig.java`
- Modify: `src/main/java/com/franquicias/infrastructure/config/security/JwtUtil.java`

**Interfaces:**
- Produces:
  - Bean `BCryptPasswordEncoder` disponible para inyeccion con tipo `org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder`
  - `JwtUtil.generateToken(String username, String role): String`
  - `JwtUtil.extractRole(String token): String`
  - `JwtUtil.validateToken(String token): boolean` ÔÇö sin cambios en firma
  - `JwtUtil.extractUsername(String token): String` ÔÇö sin cambios en firma

- [ ] **Step 1: Crear `PasswordEncoderConfig.java`**

```java
package com.franquicias.infrastructure.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

- [ ] **Step 2: Modificar `JwtUtil.java` para incluir claim `role`**

Reemplazar el contenido completo de `src/main/java/com/franquicias/infrastructure/config/security/JwtUtil.java`:

```java
package com.franquicias.infrastructure.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final String secret = "franquicias-super-secret-key-1234567890";
    private final SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

    public String generateToken(String username, String role) {
        return Jwts.builder()
            .subject(username)
            .claim("role", role)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
```

- [ ] **Step 3: Compilar para verificar**

```bash
mvn compile -q
```

Resultado esperado: BUILD SUCCESS. Nota: los tests que llaman a `generateToken(username)` con un solo argumento fallaran en compilacion ÔÇö se corrigen en Task 6.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/config/security/PasswordEncoderConfig.java
git add src/main/java/com/franquicias/infrastructure/config/security/JwtUtil.java
git commit -m "feat(usuarios): add BCryptPasswordEncoder bean and add role claim to JwtUtil"
```

---

### Task 5: SecurityConfig ÔÇö WebFilter con rol y reglas de acceso

**Files:**
- Modify: `src/main/java/com/franquicias/infrastructure/config/security/SecurityConfig.java`

**Interfaces:**
- Consumes: `JwtUtil.extractRole(String): String` (Task 4)
- Produces: `SecurityWebFilterChain` con reglas de acceso completas; `WebFilter` que inyecta `SimpleGrantedAuthority("ROLE_" + role)` en el contexto reactivo

- [ ] **Step 1: Reemplazar el contenido completo de `SecurityConfig.java`**

```java
package com.franquicias.infrastructure.config.security;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    public SecurityConfig(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(exchanges -> exchanges
                // Publicos
                .pathMatchers("/api/v1/auth/**").permitAll()
                .pathMatchers("/actuator/**").permitAll()
                .pathMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**").permitAll()
                // Solo ADMIN
                .pathMatchers(HttpMethod.POST, "/api/v1/usuarios").hasRole("ADMIN")
                // WRITE y ADMIN ÔÇö escritura sobre el catalogo
                .pathMatchers(HttpMethod.POST, "/api/v1/franquicias").hasAnyRole("WRITE", "ADMIN")
                .pathMatchers(HttpMethod.PATCH, "/api/v1/franquicias/**").hasAnyRole("WRITE", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/v1/franquicias/*/sucursales").hasAnyRole("WRITE", "ADMIN")
                .pathMatchers(HttpMethod.PATCH, "/api/v1/franquicias/*/sucursales/**").hasAnyRole("WRITE", "ADMIN")
                .pathMatchers(HttpMethod.POST, "/api/v1/franquicias/*/sucursales/*/productos").hasAnyRole("WRITE", "ADMIN")
                .pathMatchers(HttpMethod.PATCH, "/api/v1/franquicias/*/sucursales/*/productos/**").hasAnyRole("WRITE", "ADMIN")
                .pathMatchers(HttpMethod.DELETE, "/api/v1/franquicias/*/sucursales/*/productos/**").hasAnyRole("WRITE", "ADMIN")
                // READ, WRITE y ADMIN ÔÇö consultas
                .pathMatchers(HttpMethod.GET, "/api/v1/**").hasAnyRole("READ", "WRITE", "ADMIN")
                // Cualquier otra cosa autenticada
                .anyExchange().authenticated()
            )
            .addFilterAt(jwtFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
    }

    private WebFilter jwtFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String path = exchange.getRequest().getPath().value();
            if (path.startsWith("/api/v1/auth") || path.startsWith("/actuator")
                    || path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")
                    || path.startsWith("/webjars")) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtil.validateToken(token)) {
                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);
                    var authority = new SimpleGrantedAuthority("ROLE_" + role);
                    var auth = new UsernamePasswordAuthenticationToken(username, null, List.of(authority));
                    return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                }
            }
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        };
    }
}
```

- [ ] **Step 2: Compilar**

```bash
mvn compile -q
```

Resultado esperado: BUILD SUCCESS.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/config/security/SecurityConfig.java
git commit -m "feat(usuarios): update SecurityConfig with role-based access rules and authority injection"
```

---

### Task 6: AuthController real con BD y BCrypt

**Files:**
- Modify: `src/main/java/com/franquicias/infrastructure/web/controller/AuthController.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/auth/RegisterRequest.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/UsuarioResponse.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/controller/UsuarioController.java`
- Create: `src/test/java/com/franquicias/infrastructure/web/controller/AuthControllerTest.java` (nuevo, reemplaza el existente si existe)
- Create: `src/test/java/com/franquicias/infrastructure/web/controller/UsuarioControllerTest.java`

**Interfaces:**
- Consumes: `UsuarioRepositoryPort.findByUsername` (Task 3), `BCryptPasswordEncoder` (Task 4), `JwtUtil.generateToken(username, role)` (Task 4)
- Produces:
  - `POST /api/v1/auth/login` -> `AuthResponse { token: String }` o 401
  - `POST /api/v1/usuarios` -> `UsuarioResponse` o 403 si no es ADMIN o 409 si username/email duplicado

- [ ] **Step 1: Crear `RegisterRequest.java`**

```java
package com.franquicias.infrastructure.web.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.franquicias.domain.model.RolUsuario;

public record RegisterRequest(
    @NotBlank String username,
    @NotBlank String password,
    @Email @NotBlank String email,
    @NotNull RolUsuario role
) {}
```

- [ ] **Step 2: Crear `UsuarioResponse.java`**

```java
package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.RolUsuario;
import com.franquicias.domain.model.Usuario;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UsuarioResponse(
    UUID id,
    String username,
    String email,
    RolUsuario role,
    boolean activo,
    OffsetDateTime createdAt
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.id(), u.username(), u.email(), u.role(), u.activo(), u.createdAt());
    }
}
```

- [ ] **Step 3: Reemplazar `AuthController.java`**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.domain.port.UsuarioRepositoryPort;
import com.franquicias.infrastructure.config.security.JwtUtil;
import com.franquicias.infrastructure.web.dto.auth.AuthRequest;
import com.franquicias.infrastructure.web.dto.auth.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Autenticacion", description = "Endpoints para la gestion de tokens JWT")
public class AuthController {

    private final JwtUtil jwtUtil;
    private final UsuarioRepositoryPort usuarioPort;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(JwtUtil jwtUtil, UsuarioRepositoryPort usuarioPort, BCryptPasswordEncoder passwordEncoder) {
        this.jwtUtil = jwtUtil;
        this.usuarioPort = usuarioPort;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/v1/auth/login")
    @Operation(summary = "Generar Token JWT", description = "Autentica con username y password. Retorna un JWT con el rol del usuario incluido en el claim 'role'.")
    public Mono<AuthResponse> login(@RequestBody AuthRequest request) {
        return usuarioPort.findByUsername(request.username())
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas")))
            .flatMap(usuario -> {
                if (!usuario.activo()) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario inactivo"));
                }
                if (!passwordEncoder.matches(request.password(), usuario.passwordHash())) {
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));
                }
                String token = jwtUtil.generateToken(usuario.username(), usuario.role().name());
                return Mono.just(new AuthResponse(token));
            });
    }
}
```

- [ ] **Step 4: Crear `UsuarioController.java`**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.domain.model.Usuario;
import com.franquicias.domain.port.UsuarioRepositoryPort;
import com.franquicias.infrastructure.web.dto.UsuarioResponse;
import com.franquicias.infrastructure.web.dto.auth.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@Tag(name = "Usuarios", description = "Gestion de usuarios del sistema (solo ADMIN)")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioRepositoryPort usuarioPort;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepositoryPort usuarioPort, BCryptPasswordEncoder passwordEncoder) {
        this.usuarioPort = usuarioPort;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/api/v1/usuarios")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear Usuario", description = "Crea un nuevo usuario con el rol especificado. Solo accesible para usuarios con rol ADMIN.")
    public Mono<UsuarioResponse> crear(@Valid @RequestBody RegisterRequest request) {
        String hash = passwordEncoder.encode(request.password());
        Usuario nuevo = new Usuario(null, request.username(), hash, request.email(), request.role(), true, null);
        return usuarioPort.save(nuevo)
            .map(UsuarioResponse::from)
            .onErrorResume(DataIntegrityViolationException.class, e -> {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("uq_usuario_username")) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "El username ya existe"));
                }
                if (msg.contains("uq_usuario_email")) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "El email ya existe"));
                }
                return Mono.error(e);
            });
    }
}
```

- [ ] **Step 5: Escribir `AuthControllerTest.java`**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.infrastructure.web.dto.auth.AuthRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void login_adminCredentials_returnsToken() {
        webTestClient.post().uri("/api/v1/auth/login")
            .bodyValue(new AuthRequest("admin", "admin123"))
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void login_wrongPassword_returns401() {
        webTestClient.post().uri("/api/v1/auth/login")
            .bodyValue(new AuthRequest("admin", "wrong"))
            .exchange()
            .expectStatus().isUnauthorized();
    }

    @Test
    void login_unknownUser_returns401() {
        webTestClient.post().uri("/api/v1/auth/login")
            .bodyValue(new AuthRequest("noexiste", "pass"))
            .exchange()
            .expectStatus().isUnauthorized();
    }
}
```

- [ ] **Step 6: Escribir `UsuarioControllerTest.java`**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.infrastructure.config.security.JwtUtil;
import com.franquicias.infrastructure.web.dto.auth.RegisterRequest;
import com.franquicias.domain.model.RolUsuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

class UsuarioControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;
    private String writeToken;
    private String readToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateToken("admin", "ADMIN");
        writeToken = jwtUtil.generateToken("writer", "WRITE");
        readToken  = jwtUtil.generateToken("reader", "READ");
    }

    @Test
    void crearUsuario_conAdmin_returns201() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + adminToken)
            .bodyValue(new RegisterRequest("newuser", "pass123", "new@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.username").isEqualTo("newuser")
            .jsonPath("$.role").isEqualTo("READ");
    }

    @Test
    void crearUsuario_conWrite_returns403() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + writeToken)
            .bodyValue(new RegisterRequest("otro", "pass123", "otro@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isForbidden();
    }

    @Test
    void crearUsuario_conRead_returns403() {
        webTestClient.post().uri("/api/v1/usuarios")
            .header("Authorization", "Bearer " + readToken)
            .bodyValue(new RegisterRequest("otro2", "pass123", "otro2@test.com", RolUsuario.READ))
            .exchange()
            .expectStatus().isForbidden();
    }
}
```

- [ ] **Step 7: Ejecutar los tests nuevos**

```bash
mvn test -pl . -Dtest="AuthControllerTest,UsuarioControllerTest" -q
```

Resultado esperado: BUILD SUCCESS, 6 tests passed.

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/web/controller/AuthController.java
git add src/main/java/com/franquicias/infrastructure/web/controller/UsuarioController.java
git add src/main/java/com/franquicias/infrastructure/web/dto/auth/RegisterRequest.java
git add src/main/java/com/franquicias/infrastructure/web/dto/UsuarioResponse.java
git add src/test/java/com/franquicias/infrastructure/web/controller/AuthControllerTest.java
git add src/test/java/com/franquicias/infrastructure/web/controller/UsuarioControllerTest.java
git commit -m "feat(usuarios): real auth with DB+BCrypt, UsuarioController ADMIN-only, new tests"
```

---

### Task 7: Actualizar tests existentes para firma nueva de generateToken

**Files:**
- Modify: `src/test/java/com/franquicias/infrastructure/web/controller/FranquiciaControllerTest.java`
- Modify: `src/test/java/com/franquicias/infrastructure/web/controller/SucursalControllerTest.java`
- Modify: `src/test/java/com/franquicias/infrastructure/web/controller/ProductoControllerTest.java`

**Interfaces:**
- Consumes: `JwtUtil.generateToken(String username, String role)` (Task 4) ÔÇö la firma cambio de 1 argumento a 2

- [ ] **Step 1: Actualizar `FranquiciaControllerTest.java`**

Buscar la linea que llama a `jwtUtil.generateToken(...)` en `@BeforeEach` y cambiarla de:

```java
token = jwtUtil.generateToken("admin");
```

a:

```java
token = jwtUtil.generateToken("admin", "ADMIN");
```

- [ ] **Step 2: Actualizar `SucursalControllerTest.java`**

Misma busqueda y reemplazo:

```java
// antes
token = jwtUtil.generateToken("admin");
// despues
token = jwtUtil.generateToken("admin", "ADMIN");
```

- [ ] **Step 3: Actualizar `ProductoControllerTest.java`**

```java
// antes
token = jwtUtil.generateToken("admin");
// despues
token = jwtUtil.generateToken("admin", "ADMIN");
```

- [ ] **Step 4: Ejecutar toda la suite de tests**

```bash
mvn test -q
```

Resultado esperado: BUILD SUCCESS, todos los tests pasan.

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/franquicias/infrastructure/web/controller/FranquiciaControllerTest.java
git add src/test/java/com/franquicias/infrastructure/web/controller/SucursalControllerTest.java
git add src/test/java/com/franquicias/infrastructure/web/controller/ProductoControllerTest.java
git commit -m "test(usuarios): update existing controller tests to use generateToken with role param"
```

---

### Task 8: Actualizar scripts de prueba y crear PR

**Files:**
- Modify: `test.ps1`
- Modify: `test.sh`
- Modify: `api-tests.http`

- [ ] **Step 1: Verificar que `test.ps1` y `test.sh` usan login real**

Los scripts ya llaman a `/api/v1/auth/login`. Verificar que el payload sea `{"username":"admin","password":"admin123"}` ÔÇö si es asi, no hay cambio necesario pues el endpoint mantiene el mismo contrato.

- [ ] **Step 2: Agregar ejemplo de creacion de usuario en `api-tests.http`**

Agregar al final del archivo:

```http
### Crear usuario WRITE (requiere token ADMIN)
POST http://localhost:8080/api/v1/usuarios
Authorization: Bearer {{token}}
Content-Type: application/json

{
  "username": "operador1",
  "password": "operador123",
  "email": "operador1@franquicias.com",
  "role": "WRITE"
}
```

- [ ] **Step 3: Ejecutar suite completa de tests por ultima vez**

```bash
mvn test -q
```

Resultado esperado: BUILD SUCCESS.

- [ ] **Step 4: Push y crear PR**

```bash
git add test.ps1 test.sh api-tests.http
git commit -m "docs: add usuario creation example to api-tests.http"
git push origin feature/usuarios-roles
gh pr create \
  --title "feat: Usuarios reales con roles ADMIN/WRITE/READ y autenticacion BCrypt" \
  --body-file docs/superpowers/specs/2026-08-24-usuarios-roles-jwt.md \
  --base develop
```

---

## Self-Review

**Spec coverage:**
- Tabla `usuario` con todos los campos ÔÇö Task 1
- Enum `rol_usuario` PostgreSQL ÔÇö Task 1
- Seed admin ÔÇö Task 1
- Dominio `Usuario`, `RolUsuario`, `UsuarioRepositoryPort` ÔÇö Task 2
- Persistencia R2DBC completa ÔÇö Task 3
- BCrypt bean ÔÇö Task 4
- `JwtUtil` con claim `role` ÔÇö Task 4
- `SecurityConfig` con reglas por path+metodo ÔÇö Task 5
- `AuthController` real con BD ÔÇö Task 6
- `UsuarioController` ADMIN-only ÔÇö Task 6
- Tests de integracion para persistencia ÔÇö Task 3
- Tests de integracion para auth y roles ÔÇö Task 6
- Actualizacion de tests existentes ÔÇö Task 7
- Manejo de errores 401/403/409 ÔÇö Tasks 5 y 6

**Gaps:** ninguno detectado.

**Type consistency:** `generateToken(username, role)` definido en Task 4, consumido en Tasks 6 y 7 con la misma firma.
