# Quickstart

Guía rápida de arranque: local con y sin Docker, despliegue en AWS desde
cero, redeploy de una instancia existente, y problemas comunes. Para el
detalle de arquitectura, endpoints y checklist de la prueba técnica, ver
[README.md](README.md).

---

## 1. Local con Docker Compose (recomendado)

```bash
git clone https://github.com/IngKevin95/BackendFranquicias.git
cd BackendFranquicias
cp .env.example .env
# editar .env: poner ADMIN_USERNAME / ADMIN_PASSWORD / ADMIN_EMAIL propios
# (son obligatorios, docker compose up falla si faltan)
docker compose up --build
```

- API: `http://localhost:8089`
- Swagger UI: `http://localhost:8089/swagger-ui.html`
- Health: `http://localhost:8089/actuator/health`

Al primer arranque contra una base vacía, la app siembra un usuario `ADMIN`
con las credenciales de `ADMIN_USERNAME`/`ADMIN_PASSWORD`/`ADMIN_EMAIL` que
hayas puesto en `.env` — no hay default en el código. Login:

```bash
curl -X POST http://localhost:8089/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"<lo-que-pusiste-en-ADMIN_USERNAME>","password":"<lo-que-pusiste-en-ADMIN_PASSWORD>"}'
```

Parar todo: `docker compose down`. Parar y borrar los datos de Postgres:
`docker compose down -v`.

> **Siempre que vuelvas a buildear código y ya tenías el stack levantado,
> usá `docker compose up -d --build`** — sin `--build`, Compose reutiliza la
> imagen vieja cacheada y tus cambios no van a aparecer.

## 2. Local sin Docker (JDK + Maven nativos)

Levantá solo la base de datos y Redis con Docker, y corré la app con Maven:

```bash
docker compose up -d db redis
DB_PORT=5433 ADMIN_USERNAME=admin ADMIN_PASSWORD=<tu-password> ADMIN_EMAIL=admin@example.com \
  mvn spring-boot:run   # docker-compose mapea Postgres a 5433 en el host
```

> `DB_PORT=5433` es necesario porque `docker-compose.yml` expone Postgres en
> `5433:5432` para no chocar con una instalación local de Postgres en el
> puerto default. Si corrés Postgres de otra forma, ajustá `DB_HOST`/`DB_PORT`
> según corresponda (ver `application.yml`).

Requiere JDK 21 y Maven 3.9+ instalados. Instalación rápida en Windows sin
paquetes preempaquetados de Maven:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK -e
# Maven no tiene paquete winget: descargar el binario oficial y agregarlo al PATH
# https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip
```

## 3. Tests

```bash
mvn test
```

64 tests: unitarios (Mockito + `StepVerifier`, sin dependencias externas) e
integración (Testcontainers, levantan Postgres y Redis reales — requieren
Docker corriendo). `mvn verify` corre lo mismo y además el `package`.

---

## 4. Desplegar en AWS desde cero (Terraform)

Requisitos: cuenta AWS, [Terraform](https://developer.hashicorp.com/terraform/install)
instalado, credenciales AWS configuradas (`aws configure` o variables
`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`), y un par de claves SSH
(`ssh-keygen -t ed25519` si no tenés uno).

```bash
cd infra
cp terraform.tfvars.example terraform.tfvars
```

Editar `terraform.tfvars`:

| Variable | Qué poner |
|---|---|
| `db_password` | una password real, no el placeholder |
| `ssh_public_key` | el contenido de tu `~/.ssh/id_ed25519.pub` (o similar) |
| `repo_branch` | rama a desplegar (default `develop`) |
| `ssh_cidr` | tu IP con `/32` en vez de `0.0.0.0/0`, si esto no es solo una prueba descartable |

```bash
terraform init
terraform plan    # revisar qué se va a crear
terraform apply   # confirmar con "yes"
```

Recursos creados (todo dentro del free tier de una cuenta AWS nueva):

| Recurso | Tipo |
|---|---|
| EC2 | `t3.micro` — corre la app dockerizada + Redis, mismo host |
| RDS | `db.t3.micro` PostgreSQL 16 |
| Elastic IP | fija mientras la instancia EC2 exista |
| Security Groups | app (22 SSH, 8080 HTTP) y db (5432 solo desde el SG de la app) |

Al terminar `terraform apply`:

```bash
terraform output
# app_url, app_public_ip, db_endpoint, ssh_command
```

La instancia se autoconfigura sola vía `infra/user_data.sh.tpl`: instala
Docker, clona el repo en la rama indicada, buildea la imagen y levanta los
contenedores `app` y `redis`. Puede tardar 2-3 minutos desde `apply` hasta
que `app_url` responda.

Apagar todo (para no seguir consumiendo horas free tier):

```bash
terraform destroy
```

> `terraform.tfvars` y `terraform.tfstate` **no están commiteados** — viven
> solo local. Si perdés el `.tfstate`, Terraform deja de saber qué existe en
> AWS; hay que importar los recursos a mano o borrarlos desde la consola.

## 5. Redeploy de una instancia ya desplegada

### Opción A — GitHub Actions (recomendado, no requiere SSH)

*Actions* → *CI* → **Run workflow** → elegir la rama del dropdown → *Run
workflow*. Buildea, testea, y si todo pasa, deploya esa rama a la instancia
configurada en los secrets del repo (`EC2_HOST`, `EC2_SSH_KEY`).

También corre automáticamente el deploy en cada PR hacia `main`.

### Opción B — SSH manual

```bash
ssh -i <tu-clave-privada> ec2-user@<ip-de-la-instancia>
sudo bash /opt/app/infra/deploy.sh [rama]
```

Sin argumento, actualiza la rama que ya tenía checkeada (`git pull`). Con
argumento, hace `checkout` de esa rama primero — funciona aunque esa rama
nunca se haya deployado antes en esa instancia.

`deploy.sh` reutiliza las variables de entorno (DB, Redis) del contenedor
`app` anterior vía `docker inspect`, así que no hace falta volver a pasar
credenciales a mano en cada redeploy.

Ver más detalle operativo (logs, apagar todo, notas de free tier) en
[`docs/cloud.md`](docs/cloud.md).

---

## 6. Problemas comunes

| Síntoma | Causa | Solución |
|---|---|---|
| Swagger no muestra cambios recientes / falta un endpoint nuevo | `docker compose up` sin `--build` reutilizó la imagen vieja | `docker compose down && docker compose up -d --build` |
| `mvn spring-boot:run` falla con `FlywayValidateException: Migration checksum mismatch` | El volumen de Postgres local quedó con un checksum viejo de una migración que después cambió | `docker compose down -v` (borra el volumen) y volver a levantar `db`/`redis` |
| Deploy vía CI falla con `pathspec '<rama>' did not match any file(s)` | El servidor nunca hizo checkout de esa rama antes (bug ya corregido en `infra/deploy.sh`, pero si reaparece en un servidor nuevo/reseteado) | Disparar `workflow_dispatch` una vez sobre una rama que el servidor sí conozca (ej. `main`) para que el script se actualice, y recién ahí desplegar la rama nueva |
| `mvn`/`java` "command not found" | JDK/Maven no están en el `PATH` | Verificar `JAVA_HOME`/`MAVEN_HOME` y que sus `bin/` estén en el `PATH` de la sesión actual |
