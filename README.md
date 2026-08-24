# Franquicias API

API reactiva (Spring Boot WebFlux) para administrar franquicias, sus
sucursales, y los productos ofertados en cada sucursal — con autenticación
JWT por roles, kardex de inventario, consultas filtradas y despliegue
automatizado en AWS.

> **¿Querés arrancar rápido?** Ver [QUICKSTART.md](QUICKSTART.md) para correr
> el proyecto en local o desplegarlo en AWS paso a paso, incluyendo
> problemas comunes y sus soluciones.

## Arquitectura

Hexagonal: `domain` (modelos y puertos puros) → `application` (casos de uso)
→ `infrastructure` (controllers WebFlux, adaptadores R2DBC/PostgreSQL, Redis,
seguridad, OpenAPI).

```
domain/            modelos (Franquicia, Sucursal, Producto, TransaccionStock,
                    Usuario), excepciones de negocio, puertos (interfaces)
application/        servicios de casos de uso (orquestan puertos)
infrastructure/
  web/controller/   endpoints REST (WebFlux, reactivo)
  web/dto/          request/response
  web/exception/    manejador global de errores
  persistence/       entidades R2DBC, mappers, adaptadores, repos
  config/security/   JWT, filtros, reglas de autorización por rol
  config/            OpenAPI, Redis
```

## Requisitos

- Java 21 (solo si corres fuera de Docker)
- Maven 3.9+ (solo si corres fuera de Docker)
- Docker y Docker Compose

## Correr en local (recomendado: Docker Compose)

```bash
cp .env.example .env
docker compose up --build
```

La API queda disponible en `http://localhost:8089` (ver `docker-compose.yml`
para el mapeo de puertos). Swagger UI: `http://localhost:8089/swagger-ui.html`.

> Si ya habías levantado el stack antes y volviste a buildear el código,
> acordate de pasar `--build` — sin eso, `docker compose up` reutiliza la
> imagen vieja cacheada y no vas a ver tus cambios.

## Correr en local sin Docker

Requiere una instancia de PostgreSQL y Redis corriendo (podés levantar solo
esos dos servicios con `docker compose up -d db redis`) y JDK 21 + Maven
instalados.

```bash
mvn spring-boot:run
```

## Tests

```bash
mvn test
```

64 tests (unitarios con Mockito/StepVerifier + integración con Testcontainers
levantando PostgreSQL y Redis reales). Los tests de integración requieren
Docker disponible.

## Usuario admin de arranque

La migración `V4__usuarios.sql` siembra un usuario `admin` (password
`admin123`, rol `ADMIN`). Usalo para obtener el primer token vía
`POST /api/v1/auth/login` y desde ahí crear el resto de usuarios con el rol
que corresponda.

> Credencial de arranque para desarrollo local únicamente — cambiala o
> deshabilitala antes de exponer una instancia a producción real.

---

## Checklist de la prueba técnica

### Criterios de aceptación (obligatorios)

| # | Requisito | Estado |
|---|---|---|
| 1 | Desarrollado en Spring Boot | ✅ |
| 2 | Endpoint para agregar franquicia | ✅ `POST /api/v1/franquicias` |
| 3 | Endpoint para agregar sucursal a una franquicia | ✅ `POST /api/v1/franquicias/{id}/sucursales` |
| 4 | Endpoint para agregar producto a una sucursal | ✅ `POST /api/v1/franquicias/{id}/sucursales/{id}/productos` |
| 5 | Endpoint para eliminar producto de una sucursal | ✅ `DELETE .../productos/{id}` |
| 6 | Endpoint para modificar stock de un producto | ✅ `PATCH .../productos/{id}/stock` |
| 7 | Endpoint que retorna el producto con más stock por sucursal de una franquicia | ✅ `GET /api/v1/franquicias/{id}/productos/max-stock` (paginado y cacheado en Redis) |
| 8 | Persistencia en Redis/MySQL/MongoDB/DynamoDB en un proveedor cloud | ✅ PostgreSQL (RDS) + Redis, desplegados en AWS |

### Puntos extra pedidos por la prueba

| Plus | Estado |
|---|---|
| Empaquetado con Docker | ✅ `Dockerfile` multi-stage + `docker-compose.yml` |
| Programación funcional/reactiva | ✅ Spring WebFlux + R2DBC + Reactor de punta a punta, sin bloqueos |
| Endpoint para renombrar franquicia | ✅ `PATCH /api/v1/franquicias/{id}` |
| Endpoint para renombrar sucursal | ✅ `PATCH .../sucursales/{id}` |
| Endpoint para renombrar producto | ✅ `PATCH .../productos/{id}` |
| Persistencia provisionada con IaC | ✅ Terraform (`infra/`): EC2 + RDS PostgreSQL |
| Solución desplegada en la nube | ✅ AWS (EC2 free tier corriendo la app dockerizada + Redis, RDS PostgreSQL) |
| Flujo de trabajo con git en repo público | ✅ GitHub, GitFlow (`main`/`develop`/`feature`/`release`/`fix`), PRs con merge commit, ver [Flujo de trabajo](#flujo-de-trabajo-git-gitflow) |
| README con instrucciones de despliegue local | ✅ este documento |

### Extras adicionales (no pedidos por la prueba, agregados sobre la base)

Estos no estaban en los criterios ni en los puntos extra originales — se
sumaron para llevar el proyecto a un estado más cercano a producción real:

- **Autenticación y autorización JWT con roles** (`ADMIN`, `WRITE`, `READ`):
  todos los endpoints requieren Bearer token; las escrituras (POST/PATCH/DELETE)
  exigen `WRITE` o `ADMIN`, las lecturas (GET) cualquier rol autenticado, y la
  gestión de usuarios (`POST`/`GET /api/v1/usuarios`) es exclusiva de `ADMIN`.
- **Gestión de usuarios**: alta y listado de usuarios del sistema, passwords
  con BCrypt, tokens firmados con JJWT.
- **Consultas GET filtradas de todo el modelo**, para que un usuario sin
  acceso directo a la base de datos pueda ver toda la información vía API:
  - `GET /api/v1/franquicias` y `GET /api/v1/franquicias/{id}` — listar/obtener franquicias.
  - `GET /api/v1/franquicias/{id}/sucursales` — sucursales de una franquicia.
  - `GET .../sucursales/{id}/productos` — productos de una sucursal con su stock.
- **Kardex de inventario con filtro por rango de fechas**:
  `GET .../productos/{id}/kardex?desde=&hasta=` retorna el historial completo
  de transacciones de stock (ENTRADA/SALIDA) de un producto, filtrable por
  fecha ISO-8601.
- **Idempotencia**: el endpoint de modificación de stock acepta un header
  `Idempotency-Key` para evitar duplicar una transacción ante reintentos de
  red (constraint único a nivel de base de datos).
- **Atomicidad del stock**: las actualizaciones de inventario se resuelven
  con una query nativa (`UPDATE ... WHERE stock + :cambio >= 0`), evitando
  *race conditions* bajo concurrencia sin necesidad de locks explícitos.
- **Caché distribuida con Redis** para la consulta de producto con más stock
  por sucursal, invalidada automáticamente ante cualquier modificación de
  inventario, alta/baja/renombrado de producto.
- **Paginación** (`limit`/`offset`) en el endpoint de max-stock.
- **Observabilidad**: Spring Boot Actuator + Micrometer Tracing, con
  `traceId`/`spanId` inyectados en cada línea de log para trazabilidad
  distribuida, y métricas expuestas para Prometheus.
- **OpenAPI/Swagger completo**: cada controller documentado con `@Tag` y
  `@Operation`, esquema de seguridad `bearerAuth` declarado globalmente,
  botón "Authorize" funcional en `/swagger-ui.html`.
- **CI/CD con GitHub Actions**: build + 64 tests (con Testcontainers) en cada
  PR hacia `main`/`develop`, y deploy automático por SSH a la instancia EC2
  cuando el PR es hacia `main`. Además, un trigger manual
  (`workflow_dispatch`) permite redeployar cualquier rama a demanda desde la
  pestaña Actions de GitHub, sin necesidad de abrir un PR.
- **Script de redeploy sin downtime de infraestructura** (`infra/deploy.sh`):
  hace `git checkout` de la rama pedida, rebuildea la imagen Docker y
  reinicia el contenedor `app` reutilizando las variables de entorno del
  contenedor anterior (vía `docker inspect`), sin necesidad de recrear la
  instancia EC2 ni pasar credenciales a mano.
- **GitFlow disciplinado**: toda la historia del proyecto entra por rama +
  PR + merge commit (nunca commits directos a `main`/`develop`, nunca
  squash/rebase), incluyendo una rama de release (`release/0.1.1`) para el
  primer despliegue consolidado a producción.

---

## Endpoints

Todos requieren `Authorization: Bearer <token>` salvo `/api/v1/auth/login`.

### Autenticación

| Método | Path | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/api/v1/auth/login` | público | Login con `{"username":"...","password":"..."}`, retorna JWT con el rol en el claim `role` |

### Usuarios (solo ADMIN)

| Método | Path | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/api/v1/usuarios` | ADMIN | Crear usuario (`username`, `password`, `email`, `role`) |
| GET | `/api/v1/usuarios` | ADMIN | Listar todos los usuarios del sistema |

### Franquicias

| Método | Path | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/api/v1/franquicias` | WRITE, ADMIN | Crear franquicia |
| GET | `/api/v1/franquicias` | cualquiera autenticado | Listar todas las franquicias |
| GET | `/api/v1/franquicias/{id}` | cualquiera autenticado | Obtener una franquicia |
| PATCH | `/api/v1/franquicias/{id}` | WRITE, ADMIN | Renombrar franquicia |

### Sucursales

| Método | Path | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/api/v1/franquicias/{fId}/sucursales` | WRITE, ADMIN | Agregar sucursal |
| GET | `/api/v1/franquicias/{fId}/sucursales` | cualquiera autenticado | Listar sucursales de una franquicia |
| PATCH | `/api/v1/franquicias/{fId}/sucursales/{sId}` | WRITE, ADMIN | Renombrar sucursal |

### Productos y Kardex

| Método | Path | Rol requerido | Descripción |
|---|---|---|---|
| POST | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos` | WRITE, ADMIN | Agregar producto (stock inicial 0) |
| GET | `/api/v1/franquicias/{fId}/sucursales/{sId}/productos` | cualquiera autenticado | Listar productos de una sucursal con su stock |
| DELETE | `.../productos/{pId}` | WRITE, ADMIN | Eliminar producto (cascada sobre su kardex) |
| PATCH | `.../productos/{pId}` | WRITE, ADMIN | Renombrar producto |
| PATCH | `.../productos/{pId}/stock` | WRITE, ADMIN | Transacción de stock (`{"tipo":"ENTRADA\|SALIDA","cantidad":N}`), acepta header `Idempotency-Key` |
| GET | `.../productos/{pId}/kardex?desde=&hasta=` | cualquiera autenticado | Historial de transacciones de stock, filtrable por rango de fechas ISO-8601 |
| GET | `/api/v1/franquicias/{fId}/productos/max-stock?limit=10&offset=0` | cualquiera autenticado | Producto con más stock por cada sucursal de la franquicia (paginado, cacheado en Redis) |

### Ejemplo de flujo completo

```bash
# 1. Login
TOKEN=$(curl -s -X POST http://localhost:8089/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .token)

# 2. Crear franquicia
curl -X POST http://localhost:8089/api/v1/franquicias \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Frutería Don Pepe"}'

# 3. Listar franquicias
curl http://localhost:8089/api/v1/franquicias -H "Authorization: Bearer $TOKEN"
```

Más ejemplos completos en `api-tests.http`.

---

## Despliegue en AWS

Infraestructura como código en `infra/` (Terraform), pensada para caber en el
free tier de AWS: EC2 (t3.micro, corre la app dockerizada + Redis) y RDS
PostgreSQL (db.t3.micro). Sin ALB, ECS ni ECR.

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars   # editar con tus valores (password DB, clave SSH pública)
terraform init
terraform plan
terraform apply
```

`terraform apply` deja la instancia lista sola: el `user_data.sh.tpl` instala
Docker, clona el repo, buildea la imagen y levanta los contenedores `app` y
`redis`. Al terminar, `terraform output` muestra la IP pública y la URL de la
API.

### Redeploy manual (sin recrear la instancia)

Por SSH:

```bash
ssh -i <tu-clave> ec2-user@<ip-de-la-instancia>
sudo bash /opt/app/infra/deploy.sh [rama]
```

Sin argumento, actualiza la rama actual (`git pull`). Con argumento, hace
`checkout` de esa rama antes de rebuildear — soporta ramas que nunca se
hayan deployado antes en esa instancia.

### Redeploy desde GitHub Actions (recomendado)

`.github/workflows/ci.yml` corre `mvn verify` y, según el trigger:

- **PR hacia `main`**: build + test + deploy automático a EC2 vía SSH.
- **`workflow_dispatch`** (manual, pestaña *Actions* → *CI* → *Run workflow*):
  elegís cualquier rama del dropdown y se buildea + deploya esa rama a
  demanda, sin necesidad de abrir un PR.

Más detalle operativo (SSH, logs, apagar todo con `terraform destroy`) en
`docs/cloud.md`.

---

## Flujo de trabajo Git (GitFlow)

- `main`: producción. Solo recibe merges vía PR desde ramas `release/*` (o
  `hotfix/*`), siempre con merge commit (`--no-ff`), nunca squash/rebase.
- `develop`: integración. Recibe merges vía PR desde `feature/*` y `fix/*`.
- `feature/*`, `fix/*`, `chore/*`: ramas de trabajo cortas, una por cambio,
  con commits granulares.
- `release/x.y.z`: se corta desde `develop` cuando hay contenido listo para
  producción, bump de versión en `pom.xml`, PR hacia `main`. Tras mergear,
  se mergea `main` de vuelta a `develop` para reunificar el historial.

Cada PR corre el pipeline de CI (build + 64 tests) antes de poder mergearse.

## CI

`.github/workflows/ci.yml`:
- `mvn -B verify` en cada PR hacia `main` o `develop`.
- Deploy automático a EC2 en PRs hacia `main`.
- `workflow_dispatch` para deploys manuales de cualquier rama.
