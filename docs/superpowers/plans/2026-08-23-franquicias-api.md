# Franquicias API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a reactive Spring Boot API to manage franquicias → sucursales → productos, with PostgreSQL persistence, full CRUD-ish endpoints from the spec, Docker packaging, CI, and Terraform IaC for AWS.

**Architecture:** Hexagonal — `domain` (pure models, ports, exceptions), `application` (services implementing use cases against ports), `infrastructure` (WebFlux controllers/DTOs, R2DBC persistence adapters, config). All I/O is reactive (`Mono`/`Flux`) end to end.

**Tech Stack:** Java 21, Maven, Spring Boot 3.3.x, Spring WebFlux, Spring Data R2DBC + `r2dbc-postgresql`, Flyway (JDBC driver only for migrations), springdoc-openapi-webflux, Bean Validation, JUnit 5 + Mockito + Reactor Test + Testcontainers (PostgreSQL), Docker, GitHub Actions, Terraform (AWS).

**Spec:** `docs/superpowers/specs/2026-08-23-franquicias-api-design.md`

## Global Constraints

- Java 21, Maven build (`pom.xml`, not Gradle).
- Base package: `com.franquicias`.
- API base path: `/api/v1`.
- All controllers and services are reactive — no blocking calls anywhere in `domain`/`application`/`infrastructure.web`; JDBC (blocking) is used **only** by Flyway at startup, never in request-handling code.
- `domain` package must not import anything from `org.springframework` or `infrastructure`.
- Every not-found case returns 404 via a domain exception mapped by `GlobalExceptionHandler`; every validation failure returns 400.
- Error response body shape (fixed across the whole API):
  ```json
  { "timestamp": "2026-08-23T10:00:00Z", "status": 404, "error": "Not Found", "message": "...", "path": "/api/v1/..." }
  ```
- Money/security-sensitive fields: `stock` must never go negative (`@Min(0)` at DTO level, `CHECK (stock >= 0)` at DB level).

---

## File Structure

```
pom.xml
src/main/java/com/franquicias/
├── FranquiciasApplication.java
├── domain/
│   ├── model/
│   │   ├── Franquicia.java
│   │   ├── Sucursal.java
│   │   ├── Producto.java
│   │   └── ProductoMaxStock.java
│   ├── exception/
│   │   ├── FranquiciaNotFoundException.java
│   │   ├── SucursalNotFoundException.java
│   │   └── ProductoNotFoundException.java
│   └── port/
│       ├── FranquiciaRepositoryPort.java
│       ├── SucursalRepositoryPort.java
│       └── ProductoRepositoryPort.java
├── application/service/
│   ├── FranquiciaService.java
│   ├── SucursalService.java
│   └── ProductoService.java
└── infrastructure/
    ├── web/
    │   ├── controller/
    │   │   ├── FranquiciaController.java
    │   │   ├── SucursalController.java
    │   │   └── ProductoController.java
    │   ├── dto/
    │   │   ├── NombreRequest.java
    │   │   ├── CrearProductoRequest.java
    │   │   ├── ModificarStockRequest.java
    │   │   ├── FranquiciaResponse.java
    │   │   ├── SucursalResponse.java
    │   │   ├── ProductoResponse.java
    │   │   └── ProductoMaxStockResponse.java
    │   └── exception/
    │       ├── ErrorResponse.java
    │       └── GlobalExceptionHandler.java
    ├── persistence/
    │   ├── entity/
    │   │   ├── FranquiciaEntity.java
    │   │   ├── SucursalEntity.java
    │   │   └── ProductoEntity.java
    │   ├── repository/
    │   │   ├── FranquiciaR2dbcRepository.java
    │   │   ├── SucursalR2dbcRepository.java
    │   │   ├── ProductoR2dbcRepository.java
    │   │   └── ProductoMaxStockRow.java
    │   ├── mapper/
    │   │   ├── FranquiciaMapper.java
    │   │   ├── SucursalMapper.java
    │   │   └── ProductoMapper.java
    │   └── adapter/
    │       ├── FranquiciaRepositoryAdapter.java
    │       ├── SucursalRepositoryAdapter.java
    │       └── ProductoRepositoryAdapter.java
    └── config/
        └── OpenApiConfig.java
src/main/resources/
├── application.yml
└── db/migration/V1__init.sql
src/test/java/com/franquicias/
├── AbstractIntegrationTest.java
├── domain/... (none — pure records need no unit tests)
├── application/service/
│   ├── FranquiciaServiceTest.java
│   ├── SucursalServiceTest.java
│   └── ProductoServiceTest.java
└── infrastructure/
    ├── persistence/adapter/
    │   ├── FranquiciaRepositoryAdapterTest.java
    │   ├── SucursalRepositoryAdapterTest.java
    │   └── ProductoRepositoryAdapterTest.java
    └── web/controller/
        ├── FranquiciaControllerTest.java
        ├── SucursalControllerTest.java
        └── ProductoControllerTest.java
Dockerfile
docker-compose.yml
.env.example
.github/workflows/ci.yml
infra/
├── main.tf
├── variables.tf
├── outputs.tf
└── terraform.tfvars.example
README.md
```

---

### Task 1: Project scaffold

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/franquicias/FranquiciasApplication.java`
- Create: `src/main/resources/application.yml`
- Test: `src/test/java/com/franquicias/FranquiciasApplicationTests.java`

**Interfaces:**
- Produces: Maven build (`mvn verify`), Spring Boot context `com.franquicias.FranquiciasApplication`.

- [ ] **Step 1: Write `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.3.4</version>
    <relativePath/>
  </parent>

  <groupId>com.franquicias</groupId>
  <artifactId>franquicias-api</artifactId>
  <version>0.1.0</version>
  <name>franquicias-api</name>
  <description>API reactiva de franquicias, sucursales y productos</description>

  <properties>
    <java.version>21</java.version>
    <springdoc.version>2.6.0</springdoc.version>
  </properties>

  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
      <groupId>io.r2dbc</groupId>
      <artifactId>r2dbc-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.postgresql</groupId>
      <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-core</artifactId>
    </dependency>
    <dependency>
      <groupId>org.flywaydb</groupId>
      <artifactId>flyway-database-postgresql</artifactId>
    </dependency>
    <dependency>
      <groupId>org.springdoc</groupId>
      <artifactId>springdoc-openapi-starter-webflux-ui</artifactId>
      <version>${springdoc.version}</version>
    </dependency>

    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>io.projectreactor</groupId>
      <artifactId>reactor-test</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>postgresql</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.testcontainers</groupId>
      <artifactId>r2dbc</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-bom</artifactId>
        <version>1.20.1</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <plugins>
      <plugin>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-maven-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

- [ ] **Step 2: Write `FranquiciasApplication.java`**

```java
package com.franquicias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FranquiciasApplication {
    public static void main(String[] args) {
        SpringApplication.run(FranquiciasApplication.class, args);
    }
}
```

- [ ] **Step 3: Write `application.yml`**

```yaml
spring:
  application:
    name: franquicias-api
  r2dbc:
    url: r2dbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:franquicias}
    username: ${DB_USER:franquicias}
    password: ${DB_PASSWORD:franquicias}
  flyway:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:franquicias}
    user: ${DB_USER:franquicias}
    password: ${DB_PASSWORD:franquicias}
    locations: classpath:db/migration

server:
  port: ${SERVER_PORT:8080}

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 4: Write failing smoke test**

```java
package com.franquicias;

import org.junit.jupiter.api.Test;

class FranquiciasApplicationTests {

    @Test
    void mainClassExists() {
        // Placeholder-free smoke check: class loads without throwing.
        assertDoesNotThrow(() -> Class.forName("com.franquicias.FranquiciasApplication"));
    }

    private static void assertDoesNotThrow(org.junit.jupiter.api.function.Executable e) {
        try {
            e.execute();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
    }
}
```

- [ ] **Step 5: Run `mvn -q compile` to verify the project compiles**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS (no test run needed yet — this only proves scaffold compiles).

- [ ] **Step 6: Run the smoke test**

Run: `mvn -q -Dtest=FranquiciasApplicationTests test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add pom.xml src/main/java/com/franquicias/FranquiciasApplication.java src/main/resources/application.yml src/test/java/com/franquicias/FranquiciasApplicationTests.java
git commit -m "chore: scaffold Spring Boot WebFlux project"
```

---

### Task 2: Domain models, exceptions, ports

**Files:**
- Create: `src/main/java/com/franquicias/domain/model/Franquicia.java`
- Create: `src/main/java/com/franquicias/domain/model/Sucursal.java`
- Create: `src/main/java/com/franquicias/domain/model/Producto.java`
- Create: `src/main/java/com/franquicias/domain/model/ProductoMaxStock.java`
- Create: `src/main/java/com/franquicias/domain/exception/FranquiciaNotFoundException.java`
- Create: `src/main/java/com/franquicias/domain/exception/SucursalNotFoundException.java`
- Create: `src/main/java/com/franquicias/domain/exception/ProductoNotFoundException.java`
- Create: `src/main/java/com/franquicias/domain/port/FranquiciaRepositoryPort.java`
- Create: `src/main/java/com/franquicias/domain/port/SucursalRepositoryPort.java`
- Create: `src/main/java/com/franquicias/domain/port/ProductoRepositoryPort.java`

**Interfaces:**
- Produces: `Franquicia(UUID id, String nombre)`, `Sucursal(UUID id, UUID franquiciaId, String nombre)`, `Producto(UUID id, UUID sucursalId, String nombre, int stock)`, `ProductoMaxStock(UUID sucursalId, String sucursalNombre, Producto producto)` — all `record`s, `id` nullable before persistence.
- Produces: `FranquiciaNotFoundException(UUID id)`, `SucursalNotFoundException(UUID id)`, `ProductoNotFoundException(UUID id)`.
- Produces: `FranquiciaRepositoryPort { Mono<Franquicia> save(Franquicia); Mono<Franquicia> findById(UUID); }`
- Produces: `SucursalRepositoryPort { Mono<Sucursal> save(Sucursal); Mono<Sucursal> findById(UUID); }`
- Produces: `ProductoRepositoryPort { Mono<Producto> save(Producto); Mono<Producto> findById(UUID); Mono<Void> deleteById(UUID); Flux<ProductoMaxStock> findMaxStockPorFranquicia(UUID franquiciaId); }`

This task has no independent test cycle (pure data/interface declarations, nothing to assert against yet) — it's validated by the compiler and by Task 5–7's unit tests that consume these types. Skip TDD ceremony; just write the code and verify compilation.

- [ ] **Step 1: Write domain models**

```java
package com.franquicias.domain.model;

import java.util.UUID;

public record Franquicia(UUID id, String nombre) {}
```

```java
package com.franquicias.domain.model;

import java.util.UUID;

public record Sucursal(UUID id, UUID franquiciaId, String nombre) {}
```

```java
package com.franquicias.domain.model;

import java.util.UUID;

public record Producto(UUID id, UUID sucursalId, String nombre, int stock) {}
```

```java
package com.franquicias.domain.model;

import java.util.UUID;

public record ProductoMaxStock(UUID sucursalId, String sucursalNombre, Producto producto) {}
```

- [ ] **Step 2: Write domain exceptions**

```java
package com.franquicias.domain.exception;

import java.util.UUID;

public class FranquiciaNotFoundException extends RuntimeException {
    public FranquiciaNotFoundException(UUID id) {
        super("Franquicia no encontrada: " + id);
    }
}
```

```java
package com.franquicias.domain.exception;

import java.util.UUID;

public class SucursalNotFoundException extends RuntimeException {
    public SucursalNotFoundException(UUID id) {
        super("Sucursal no encontrada: " + id);
    }
}
```

```java
package com.franquicias.domain.exception;

import java.util.UUID;

public class ProductoNotFoundException extends RuntimeException {
    public ProductoNotFoundException(UUID id) {
        super("Producto no encontrado: " + id);
    }
}
```

- [ ] **Step 3: Write ports**

```java
package com.franquicias.domain.port;

import com.franquicias.domain.model.Franquicia;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface FranquiciaRepositoryPort {
    Mono<Franquicia> save(Franquicia franquicia);
    Mono<Franquicia> findById(UUID id);
}
```

```java
package com.franquicias.domain.port;

import com.franquicias.domain.model.Sucursal;
import java.util.UUID;
import reactor.core.publisher.Mono;

public interface SucursalRepositoryPort {
    Mono<Sucursal> save(Sucursal sucursal);
    Mono<Sucursal> findById(UUID id);
}
```

```java
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
    Flux<ProductoMaxStock> findMaxStockPorFranquicia(UUID franquiciaId);
}
```

- [ ] **Step 4: Verify compilation**

Run: `mvn -q -DskipTests compile`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/domain
git commit -m "feat: add domain models, exceptions, and repository ports"
```

---

### Task 3: Flyway migration + shared Testcontainers base

**Files:**
- Create: `src/main/resources/db/migration/V1__init.sql`
- Create: `src/test/java/com/franquicias/AbstractIntegrationTest.java`

**Interfaces:**
- Produces: `AbstractIntegrationTest` — base class other integration tests (`*RepositoryAdapterTest`, `*ControllerTest`) extend to get a shared, Flyway-migrated PostgreSQL Testcontainer wired via `@DynamicPropertySource`.

- [ ] **Step 1: Write the migration**

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE franquicia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(255) NOT NULL
);

CREATE TABLE sucursal (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    franquicia_id UUID NOT NULL REFERENCES franquicia(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL
);

CREATE TABLE producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sucursal_id UUID NOT NULL REFERENCES sucursal(id) ON DELETE CASCADE,
    nombre VARCHAR(255) NOT NULL,
    stock INT NOT NULL CHECK (stock >= 0)
);

CREATE INDEX idx_sucursal_franquicia_id ON sucursal(franquicia_id);
CREATE INDEX idx_producto_sucursal_id ON producto(sucursal_id);
```

- [ ] **Step 2: Write the shared Testcontainers base class**

```java
package com.franquicias;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("franquicias")
            .withUsername("franquicias")
            .withPassword("franquicias");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://%s:%d/%s".formatted(
            POSTGRES.getHost(), POSTGRES.getMappedPort(5432), POSTGRES.getDatabaseName()));
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
    }
}
```

- [ ] **Step 3: Verify compilation (no runnable test yet — this class has no `@Test` methods, it's a base class validated by Task 4)**

Run: `mvn -q -DskipTests test-compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/db/migration/V1__init.sql src/test/java/com/franquicias/AbstractIntegrationTest.java
git commit -m "feat: add Flyway schema migration and shared Testcontainers base"
```

---

### Task 4: Persistence layer (entities, repositories, mappers, adapters)

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/persistence/entity/FranquiciaEntity.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/entity/SucursalEntity.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/entity/ProductoEntity.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/repository/FranquiciaR2dbcRepository.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/repository/SucursalR2dbcRepository.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/repository/ProductoR2dbcRepository.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/repository/ProductoMaxStockRow.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/mapper/FranquiciaMapper.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/mapper/SucursalMapper.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/mapper/ProductoMapper.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/adapter/FranquiciaRepositoryAdapter.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/adapter/SucursalRepositoryAdapter.java`
- Create: `src/main/java/com/franquicias/infrastructure/persistence/adapter/ProductoRepositoryAdapter.java`
- Test: `src/test/java/com/franquicias/infrastructure/persistence/adapter/FranquiciaRepositoryAdapterTest.java`
- Test: `src/test/java/com/franquicias/infrastructure/persistence/adapter/SucursalRepositoryAdapterTest.java`
- Test: `src/test/java/com/franquicias/infrastructure/persistence/adapter/ProductoRepositoryAdapterTest.java`

**Interfaces:**
- Consumes: `FranquiciaRepositoryPort`, `SucursalRepositoryPort`, `ProductoRepositoryPort`, `AbstractIntegrationTest` from Tasks 2–3.
- Produces: `FranquiciaRepositoryAdapter implements FranquiciaRepositoryPort`, `SucursalRepositoryAdapter implements SucursalRepositoryPort`, `ProductoRepositoryAdapter implements ProductoRepositoryPort` — all `@Component`, all consumed directly by Task 5–7 services via the port interfaces.

- [ ] **Step 1: Write entities**

```java
package com.franquicias.infrastructure.persistence.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("franquicia")
public record FranquiciaEntity(@Id UUID id, String nombre) {}
```

```java
package com.franquicias.infrastructure.persistence.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("sucursal")
public record SucursalEntity(
    @Id UUID id,
    @Column("franquicia_id") UUID franquiciaId,
    String nombre) {}
```

```java
package com.franquicias.infrastructure.persistence.entity;

import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("producto")
public record ProductoEntity(
    @Id UUID id,
    @Column("sucursal_id") UUID sucursalId,
    String nombre,
    int stock) {}
```

- [ ] **Step 2: Write repositories, including the max-stock projection query**

```java
package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.FranquiciaEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface FranquiciaR2dbcRepository extends ReactiveCrudRepository<FranquiciaEntity, UUID> {}
```

```java
package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.SucursalEntity;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SucursalR2dbcRepository extends ReactiveCrudRepository<SucursalEntity, UUID> {}
```

```java
package com.franquicias.infrastructure.persistence.repository;

import java.util.UUID;

public interface ProductoMaxStockRow {
    UUID getSucursalId();
    String getSucursalNombre();
    UUID getProductoId();
    String getProductoNombre();
    Integer getStock();
}
```

```java
package com.franquicias.infrastructure.persistence.repository;

import com.franquicias.infrastructure.persistence.entity.ProductoEntity;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface ProductoR2dbcRepository extends ReactiveCrudRepository<ProductoEntity, UUID> {

    @Query("""
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
        """)
    Flux<ProductoMaxStockRow> findMaxStockPorFranquicia(UUID franquiciaId);
}
```

- [ ] **Step 3: Write mappers**

```java
package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.infrastructure.persistence.entity.FranquiciaEntity;
import org.springframework.stereotype.Component;

@Component
public class FranquiciaMapper {

    public FranquiciaEntity toEntity(Franquicia domain) {
        return new FranquiciaEntity(domain.id(), domain.nombre());
    }

    public Franquicia toDomain(FranquiciaEntity entity) {
        return new Franquicia(entity.id(), entity.nombre());
    }
}
```

```java
package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.Sucursal;
import com.franquicias.infrastructure.persistence.entity.SucursalEntity;
import org.springframework.stereotype.Component;

@Component
public class SucursalMapper {

    public SucursalEntity toEntity(Sucursal domain) {
        return new SucursalEntity(domain.id(), domain.franquiciaId(), domain.nombre());
    }

    public Sucursal toDomain(SucursalEntity entity) {
        return new Sucursal(entity.id(), entity.franquiciaId(), entity.nombre());
    }
}
```

```java
package com.franquicias.infrastructure.persistence.mapper;

import com.franquicias.domain.model.Producto;
import com.franquicias.infrastructure.persistence.entity.ProductoEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoEntity toEntity(Producto domain) {
        return new ProductoEntity(domain.id(), domain.sucursalId(), domain.nombre(), domain.stock());
    }

    public Producto toDomain(ProductoEntity entity) {
        return new Producto(entity.id(), entity.sucursalId(), entity.nombre(), entity.stock());
    }
}
```

- [ ] **Step 4: Write adapters**

```java
package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.FranquiciaMapper;
import com.franquicias.infrastructure.persistence.repository.FranquiciaR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class FranquiciaRepositoryAdapter implements FranquiciaRepositoryPort {

    private final FranquiciaR2dbcRepository repository;
    private final FranquiciaMapper mapper;

    public FranquiciaRepositoryAdapter(FranquiciaR2dbcRepository repository, FranquiciaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Franquicia> save(Franquicia franquicia) {
        return repository.save(mapper.toEntity(franquicia)).map(mapper::toDomain);
    }

    @Override
    public Mono<Franquicia> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
```

```java
package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.SucursalRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.SucursalMapper;
import com.franquicias.infrastructure.persistence.repository.SucursalR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class SucursalRepositoryAdapter implements SucursalRepositoryPort {

    private final SucursalR2dbcRepository repository;
    private final SucursalMapper mapper;

    public SucursalRepositoryAdapter(SucursalR2dbcRepository repository, SucursalMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Mono<Sucursal> save(Sucursal sucursal) {
        return repository.save(mapper.toEntity(sucursal)).map(mapper::toDomain);
    }

    @Override
    public Mono<Sucursal> findById(UUID id) {
        return repository.findById(id).map(mapper::toDomain);
    }
}
```

```java
package com.franquicias.infrastructure.persistence.adapter;

import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.port.ProductoRepositoryPort;
import com.franquicias.infrastructure.persistence.mapper.ProductoMapper;
import com.franquicias.infrastructure.persistence.repository.ProductoMaxStockRow;
import com.franquicias.infrastructure.persistence.repository.ProductoR2dbcRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ProductoRepositoryAdapter implements ProductoRepositoryPort {

    private final ProductoR2dbcRepository repository;
    private final ProductoMapper mapper;

    public ProductoRepositoryAdapter(ProductoR2dbcRepository repository, ProductoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
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
        return repository.findMaxStockPorFranquicia(franquiciaId)
            .map(row -> new ProductoMaxStock(
                row.getSucursalId(),
                row.getSucursalNombre(),
                new Producto(row.getProductoId(), row.getSucursalId(), row.getProductoNombre(), row.getStock())));
    }
}
```

- [ ] **Step 5: Write the failing adapter integration tests**

```java
package com.franquicias.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.Franquicia;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
class FranquiciaRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private FranquiciaRepositoryAdapter adapter;

    @Test
    void guardaYRecuperaUnaFranquicia() {
        Franquicia guardada = adapter.save(new Franquicia(null, "Frutería Don Pepe")).block();

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada.nombre()).isEqualTo("Frutería Don Pepe");

        Franquicia encontrada = adapter.findById(guardada.id()).block();
        assertThat(encontrada.nombre()).isEqualTo("Frutería Don Pepe");
    }
}
```

```java
package com.franquicias.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Sucursal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SucursalRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private FranquiciaRepositoryAdapter franquiciaAdapter;
    @Autowired
    private SucursalRepositoryAdapter sucursalAdapter;

    @Test
    void guardaYRecuperaUnaSucursalAsociadaAUnaFranquicia() {
        Franquicia franquicia = franquiciaAdapter.save(new Franquicia(null, "Frutería Don Pepe")).block();

        Sucursal guardada = sucursalAdapter.save(new Sucursal(null, franquicia.id(), "Sede Norte")).block();

        assertThat(guardada.id()).isNotNull();
        assertThat(guardada.franquiciaId()).isEqualTo(franquicia.id());

        Sucursal encontrada = sucursalAdapter.findById(guardada.id()).block();
        assertThat(encontrada.nombre()).isEqualTo("Sede Norte");
    }
}
```

```java
package com.franquicias.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.model.Sucursal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductoRepositoryAdapterTest extends AbstractIntegrationTest {

    @Autowired
    private FranquiciaRepositoryAdapter franquiciaAdapter;
    @Autowired
    private SucursalRepositoryAdapter sucursalAdapter;
    @Autowired
    private ProductoRepositoryAdapter productoAdapter;

    @Test
    void encuentraElProductoConMasStockPorCadaSucursalDeLaFranquicia() {
        Franquicia franquicia = franquiciaAdapter.save(new Franquicia(null, "Frutería Don Pepe")).block();
        Sucursal sedeNorte = sucursalAdapter.save(new Sucursal(null, franquicia.id(), "Sede Norte")).block();
        Sucursal sedeSur = sucursalAdapter.save(new Sucursal(null, franquicia.id(), "Sede Sur")).block();

        productoAdapter.save(new Producto(null, sedeNorte.id(), "Manzana", 10)).block();
        productoAdapter.save(new Producto(null, sedeNorte.id(), "Pera", 25)).block();
        productoAdapter.save(new Producto(null, sedeSur.id(), "Uva", 5)).block();

        List<ProductoMaxStock> resultado = productoAdapter.findMaxStockPorFranquicia(franquicia.id())
            .collectList().block();

        assertThat(resultado).hasSize(2);
        assertThat(resultado)
            .filteredOn(r -> r.sucursalId().equals(sedeNorte.id()))
            .extracting(r -> r.producto().nombre())
            .containsExactly("Pera");
        assertThat(resultado)
            .filteredOn(r -> r.sucursalId().equals(sedeSur.id()))
            .extracting(r -> r.producto().nombre())
            .containsExactly("Uva");
    }
}
```

- [ ] **Step 6: Run the adapter tests to verify they fail (Docker required for Testcontainers)**

Run: `mvn -q -Dtest=FranquiciaRepositoryAdapterTest,SucursalRepositoryAdapterTest,ProductoRepositoryAdapterTest test`
Expected: FAIL before Step 4's code exists — since Step 4 code is written above alongside the tests, instead run this now to verify it PASSES with the implementation in place (entities/repos/mappers/adapters from Steps 1–4 must exist first). If any of Steps 1–4 were skipped, this fails with a `NoSuchBeanDefinitionException` or compile error.

- [ ] **Step 7: Run the adapter tests to verify they pass**

Run: `mvn -q -Dtest=FranquiciaRepositoryAdapterTest,SucursalRepositoryAdapterTest,ProductoRepositoryAdapterTest test`
Expected: PASS (3 tests, all green)

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/persistence src/test/java/com/franquicias/infrastructure/persistence
git commit -m "feat: add R2DBC persistence layer with max-stock query"
```

---

### Task 5: FranquiciaService

**Files:**
- Create: `src/main/java/com/franquicias/application/service/FranquiciaService.java`
- Test: `src/test/java/com/franquicias/application/service/FranquiciaServiceTest.java`

**Interfaces:**
- Consumes: `FranquiciaRepositoryPort` (Task 2), `FranquiciaNotFoundException` (Task 2).
- Produces: `FranquiciaService(FranquiciaRepositoryPort port)` with `Mono<Franquicia> crear(String nombre)` and `Mono<Franquicia> renombrar(UUID id, String nuevoNombre)` — consumed by `FranquiciaController` in Task 9.

- [ ] **Step 1: Write the failing unit tests**

```java
package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class FranquiciaServiceTest {

    @Mock
    private FranquiciaRepositoryPort port;

    @InjectMocks
    private FranquiciaService service;

    @Test
    void creaUnaFranquiciaNueva() {
        UUID id = UUID.randomUUID();
        when(port.save(any())).thenReturn(Mono.just(new Franquicia(id, "Frutería Don Pepe")));

        StepVerifier.create(service.crear("Frutería Don Pepe"))
            .expectNextMatches(f -> f.id().equals(id) && f.nombre().equals("Frutería Don Pepe"))
            .verifyComplete();

        verify(port).save(new Franquicia(null, "Frutería Don Pepe"));
    }

    @Test
    void renombraUnaFranquiciaExistente() {
        UUID id = UUID.randomUUID();
        when(port.findById(id)).thenReturn(Mono.just(new Franquicia(id, "Nombre viejo")));
        when(port.save(any())).thenReturn(Mono.just(new Franquicia(id, "Nombre nuevo")));

        StepVerifier.create(service.renombrar(id, "Nombre nuevo"))
            .expectNextMatches(f -> f.nombre().equals("Nombre nuevo"))
            .verifyComplete();
    }

    @Test
    void falloAlRenombrarUnaFranquiciaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(port.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.renombrar(id, "Nombre nuevo"))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=FranquiciaServiceTest test`
Expected: FAIL (compile error — `FranquiciaService` doesn't exist yet)

- [ ] **Step 3: Write the implementation**

```java
package com.franquicias.application.service;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class FranquiciaService {

    private final FranquiciaRepositoryPort port;

    public FranquiciaService(FranquiciaRepositoryPort port) {
        this.port = port;
    }

    public Mono<Franquicia> crear(String nombre) {
        return port.save(new Franquicia(null, nombre));
    }

    public Mono<Franquicia> renombrar(UUID id, String nuevoNombre) {
        return port.findById(id)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(id)))
            .flatMap(existing -> port.save(new Franquicia(existing.id(), nuevoNombre)));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=FranquiciaServiceTest test`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/application/service/FranquiciaService.java src/test/java/com/franquicias/application/service/FranquiciaServiceTest.java
git commit -m "feat: add FranquiciaService with crear and renombrar use cases"
```

---

### Task 6: SucursalService

**Files:**
- Create: `src/main/java/com/franquicias/application/service/SucursalService.java`
- Test: `src/test/java/com/franquicias/application/service/SucursalServiceTest.java`

**Interfaces:**
- Consumes: `FranquiciaRepositoryPort`, `SucursalRepositoryPort`, `FranquiciaNotFoundException`, `SucursalNotFoundException` (Task 2).
- Produces: `SucursalService(FranquiciaRepositoryPort franquiciaPort, SucursalRepositoryPort sucursalPort)` with `Mono<Sucursal> agregar(UUID franquiciaId, String nombre)` and `Mono<Sucursal> renombrar(UUID franquiciaId, UUID sucursalId, String nuevoNombre)` — consumed by `SucursalController` in Task 10. `renombrar` throws `SucursalNotFoundException` if the sucursal exists but belongs to a different franquicia (ownership check).

- [ ] **Step 1: Write the failing unit tests**

```java
package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.SucursalRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SucursalServiceTest {

    @Mock
    private FranquiciaRepositoryPort franquiciaPort;
    @Mock
    private SucursalRepositoryPort sucursalPort;

    @InjectMocks
    private SucursalService service;

    @Test
    void agregaUnaSucursalAUnaFranquiciaExistente() {
        UUID franquiciaId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(franquiciaPort.findById(franquiciaId)).thenReturn(Mono.just(new Franquicia(franquiciaId, "Frutería")));
        when(sucursalPort.save(any())).thenReturn(Mono.just(new Sucursal(sucursalId, franquiciaId, "Sede Norte")));

        StepVerifier.create(service.agregar(franquiciaId, "Sede Norte"))
            .expectNextMatches(s -> s.id().equals(sucursalId) && s.franquiciaId().equals(franquiciaId))
            .verifyComplete();
    }

    @Test
    void falloAlAgregarSucursalAFranquiciaInexistente() {
        UUID franquiciaId = UUID.randomUUID();
        when(franquiciaPort.findById(franquiciaId)).thenReturn(Mono.empty());

        StepVerifier.create(service.agregar(franquiciaId, "Sede Norte"))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }

    @Test
    void falloAlRenombrarSucursalQuePerteneceAOtraFranquicia() {
        UUID franquiciaId = UUID.randomUUID();
        UUID otraFranquiciaId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(sucursalPort.findById(sucursalId))
            .thenReturn(Mono.just(new Sucursal(sucursalId, otraFranquiciaId, "Sede Norte")));

        StepVerifier.create(service.renombrar(franquiciaId, sucursalId, "Nuevo nombre"))
            .expectError(SucursalNotFoundException.class)
            .verify();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=SucursalServiceTest test`
Expected: FAIL (compile error — `SucursalService` doesn't exist yet)

- [ ] **Step 3: Write the implementation**

```java
package com.franquicias.application.service;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.SucursalRepositoryPort;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SucursalService {

    private final FranquiciaRepositoryPort franquiciaPort;
    private final SucursalRepositoryPort sucursalPort;

    public SucursalService(FranquiciaRepositoryPort franquiciaPort, SucursalRepositoryPort sucursalPort) {
        this.franquiciaPort = franquiciaPort;
        this.sucursalPort = sucursalPort;
    }

    public Mono<Sucursal> agregar(UUID franquiciaId, String nombre) {
        return franquiciaPort.findById(franquiciaId)
            .switchIfEmpty(Mono.error(new FranquiciaNotFoundException(franquiciaId)))
            .flatMap(franquicia -> sucursalPort.save(new Sucursal(null, franquiciaId, nombre)));
    }

    public Mono<Sucursal> renombrar(UUID franquiciaId, UUID sucursalId, String nuevoNombre) {
        return sucursalPort.findById(sucursalId)
            .switchIfEmpty(Mono.error(new SucursalNotFoundException(sucursalId)))
            .filter(sucursal -> sucursal.franquiciaId().equals(franquiciaId))
            .switchIfEmpty(Mono.error(new SucursalNotFoundException(sucursalId)))
            .flatMap(sucursal -> sucursalPort.save(new Sucursal(sucursal.id(), sucursal.franquiciaId(), nuevoNombre)));
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=SucursalServiceTest test`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/application/service/SucursalService.java src/test/java/com/franquicias/application/service/SucursalServiceTest.java
git commit -m "feat: add SucursalService with agregar and renombrar use cases"
```

---

### Task 7: ProductoService

**Files:**
- Create: `src/main/java/com/franquicias/application/service/ProductoService.java`
- Test: `src/test/java/com/franquicias/application/service/ProductoServiceTest.java`

**Interfaces:**
- Consumes: `SucursalRepositoryPort`, `ProductoRepositoryPort`, `FranquiciaRepositoryPort`, `SucursalNotFoundException`, `ProductoNotFoundException`, `FranquiciaNotFoundException` (Task 2).
- Produces: `ProductoService(SucursalRepositoryPort sucursalPort, ProductoRepositoryPort productoPort, FranquiciaRepositoryPort franquiciaPort)` with:
  - `Mono<Producto> agregar(UUID franquiciaId, UUID sucursalId, String nombre, int stock)`
  - `Mono<Void> eliminar(UUID franquiciaId, UUID sucursalId, UUID productoId)`
  - `Mono<Producto> modificarStock(UUID franquiciaId, UUID sucursalId, UUID productoId, int nuevoStock)`
  - `Mono<Producto> renombrar(UUID franquiciaId, UUID sucursalId, UUID productoId, String nuevoNombre)`
  - `Flux<ProductoMaxStock> obtenerMaxStockPorFranquicia(UUID franquiciaId)`

  All consumed by `ProductoController` in Tasks 11–12. All path-hierarchy mismatches (sucursal not under franquicia, producto not under sucursal) surface as the corresponding `*NotFoundException`.

- [ ] **Step 1: Write the failing unit tests**

```java
package com.franquicias.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.ProductoNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Producto;
import com.franquicias.domain.model.ProductoMaxStock;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.domain.port.FranquiciaRepositoryPort;
import com.franquicias.domain.port.ProductoRepositoryPort;
import com.franquicias.domain.port.SucursalRepositoryPort;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private SucursalRepositoryPort sucursalPort;
    @Mock
    private ProductoRepositoryPort productoPort;
    @Mock
    private FranquiciaRepositoryPort franquiciaPort;

    @InjectMocks
    private ProductoService service;

    private static final UUID FRANQUICIA_ID = UUID.randomUUID();
    private static final UUID SUCURSAL_ID = UUID.randomUUID();

    @Test
    void agregaUnProductoAUnaSucursalExistente() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.save(any()))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));

        StepVerifier.create(service.agregar(FRANQUICIA_ID, SUCURSAL_ID, "Manzana", 10))
            .expectNextMatches(p -> p.nombre().equals("Manzana") && p.stock() == 10)
            .verifyComplete();
    }

    @Test
    void falloAlAgregarProductoASucursalDeOtraFranquicia() {
        UUID otraFranquiciaId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, otraFranquiciaId, "Sede Norte")));

        StepVerifier.create(service.agregar(FRANQUICIA_ID, SUCURSAL_ID, "Manzana", 10))
            .expectError(SucursalNotFoundException.class)
            .verify();
    }

    @Test
    void eliminaUnProductoExistente() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        when(productoPort.deleteById(productoId)).thenReturn(Mono.empty());

        StepVerifier.create(service.eliminar(FRANQUICIA_ID, SUCURSAL_ID, productoId))
            .verifyComplete();
    }

    @Test
    void modificaElStockDeUnProducto() {
        UUID productoId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 10)));
        when(productoPort.save(any()))
            .thenReturn(Mono.just(new Producto(productoId, SUCURSAL_ID, "Manzana", 50)));

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, 50))
            .expectNextMatches(p -> p.stock() == 50)
            .verifyComplete();
    }

    @Test
    void falloAlModificarStockDeProductoDeOtraSucursal() {
        UUID productoId = UUID.randomUUID();
        UUID otraSucursalId = UUID.randomUUID();
        when(sucursalPort.findById(SUCURSAL_ID))
            .thenReturn(Mono.just(new Sucursal(SUCURSAL_ID, FRANQUICIA_ID, "Sede Norte")));
        when(productoPort.findById(productoId))
            .thenReturn(Mono.just(new Producto(productoId, otraSucursalId, "Manzana", 10)));

        StepVerifier.create(service.modificarStock(FRANQUICIA_ID, SUCURSAL_ID, productoId, 50))
            .expectError(ProductoNotFoundException.class)
            .verify();
    }

    @Test
    void obtieneElProductoConMasStockPorSucursal() {
        when(franquiciaPort.findById(FRANQUICIA_ID))
            .thenReturn(Mono.just(new Franquicia(FRANQUICIA_ID, "Frutería")));
        ProductoMaxStock resultado = new ProductoMaxStock(
            SUCURSAL_ID, "Sede Norte", new Producto(UUID.randomUUID(), SUCURSAL_ID, "Pera", 25));
        when(productoPort.findMaxStockPorFranquicia(FRANQUICIA_ID)).thenReturn(Flux.just(resultado));

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID))
            .expectNext(resultado)
            .verifyComplete();
    }

    @Test
    void falloAlObtenerMaxStockDeFranquiciaInexistente() {
        when(franquiciaPort.findById(FRANQUICIA_ID)).thenReturn(Mono.empty());

        StepVerifier.create(service.obtenerMaxStockPorFranquicia(FRANQUICIA_ID))
            .expectError(FranquiciaNotFoundException.class)
            .verify();
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `mvn -q -Dtest=ProductoServiceTest test`
Expected: FAIL (compile error — `ProductoService` doesn't exist yet)

- [ ] **Step 3: Write the implementation**

```java
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
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `mvn -q -Dtest=ProductoServiceTest test`
Expected: PASS (7 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/application/service/ProductoService.java src/test/java/com/franquicias/application/service/ProductoServiceTest.java
git commit -m "feat: add ProductoService with CRUD and max-stock use cases"
```

---

### Task 8: DTOs and GlobalExceptionHandler

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/NombreRequest.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/CrearProductoRequest.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/ModificarStockRequest.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/FranquiciaResponse.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/SucursalResponse.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/ProductoResponse.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/dto/ProductoMaxStockResponse.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/exception/ErrorResponse.java`
- Create: `src/main/java/com/franquicias/infrastructure/web/exception/GlobalExceptionHandler.java`
- Test: `src/test/java/com/franquicias/infrastructure/web/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `FranquiciaNotFoundException`, `SucursalNotFoundException`, `ProductoNotFoundException` (Task 2), `Franquicia`/`Sucursal`/`Producto`/`ProductoMaxStock` (Task 2).
- Produces: DTOs used as request/response bodies in Tasks 9–12. `FranquiciaResponse(UUID id, String nombre)`, `SucursalResponse(UUID id, UUID franquiciaId, String nombre)`, `ProductoResponse(UUID id, UUID sucursalId, String nombre, int stock)`, `ProductoMaxStockResponse(UUID sucursalId, String sucursalNombre, ProductoResponse producto)`, `NombreRequest(@NotBlank String nombre)` (shared by franquicia/sucursal/producto rename and by "crear sucursal"), `CrearProductoRequest(@NotBlank String nombre, @Min(0) int stock)`, `ModificarStockRequest(@Min(0) int stock)`. `GlobalExceptionHandler` (`@RestControllerAdvice` + `Ordered.HIGHEST_PRECEDENCE`) maps `*NotFoundException` → 404, `WebExchangeBindException`/`ServerWebInputException` → 400, anything else → 500, all as `ErrorResponse`.

- [ ] **Step 1: Write the DTOs**

```java
package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NombreRequest(@NotBlank(message = "nombre no puede estar vacío") String nombre) {}
```

```java
package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CrearProductoRequest(
    @NotBlank(message = "nombre no puede estar vacío") String nombre,
    @Min(value = 0, message = "stock no puede ser negativo") int stock) {}
```

```java
package com.franquicias.infrastructure.web.dto;

import jakarta.validation.constraints.Min;

public record ModificarStockRequest(@Min(value = 0, message = "stock no puede ser negativo") int stock) {}
```

```java
package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Franquicia;
import java.util.UUID;

public record FranquiciaResponse(UUID id, String nombre) {
    public static FranquiciaResponse from(Franquicia f) {
        return new FranquiciaResponse(f.id(), f.nombre());
    }
}
```

```java
package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Sucursal;
import java.util.UUID;

public record SucursalResponse(UUID id, UUID franquiciaId, String nombre) {
    public static SucursalResponse from(Sucursal s) {
        return new SucursalResponse(s.id(), s.franquiciaId(), s.nombre());
    }
}
```

```java
package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.Producto;
import java.util.UUID;

public record ProductoResponse(UUID id, UUID sucursalId, String nombre, int stock) {
    public static ProductoResponse from(Producto p) {
        return new ProductoResponse(p.id(), p.sucursalId(), p.nombre(), p.stock());
    }
}
```

```java
package com.franquicias.infrastructure.web.dto;

import com.franquicias.domain.model.ProductoMaxStock;
import java.util.UUID;

public record ProductoMaxStockResponse(UUID sucursalId, String sucursalNombre, ProductoResponse producto) {
    public static ProductoMaxStockResponse from(ProductoMaxStock m) {
        return new ProductoMaxStockResponse(m.sucursalId(), m.sucursalNombre(), ProductoResponse.from(m.producto()));
    }
}
```

- [ ] **Step 2: Write `ErrorResponse`**

```java
package com.franquicias.infrastructure.web.exception;

import java.time.Instant;

public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path);
    }
}
```

- [ ] **Step 3: Write the failing test for `GlobalExceptionHandler`**

```java
package com.franquicias.infrastructure.web.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapeaFranquiciaNotFoundA404() {
        UUID id = UUID.randomUUID();
        ServerHttpRequest request = MockServerHttpRequest.get("/api/v1/franquicias/" + id).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        Mono<ErrorResponse> result = handler.handleNotFound(new FranquiciaNotFoundException(id), exchange)
            .map(entity -> {
                assertThat(entity.getStatusCode().value()).isEqualTo(404);
                return entity.getBody();
            });

        StepVerifier.create(result)
            .expectNextMatches(body -> body.status() == 404 && body.error().equals("Not Found"))
            .verifyComplete();
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: FAIL (compile error — `GlobalExceptionHandler` doesn't exist yet)

- [ ] **Step 5: Write the implementation**

```java
package com.franquicias.infrastructure.web.exception;

import com.franquicias.domain.exception.FranquiciaNotFoundException;
import com.franquicias.domain.exception.ProductoNotFoundException;
import com.franquicias.domain.exception.SucursalNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    @ExceptionHandler({FranquiciaNotFoundException.class, SucursalNotFoundException.class, ProductoNotFoundException.class})
    public Mono<ResponseEntity<ErrorResponse>> handleNotFound(RuntimeException ex, ServerWebExchange exchange) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), exchange);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleValidation(WebExchangeBindException ex, ServerWebExchange exchange) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Solicitud inválida");
        return build(HttpStatus.BAD_REQUEST, message, exchange);
    }

    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleBadInput(ServerWebInputException ex, ServerWebExchange exchange) {
        return build(HttpStatus.BAD_REQUEST, ex.getReason(), exchange);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex, ServerWebExchange exchange) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno inesperado", exchange);
    }

    private Mono<ResponseEntity<ErrorResponse>> build(HttpStatus status, String message, ServerWebExchange exchange) {
        ErrorResponse body = ErrorResponse.of(
            status.value(), status.getReasonPhrase(), message, exchange.getRequest().getPath().value());
        return Mono.just(ResponseEntity.status(status).body(body));
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `mvn -q -Dtest=GlobalExceptionHandlerTest test`
Expected: PASS

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/web/dto src/main/java/com/franquicias/infrastructure/web/exception src/test/java/com/franquicias/infrastructure/web/exception
git commit -m "feat: add web DTOs and global exception handler"
```

---

### Task 9: FranquiciaController

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/web/controller/FranquiciaController.java`
- Test: `src/test/java/com/franquicias/infrastructure/web/controller/FranquiciaControllerTest.java`

**Interfaces:**
- Consumes: `FranquiciaService` (Task 5), `FranquiciaResponse`, `NombreRequest`, `GlobalExceptionHandler` (Task 8).
- Produces: `POST /api/v1/franquicias`, `PATCH /api/v1/franquicias/{id}`.

- [ ] **Step 1: Write the failing integration test**

```java
package com.franquicias.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.infrastructure.web.dto.FranquiciaResponse;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FranquiciaControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void creaUnaFranquiciaYLaRenombra() {
        FranquiciaResponse creada = webTestClient.post().uri("/api/v1/franquicias")
            .bodyValue(new NombreRequest("Frutería Don Pepe"))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(FranquiciaResponse.class)
            .returnResult().getResponseBody();

        assertThat(creada.id()).isNotNull();
        assertThat(creada.nombre()).isEqualTo("Frutería Don Pepe");

        webTestClient.patch().uri("/api/v1/franquicias/" + creada.id())
            .bodyValue(new NombreRequest("Frutería Don Pepe Renovada"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(FranquiciaResponse.class)
            .value(f -> assertThat(f.nombre()).isEqualTo("Frutería Don Pepe Renovada"));
    }

    @Test
    void retorna404AlRenombrarFranquiciaInexistente() {
        webTestClient.patch().uri("/api/v1/franquicias/" + UUID.randomUUID())
            .bodyValue(new NombreRequest("No importa"))
            .exchange()
            .expectStatus().isNotFound();
    }

    @Test
    void retorna400AlCrearFranquiciaConNombreVacio() {
        webTestClient.post().uri("/api/v1/franquicias")
            .bodyValue(new NombreRequest(""))
            .exchange()
            .expectStatus().isBadRequest();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=FranquiciaControllerTest test`
Expected: FAIL (compile error / 404 — `FranquiciaController` doesn't exist yet)

- [ ] **Step 3: Write the implementation**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.FranquiciaService;
import com.franquicias.infrastructure.web.dto.FranquiciaResponse;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FranquiciaController {

    private final FranquiciaService service;

    public FranquiciaController(FranquiciaService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<FranquiciaResponse> crear(@Valid @RequestBody NombreRequest request) {
        return service.crear(request.nombre()).map(FranquiciaResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{id}")
    public Mono<FranquiciaResponse> renombrar(@PathVariable UUID id, @Valid @RequestBody NombreRequest request) {
        return service.renombrar(id, request.nombre()).map(FranquiciaResponse::from);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=FranquiciaControllerTest test`
Expected: PASS (3 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/web/controller/FranquiciaController.java src/test/java/com/franquicias/infrastructure/web/controller/FranquiciaControllerTest.java
git commit -m "feat: add FranquiciaController with create and rename endpoints"
```

---

### Task 10: SucursalController

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/web/controller/SucursalController.java`
- Test: `src/test/java/com/franquicias/infrastructure/web/controller/SucursalControllerTest.java`

**Interfaces:**
- Consumes: `SucursalService` (Task 6), `FranquiciaService` (Task 5, to set up test fixtures), `SucursalResponse`, `NombreRequest` (Task 8).
- Produces: `POST /api/v1/franquicias/{franquiciaId}/sucursales`, `PATCH /api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}`.

- [ ] **Step 1: Write the failing integration test**

```java
package com.franquicias.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.application.service.FranquiciaService;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.SucursalResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SucursalControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private FranquiciaService franquiciaService;

    @Test
    void agregaUnaSucursalYLaRenombra() {
        Franquicia franquicia = franquiciaService.crear("Frutería Don Pepe").block();

        SucursalResponse creada = webTestClient.post()
            .uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales")
            .bodyValue(new NombreRequest("Sede Norte"))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(SucursalResponse.class)
            .returnResult().getResponseBody();

        assertThat(creada.franquiciaId()).isEqualTo(franquicia.id());

        webTestClient.patch()
            .uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + creada.id())
            .bodyValue(new NombreRequest("Sede Norte Renovada"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(SucursalResponse.class)
            .value(s -> assertThat(s.nombre()).isEqualTo("Sede Norte Renovada"));
    }

    @Test
    void retorna404AlAgregarSucursalAFranquiciaInexistente() {
        webTestClient.post()
            .uri("/api/v1/franquicias/" + java.util.UUID.randomUUID() + "/sucursales")
            .bodyValue(new NombreRequest("Sede Norte"))
            .exchange()
            .expectStatus().isNotFound();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=SucursalControllerTest test`
Expected: FAIL (compile error — `SucursalController` doesn't exist yet)

- [ ] **Step 3: Write the implementation**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.SucursalService;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.SucursalResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class SucursalController {

    private final SucursalService service;

    public SucursalController(SucursalService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias/{franquiciaId}/sucursales")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<SucursalResponse> agregar(@PathVariable UUID franquiciaId, @Valid @RequestBody NombreRequest request) {
        return service.agregar(franquiciaId, request.nombre()).map(SucursalResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}")
    public Mono<SucursalResponse> renombrar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                             @Valid @RequestBody NombreRequest request) {
        return service.renombrar(franquiciaId, sucursalId, request.nombre()).map(SucursalResponse::from);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=SucursalControllerTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/web/controller/SucursalController.java src/test/java/com/franquicias/infrastructure/web/controller/SucursalControllerTest.java
git commit -m "feat: add SucursalController with add and rename endpoints"
```

---

### Task 11: ProductoController — CRUD endpoints

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/web/controller/ProductoController.java`
- Test: `src/test/java/com/franquicias/infrastructure/web/controller/ProductoControllerTest.java`

**Interfaces:**
- Consumes: `ProductoService` (Task 7), `FranquiciaService`, `SucursalService` (fixtures), `ProductoResponse`, `NombreRequest`, `CrearProductoRequest`, `ModificarStockRequest` (Task 8).
- Produces: `POST /api/v1/franquicias/{fId}/sucursales/{sId}/productos`, `DELETE .../productos/{pId}`, `PATCH .../productos/{pId}/stock`, `PATCH .../productos/{pId}` — this task covers all four CRUD endpoints; Task 12 adds the `max-stock` GET endpoint to this same controller class.

- [ ] **Step 1: Write the failing integration test**

```java
package com.franquicias.infrastructure.web.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import com.franquicias.application.service.FranquiciaService;
import com.franquicias.application.service.SucursalService;
import com.franquicias.domain.model.Franquicia;
import com.franquicias.domain.model.Sucursal;
import com.franquicias.infrastructure.web.dto.CrearProductoRequest;
import com.franquicias.infrastructure.web.dto.ModificarStockRequest;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.ProductoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductoControllerTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;
    @Autowired
    private FranquiciaService franquiciaService;
    @Autowired
    private SucursalService sucursalService;

    private Franquicia franquicia;
    private Sucursal sucursal;

    @BeforeEach
    void crearFixtures() {
        franquicia = franquiciaService.crear("Frutería Don Pepe").block();
        sucursal = sucursalService.agregar(franquicia.id(), "Sede Norte").block();
    }

    private String basePath() {
        return "/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + sucursal.id() + "/productos";
    }

    @Test
    void agregaModificaYEliminaUnProducto() {
        ProductoResponse creado = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana", 10))
            .exchange()
            .expectStatus().isCreated()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        assertThat(creado.stock()).isEqualTo(10);

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest(50))
            .exchange()
            .expectStatus().isOk()
            .expectBody(ProductoResponse.class)
            .value(p -> assertThat(p.stock()).isEqualTo(50));

        webTestClient.patch().uri(basePath() + "/" + creado.id())
            .bodyValue(new NombreRequest("Manzana Roja"))
            .exchange()
            .expectStatus().isOk()
            .expectBody(ProductoResponse.class)
            .value(p -> assertThat(p.nombre()).isEqualTo("Manzana Roja"));

        webTestClient.delete().uri(basePath() + "/" + creado.id())
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    void retorna400AlModificarStockANegativo() {
        ProductoResponse creado = webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana", 10))
            .exchange()
            .expectBody(ProductoResponse.class)
            .returnResult().getResponseBody();

        webTestClient.patch().uri(basePath() + "/" + creado.id() + "/stock")
            .bodyValue(new ModificarStockRequest(-5))
            .exchange()
            .expectStatus().isBadRequest();
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductoControllerTest test`
Expected: FAIL (compile error — `ProductoController` doesn't exist yet)

- [ ] **Step 3: Write the implementation**

```java
package com.franquicias.infrastructure.web.controller;

import com.franquicias.application.service.ProductoService;
import com.franquicias.infrastructure.web.dto.CrearProductoRequest;
import com.franquicias.infrastructure.web.dto.ModificarStockRequest;
import com.franquicias.infrastructure.web.dto.NombreRequest;
import com.franquicias.infrastructure.web.dto.ProductoResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductoResponse> agregar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                           @Valid @RequestBody CrearProductoRequest request) {
        return service.agregar(franquiciaId, sucursalId, request.nombre(), request.stock())
            .map(ProductoResponse::from);
    }

    @DeleteMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> eliminar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                @PathVariable UUID productoId) {
        return service.eliminar(franquiciaId, sucursalId, productoId);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}/stock")
    public Mono<ProductoResponse> modificarStock(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                                  @PathVariable UUID productoId,
                                                  @Valid @RequestBody ModificarStockRequest request) {
        return service.modificarStock(franquiciaId, sucursalId, productoId, request.stock())
            .map(ProductoResponse::from);
    }

    @PatchMapping("/api/v1/franquicias/{franquiciaId}/sucursales/{sucursalId}/productos/{productoId}")
    public Mono<ProductoResponse> renombrar(@PathVariable UUID franquiciaId, @PathVariable UUID sucursalId,
                                             @PathVariable UUID productoId,
                                             @Valid @RequestBody NombreRequest request) {
        return service.renombrar(franquiciaId, sucursalId, productoId, request.nombre())
            .map(ProductoResponse::from);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProductoControllerTest test`
Expected: PASS (2 tests)

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/web/controller/ProductoController.java src/test/java/com/franquicias/infrastructure/web/controller/ProductoControllerTest.java
git commit -m "feat: add ProductoController with add, delete, stock, and rename endpoints"
```

---

### Task 12: ProductoController — max-stock endpoint

**Files:**
- Modify: `src/main/java/com/franquicias/infrastructure/web/controller/ProductoController.java`
- Modify: `src/test/java/com/franquicias/infrastructure/web/controller/ProductoControllerTest.java`

**Interfaces:**
- Consumes: `ProductoService.obtenerMaxStockPorFranquicia(UUID)` (Task 7), `ProductoMaxStockResponse` (Task 8).
- Produces: `GET /api/v1/franquicias/{franquiciaId}/productos/max-stock` returning `Flux<ProductoMaxStockResponse>` — this is the acceptance-criteria-7 endpoint (product with most stock per sucursal for a franquicia).

- [ ] **Step 1: Add the failing test to `ProductoControllerTest`**

```java
    @Test
    void retornaElProductoConMasStockPorCadaSucursal() {
        Sucursal sedeSur = sucursalService.agregar(franquicia.id(), "Sede Sur").block();

        webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Manzana", 10))
            .exchange().expectStatus().isCreated();
        webTestClient.post().uri(basePath())
            .bodyValue(new CrearProductoRequest("Pera", 25))
            .exchange().expectStatus().isCreated();
        webTestClient.post()
            .uri("/api/v1/franquicias/" + franquicia.id() + "/sucursales/" + sedeSur.id() + "/productos")
            .bodyValue(new CrearProductoRequest("Uva", 5))
            .exchange().expectStatus().isCreated();

        webTestClient.get().uri("/api/v1/franquicias/" + franquicia.id() + "/productos/max-stock")
            .exchange()
            .expectStatus().isOk()
            .expectBodyList(com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse.class)
            .value(list -> {
                assertThat(list).hasSize(2);
                assertThat(list).extracting(r -> r.producto().nombre())
                    .containsExactlyInAnyOrder("Pera", "Uva");
            });
    }

    @Test
    void retorna404AlPedirMaxStockDeFranquiciaInexistente() {
        webTestClient.get().uri("/api/v1/franquicias/" + java.util.UUID.randomUUID() + "/productos/max-stock")
            .exchange()
            .expectStatus().isNotFound();
    }
```

Add this inside the existing `ProductoControllerTest` class (after the two tests from Task 11).

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=ProductoControllerTest test`
Expected: FAIL (404 — endpoint doesn't exist yet)

- [ ] **Step 3: Add the endpoint to `ProductoController`**

```java
    @org.springframework.web.bind.annotation.GetMapping("/api/v1/franquicias/{franquiciaId}/productos/max-stock")
    public reactor.core.publisher.Flux<com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse> maxStockPorSucursal(
            @PathVariable UUID franquiciaId) {
        return service.obtenerMaxStockPorFranquicia(franquiciaId)
            .map(com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse::from);
    }
```

Add this method inside `ProductoController`, and replace the fully-qualified names with proper imports at the top of the file:

```java
import com.franquicias.infrastructure.web.dto.ProductoMaxStockResponse;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Flux;
```

then use `@GetMapping(...)` and `Flux<ProductoMaxStockResponse>` directly instead of the fully-qualified forms.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=ProductoControllerTest test`
Expected: PASS (4 tests total in this class)

- [ ] **Step 5: Run the full test suite to confirm nothing regressed**

Run: `mvn -q verify`
Expected: BUILD SUCCESS, all tests green

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/web/controller/ProductoController.java src/test/java/com/franquicias/infrastructure/web/controller/ProductoControllerTest.java
git commit -m "feat: add max-stock-per-sucursal endpoint"
```

---

### Task 13: OpenAPI documentation

**Files:**
- Create: `src/main/java/com/franquicias/infrastructure/config/OpenApiConfig.java`
- Test: `src/test/java/com/franquicias/infrastructure/config/OpenApiConfigTest.java`

**Interfaces:**
- Produces: `OpenApiConfig` `@Configuration` bean exposing API metadata; consumed only by springdoc at runtime (`/v3/api-docs`, `/swagger-ui.html`).

- [ ] **Step 1: Write the failing test**

```java
package com.franquicias.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.franquicias.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OpenApiConfigTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void exponeElDocumentoOpenApi() {
        webTestClient.get().uri("/v3/api-docs")
            .exchange()
            .expectStatus().isOk()
            .expectBody(String.class)
            .value(body -> assertThat(body).contains("\"title\":\"Franquicias API\""));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -q -Dtest=OpenApiConfigTest test`
Expected: FAIL (default springdoc title is the artifact name, not "Franquicias API")

- [ ] **Step 3: Write the implementation**

```java
package com.franquicias.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franquiciasOpenApi() {
        return new OpenAPI().info(new Info()
            .title("Franquicias API")
            .version("v1")
            .description("API reactiva para gestionar franquicias, sucursales y productos"));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -q -Dtest=OpenApiConfigTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/franquicias/infrastructure/config/OpenApiConfig.java src/test/java/com/franquicias/infrastructure/config/OpenApiConfigTest.java
git commit -m "feat: add OpenAPI documentation config"
```

---

### Task 14: Docker packaging

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`
- Create: `.env.example`

**Interfaces:**
- Consumes: `pom.xml` build output (`target/franquicias-api-0.1.0.jar`), `application.yml` env vars (`DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `SERVER_PORT` from Task 1).
- Produces: a runnable container image and a one-command local stack (`docker-compose up`). This task has no automated test — verification is manual (documented below) since it exercises Docker itself, which is outside the JVM test harness.

- [ ] **Step 1: Write the multi-stage `Dockerfile`**

```dockerfile
# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /app/target/franquicias-api-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Write `docker-compose.yml`**

```yaml
services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: ${DB_NAME:-franquicias}
      POSTGRES_USER: ${DB_USER:-franquicias}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-franquicias}
    ports:
      - "5432:5432"
    volumes:
      - franquicias_pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-franquicias}"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    build: .
    environment:
      DB_HOST: db
      DB_PORT: 5432
      DB_NAME: ${DB_NAME:-franquicias}
      DB_USER: ${DB_USER:-franquicias}
      DB_PASSWORD: ${DB_PASSWORD:-franquicias}
      SERVER_PORT: 8080
    ports:
      - "8080:8080"
    depends_on:
      db:
        condition: service_healthy

volumes:
  franquicias_pgdata:
```

- [ ] **Step 3: Write `.env.example`**

```
DB_NAME=franquicias
DB_USER=franquicias
DB_PASSWORD=franquicias
```

- [ ] **Step 4: Manually verify the stack builds and serves traffic**

Run: `docker compose up --build -d`
Expected: both containers report healthy/running (`docker compose ps`)

Run: `curl -s -X POST http://localhost:8080/api/v1/franquicias -H "Content-Type: application/json" -d "{\"nombre\":\"Prueba Docker\"}"`
Expected: HTTP 201 with a JSON body containing `"nombre":"Prueba Docker"`

Run: `docker compose down -v`
Expected: containers and volume removed cleanly

- [ ] **Step 5: Commit**

```bash
git add Dockerfile docker-compose.yml .env.example
git commit -m "feat: add Docker packaging and docker-compose local stack"
```

---

### Task 15: GitHub Actions CI

**Files:**
- Create: `.github/workflows/ci.yml`

**Interfaces:**
- Consumes: `pom.xml` (Task 1) — runs `mvn verify`, which requires Docker for Testcontainers (available by default on GitHub-hosted `ubuntu-latest` runners).
- Produces: a CI status check on every push/PR to `main`.

- [ ] **Step 1: Write the workflow**

```yaml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven

      - name: Build and test
        run: mvn -B verify
```

- [ ] **Step 2: Verify locally that the same command the CI runs succeeds**

Run: `mvn -B verify`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit and push, then confirm the Actions tab shows a green run**

```bash
git add .github/workflows/ci.yml
git commit -m "ci: add GitHub Actions build and test workflow"
```

(Push and check the Actions tab happens as part of normal git workflow — do not push without the user's confirmation per repo convention.)

---

### Task 16: Terraform IaC for AWS (RDS + ECR + ECS Fargate)

**Files:**
- Create: `infra/variables.tf`
- Create: `infra/main.tf`
- Create: `infra/outputs.tf`
- Create: `infra/terraform.tfvars.example`

**Interfaces:**
- Consumes: Docker image built in Task 14, pushed to the ECR repo this task creates.
- Produces: `terraform plan`/`apply`-able infrastructure: default-VPC-based RDS PostgreSQL instance, ECR repository, ECS Fargate cluster/service/task definition wired to the RDS endpoint via env vars matching `application.yml`'s `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USER`/`DB_PASSWORD`. No automated test — verified via `terraform validate` and `terraform plan` (both safe, no resources created).

- [ ] **Step 1: Write `variables.tf`**

```hcl
variable "aws_region" {
  description = "Región de AWS donde se despliega la infraestructura"
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Nombre base para etiquetar los recursos"
  type        = string
  default     = "franquicias-api"
}

variable "db_name" {
  description = "Nombre de la base de datos"
  type        = string
  default     = "franquicias"
}

variable "db_username" {
  description = "Usuario de la base de datos"
  type        = string
  default     = "franquicias"
}

variable "db_password" {
  description = "Password de la base de datos (sensible, pasar por tfvars o TF_VAR_db_password)"
  type        = string
  sensitive   = true
}

variable "container_image" {
  description = "URI completa de la imagen en ECR (repo:tag) a desplegar. Vacío en el primer apply antes de hacer push."
  type        = string
  default     = ""
}
```

- [ ] **Step 2: Write `main.tf`**

```hcl
terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

resource "aws_security_group" "db" {
  name        = "${var.project_name}-db-sg"
  description = "Permite trafico Postgres desde el servicio ECS"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.app.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "app" {
  name        = "${var.project_name}-app-sg"
  description = "Permite trafico HTTP entrante al servicio ECS"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    from_port   = 8080
    to_port     = 8080
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_db_subnet_group" "this" {
  name       = "${var.project_name}-db-subnet-group"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "postgres" {
  identifier             = "${var.project_name}-db"
  engine                 = "postgres"
  engine_version         = "16"
  instance_class         = "db.t3.micro"
  allocated_storage      = 20
  db_name                = var.db_name
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.this.name
  vpc_security_group_ids = [aws_security_group.db.id]
  skip_final_snapshot    = true
  publicly_accessible    = false
}

resource "aws_ecr_repository" "app" {
  name                 = var.project_name
  image_tag_mutability = "MUTABLE"
}

resource "aws_ecs_cluster" "this" {
  name = "${var.project_name}-cluster"
}

resource "aws_iam_role" "ecs_execution" {
  name = "${var.project_name}-ecs-execution-role"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action    = "sts:AssumeRole"
      Effect    = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_execution" {
  role       = aws_iam_role.ecs_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_ecs_task_definition" "app" {
  family                   = "${var.project_name}-task"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.ecs_execution.arn

  container_definitions = jsonencode([{
    name  = var.project_name
    image = var.container_image != "" ? var.container_image : "${aws_ecr_repository.app.repository_url}:latest"
    portMappings = [{ containerPort = 8080, protocol = "tcp" }]
    environment = [
      { name = "DB_HOST", value = aws_db_instance.postgres.address },
      { name = "DB_PORT", value = "5432" },
      { name = "DB_NAME", value = var.db_name },
      { name = "DB_USER", value = var.db_username },
      { name = "DB_PASSWORD", value = var.db_password },
    ]
  }])
}

resource "aws_ecs_service" "app" {
  name            = "${var.project_name}-service"
  cluster         = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.app.arn
  desired_count   = 1
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = data.aws_subnets.default.ids
    security_groups  = [aws_security_group.app.id]
    assign_public_ip = true
  }
}
```

- [ ] **Step 3: Write `outputs.tf`**

```hcl
output "ecr_repository_url" {
  value = aws_ecr_repository.app.repository_url
}

output "db_endpoint" {
  value = aws_db_instance.postgres.address
}

output "ecs_cluster_name" {
  value = aws_ecs_cluster.this.name
}
```

- [ ] **Step 4: Write `terraform.tfvars.example`**

```hcl
aws_region      = "us-east-1"
project_name    = "franquicias-api"
db_name         = "franquicias"
db_username     = "franquicias"
db_password     = "CHANGE_ME_use_a_real_secret"
container_image = ""
```

- [ ] **Step 5: Validate the Terraform syntax (no AWS credentials needed for `validate`)**

Run: `cd infra && terraform init -backend=false && terraform validate`
Expected: `Success! The configuration is valid.`

- [ ] **Step 6: Commit**

```bash
git add infra
git commit -m "feat: add Terraform IaC for AWS RDS, ECR, and ECS Fargate"
```

---

### Task 17: README

**Files:**
- Create: `README.md`

**Interfaces:**
- Consumes: nothing programmatically — this is documentation summarizing every prior task's deliverable (endpoints from Tasks 9–12, Docker from Task 14, CI from Task 15, Terraform from Task 16).
- Produces: the entry point a reviewer reads first.

- [ ] **Step 1: Write `README.md`**

```markdown
# Franquicias API

API reactiva (Spring Boot WebFlux) para administrar franquicias, sus
sucursales, y los productos ofertados en cada sucursal.

## Arquitectura

Hexagonal: `domain` (modelos y puertos puros) → `application` (casos de uso)
→ `infrastructure` (controllers WebFlux, adaptadores R2DBC/PostgreSQL).

## Requisitos

- Java 21 (solo si corres fuera de Docker)
- Maven 3.9+ (solo si corres fuera de Docker)
- Docker y Docker Compose

## Correr en local (recomendado: Docker Compose)

```bash
cp .env.example .env
docker compose up --build
```

La API queda disponible en `http://localhost:8080`. Swagger UI:
`http://localhost:8080/swagger-ui.html`.

## Correr en local sin Docker

Requiere una instancia de PostgreSQL corriendo en `localhost:5432` con las
credenciales de `.env.example`.

```bash
mvn spring-boot:run
```

## Tests

```bash
mvn verify
```

Los tests de integración usan Testcontainers (requieren Docker disponible).

## Endpoints principales

| Método | Path | Descripción |
|---|---|---|
| POST | `/api/v1/franquicias` | Crear franquicia |
| PATCH | `/api/v1/franquicias/{id}` | Renombrar franquicia |
| POST | `/api/v1/franquicias/{fId}/sucursales` | Agregar sucursal |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}` | Renombrar sucursal |
| POST | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos` | Agregar producto |
| DELETE | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos/{pId}` | Eliminar producto |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos/{pId}/stock` | Modificar stock |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos/{pId}` | Renombrar producto |
| GET | `/api/v1/franquicias/{fId}/productos/max-stock` | Producto con más stock por sucursal |

Ejemplo:

```bash
curl -X POST http://localhost:8080/api/v1/franquicias \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Frutería Don Pepe"}'
```

## Despliegue en AWS

Infraestructura como código en `infra/` (Terraform): RDS PostgreSQL, ECR,
ECS Fargate.

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars   # editar con tus valores
terraform init
terraform apply
```

Luego construir y publicar la imagen:

```bash
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <ecr_repository_url>
docker build -t <ecr_repository_url>:latest .
docker push <ecr_repository_url>:latest
```

Y actualizar el servicio ECS para usar la nueva imagen (o volver a aplicar
Terraform pasando `container_image` con el tag correspondiente).

## CI

`.github/workflows/ci.yml` corre `mvn verify` en cada push/PR a `main`.
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README with local and AWS deployment instructions"
```

---

## Self-Review Notes

- **Spec coverage:** all 8 acceptance criteria (Spring Boot, crear franquicia, agregar sucursal, agregar producto, eliminar producto, modificar stock, producto con más stock por sucursal, persistencia en nube) are covered by Tasks 1, 9, 10, 11, 11, 11, 12, 16. All six plus items (Docker, reactivo, renombrar franquicia/sucursal/producto, Terraform, despliegue en nube) are covered by Tasks 14, (WebFlux throughout), 9/10/11, 16, 14+16.
- **Placeholder scan:** no TBD/TODO; every step has literal code or an exact `mvn`/`docker`/`terraform`/`curl` command.
- **Type consistency:** `Franquicia`, `Sucursal`, `Producto`, `ProductoMaxStock` signatures introduced in Task 2 are reused verbatim in Tasks 4–12; port method names match adapter and service usages throughout.
