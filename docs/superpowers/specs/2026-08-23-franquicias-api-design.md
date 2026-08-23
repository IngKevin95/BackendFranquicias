# API de Franquicias — Diseño

## Contexto

Prueba técnica: API para gestionar franquicias, cada una con sucursales, cada
sucursal con productos (nombre + stock). Requisitos obligatorios: CRUD parcial
sobre franquicia/sucursal/producto, endpoint de producto con más stock por
sucursal, persistencia en Redis/MySQL/MongoDB/DynamoDB en algún proveedor
cloud, desarrollado en Spring Boot. Se evalúa también flujo de trabajo git y
documentación de despliegue local.

Plus perseguidos: Docker, programación funcional/reactiva, endpoints PATCH de
nombre (franquicia/sucursal/producto), IaC (Terraform), despliegue completo en
la nube.

## Stack

- **Lenguaje/runtime**: Java 21.
- **Build**: Maven.
- **Framework**: Spring Boot 3.x + Spring WebFlux (reactivo end-to-end).
- **Persistencia**: PostgreSQL vía Spring Data R2DBC (reactivo, no bloqueante).
- **Migraciones**: Flyway.
- **Docs API**: springdoc-openapi (Swagger UI en `/swagger-ui.html`).
- **Testing**: JUnit 5 + Mockito (unitarios sobre dominio/servicios) +
  Testcontainers (integración de repositorios/controllers contra Postgres
  real).
- **CI**: GitHub Actions — build + test en cada push/PR a `main`.
- **Contenedor**: Docker multi-stage + docker-compose (app + Postgres) para
  entorno local.
- **IaC**: Terraform — VPC mínima, RDS PostgreSQL, ECR, ECS Fargate, Security
  Groups, ALB opcional.
- **Cloud objetivo**: AWS (ECS Fargate + RDS). El despliegue real requiere
  credenciales del usuario y se documenta en README; no se automatiza el
  `terraform apply` ni el push a ECR dentro del CI del repo público.

## Arquitectura de código (hexagonal)

```
src/main/java/.../franquicias/
├── domain/
│   ├── model/          Franquicia, Sucursal, Producto (POJOs puros)
│   └── port/            FranquiciaRepositoryPort (interfaces)
├── application/
│   └── service/         Casos de uso: CrearFranquicia, AgregarSucursal,
│                         AgregarProducto, EliminarProducto,
│                         ModificarStock, ActualizarNombre*,
│                         ObtenerProductoMaxStockPorSucursal
└── infrastructure/
    ├── web/
    │   ├── controller/   Controllers reactivos (Mono/Flux)
    │   ├── dto/          Request/Response DTOs
    │   └── exception/    GlobalExceptionHandler (@ControllerAdvice reactivo)
    ├── persistence/
    │   ├── entity/       Entities R2DBC (tablas)
    │   ├── repository/   Spring Data R2DBC repositories
    │   ├── mapper/        domain <-> entity
    │   └── adapter/       Implementación de los ports
    └── config/           OpenAPI, Flyway, WebFlux config
```

Reglas: `domain` no depende de Spring ni de infraestructura. `application`
depende solo de `domain` (ports). `infrastructure` implementa los ports y
expone HTTP.

## Modelo de datos (PostgreSQL)

```sql
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
```

Gestionado con Flyway (`V1__init.sql`).

## Endpoints

Base path: `/api/v1`

| Método | Path | Body | Respuesta | Descripción |
|---|---|---|---|---|
| POST | `/franquicias` | `{nombre}` | 201 Franquicia | Crear franquicia |
| PATCH | `/franquicias/{id}` | `{nombre}` | 200 Franquicia | Plus: renombrar franquicia |
| POST | `/franquicias/{id}/sucursales` | `{nombre}` | 201 Sucursal | Agregar sucursal |
| PATCH | `/franquicias/{fId}/sucursales/{sId}` | `{nombre}` | 200 Sucursal | Plus: renombrar sucursal |
| POST | `/franquicias/{fId}/sucursales/{sId}/productos` | `{nombre, stock}` | 201 Producto | Agregar producto |
| DELETE | `/franquicias/{fId}/sucursales/{sId}/productos/{pId}` | — | 204 | Eliminar producto |
| PATCH | `/franquicias/{fId}/sucursales/{sId}/productos/{pId}/stock` | `{stock}` | 200 Producto | Modificar stock |
| PATCH | `/franquicias/{fId}/sucursales/{sId}/productos/{pId}` | `{nombre}` | 200 Producto | Plus: renombrar producto |
| GET | `/franquicias/{id}/productos/max-stock` | — | 200 `[{sucursalId, sucursalNombre, producto}]` | Producto con más stock por sucursal, para la franquicia |

Validación con Bean Validation (`@NotBlank` en nombres, `@Min(0)` en stock).
Errores mapeados vía `GlobalExceptionHandler` a JSON estándar:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "..." }
```

Casos: 404 si franquicia/sucursal/producto no existe, 400 si validación
falla, 409 si aplica conflicto (no se prevé por ahora).

## Docker

- `Dockerfile` multi-stage: stage `build` (maven:3-eclipse-temurin-21) → stage
  `runtime` (eclipse-temurin:21-jre-alpine), usuario no-root, `JAR` copiado.
- `docker-compose.yml`: servicio `app` + servicio `db` (postgres:16-alpine),
  variables de entorno vía `.env` (ejemplo en `.env.example`).

## CI (GitHub Actions)

`.github/workflows/ci.yml`: en push/PR a `main`, `mvn verify` (unit +
integration tests con Testcontainers, requiere Docker disponible en el
runner, que GitHub Actions provee por defecto).

## IaC (Terraform) — `/infra`

Recursos: VPC mínima (o uso de default VPC para simplicidad), subnets, RDS
PostgreSQL (instancia pequeña, `db.t3.micro`), ECR repository, ECS cluster +
Fargate service + task definition (imagen desde ECR, env vars con endpoint de
RDS), Security Groups (solo tráfico necesario), ALB público opcional para
exponer el servicio.

`terraform.tfvars.example` documenta variables requeridas (región, DB
password vía variable sensible, etc.). No se commitea ningún secreto real.

## Documentación (README.md)

- Descripción del proyecto y arquitectura (diagrama simple en texto/mermaid).
- Cómo correr localmente: `docker-compose up`.
- Cómo correr tests: `mvn verify`.
- Cómo desplegar en AWS: `terraform init/plan/apply` + build/push imagen a
  ECR + notas de credenciales.
- Link a Swagger UI.
- Ejemplos curl de cada endpoint.

## Fuera de alcance

- Autenticación/autorización (no pedida en el enunciado).
- CD automático hacia AWS (requiere credenciales del usuario en secrets del
  repo; se documenta el proceso manual).
- Multi-tenancy o soft-delete.
