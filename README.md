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

## Funcionalidades Avanzadas

- **Seguridad JWT:** Todos los endpoints están protegidos usando tokens asimétricos (JJWT) validados nativamente con Spring Security WebFlux.
- **Idempotencia:** El endpoint de Kardex soporta protección contra reintentos por red mediante el header `Idempotency-Key`, evitando actualización duplicada.
- **Observabilidad:** Integración de Actuator y Micrometer Tracing para exponer métricas a Prometheus e inyectar `[traceId, spanId]` en cada log para seguimiento distribuido.
- **Caché Distribuida:** Uso de Redis Reactivo para cachear la respuesta pesada de `max-stock`. La caché se invalida automáticamente si ocurre una modificación de inventario.
- **Kardex y Atomicidad:** Las modificaciones de stock generan un registro histórico inmutable (ENTRADA/SALIDA) y las actualizaciones de inventario se resuelven nativamente en la base de datos (previniendo *race conditions* concurrentes).
- **Paginación:** Endpoint de reportes soporta `limit` y `offset`.
- **Swagger Rica:** Todos los controladores están decorados con `@Operation`, `@Tag` y requisitos de Auth Bearer para una exploración interactiva en `/swagger-ui.html`.

## Endpoints principales

| Método | Path | Descripción |
|---|---|---|
| POST | `/api/v1/auth/login` | **NUEVO:** Obtener Token JWT usando `{"username":"admin", "password":"admin123"}` |
| POST | `/api/v1/franquicias` | Crear franquicia |
| PATCH | `/api/v1/franquicias/{id}` | Renombrar franquicia |
| POST | `/api/v1/franquicias/{fId}/sucursales` | Agregar sucursal |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}` | Renombrar sucursal |
| POST | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos` | Agregar producto |
| DELETE | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos/{pId}` | Eliminar producto |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos/{pId}/stock` | Modificar stock (recibe JSON con `tipo` ENTRADA/SALIDA y `cantidad`, acepta header `Idempotency-Key`) |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos/{pId}` | Renombrar producto |
| GET | `/api/v1/franquicias/{fId}/productos/max-stock?limit=10&offset=0` | Producto con más stock por sucursal (paginado) |

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
