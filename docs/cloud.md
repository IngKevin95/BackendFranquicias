# Despliegue AWS (free tier)

## Acceso rápido

| | |
|---|---|
| API    | http://52.4.108.93:8080 |
| Swagger | http://52.4.108.93:8080/swagger-ui.html |
| Health | http://52.4.108.93:8080/actuator/health |
| SSH    | `ssh -i ~/.ssh/id_rsa ec2-user@52.4.108.93` |
| DB     | franquicias-api-db.cezeaao661s0.us-east-1.rds.amazonaws.com:5432 (SSL requerido) |

## Cuenta y región

- Cuenta AWS: `196985821127`
- Usuario CLI/consola: `KevinTest` (con `AdministratorAccess`)
- Consola: https://196985821127.signin.aws.amazon.com/console
- Región: `us-east-1` (N. Virginia)

## Recursos desplegados

| Recurso | Tipo | Nombre/ID |
|---|---|---|
| EC2 | t3.micro | `franquicias-api-app` (`i-0946734bb62b06195`) — app dockerizada (`java -jar` en contenedor) + Redis, mismo host |
| RDS | db.t3.micro Postgres 16 | `franquicias-api-db` |
| Elastic IP | — | `52.4.108.93` (fija mientras la EC2 exista) |
| Security Group app | 22 (SSH), 8080 (HTTP) | `franquicias-api-app-sg` |
| Security Group db | 5432 solo desde el SG de la app | `franquicias-api-db-sg` |
| Key pair | SSH | `franquicias-api-key` (clave pública en `infra/terraform.tfvars`) |

Todo provisionado con Terraform en `infra/` (ver PR #22, rama `feature/aws-terraform-free-tier`).

## Cómo ver los recursos en la consola web

1. Entrar con el usuario `KevinTest` en la URL de arriba.
2. Confirmar región **N. Virginia (us-east-1)** en el selector superior derecho.
3. EC2: buscador → "EC2" → **Instances** → `franquicias-api-app`.
4. RDS: buscador → "RDS" → **Databases** → `franquicias-api-db`.

## Operar la instancia

```bash
# entrar
ssh -i ~/.ssh/id_rsa ec2-user@52.4.108.93

# ver logs de la app
sudo docker logs app --tail 50 -f

# ver logs de redis
sudo docker logs redis --tail 50

# reiniciar la app (por ejemplo tras cambiar código)
cd /opt/app
sudo git pull
sudo docker build -t franquicias-api:latest .
sudo docker rm -f app
sudo docker run -d --name app --restart unless-stopped --network host \
  -e SPRING_R2DBC_URL='r2dbc:postgresql://franquicias-api-db.cezeaao661s0.us-east-1.rds.amazonaws.com:5432/franquicias?sslMode=require' \
  -e SPRING_FLYWAY_URL='jdbc:postgresql://franquicias-api-db.cezeaao661s0.us-east-1.rds.amazonaws.com:5432/franquicias?ssl=true&sslmode=require' \
  -e DB_USER=franquicias -e DB_PASSWORD='<ver terraform.tfvars>' \
  -e REDIS_HOST=127.0.0.1 -e REDIS_PORT=6379 -e SERVER_PORT=8080 \
  franquicias-api:latest
```

## Gestión con Terraform

```bash
cd infra
terraform init
terraform plan    # revisar cambios antes de aplicar
terraform apply   # aplicar
terraform destroy # apagar todo cuando no se necesite (evita seguir consumiendo horas free tier)
```

`infra/terraform.tfvars` (con la password de DB y la clave SSH) **no está commiteado** — vive solo local. `infra/terraform.tfstate` tampoco — es el registro de lo realmente desplegado; si se pierde, Terraform ya no sabe qué existe en AWS y hay que importar los recursos a mano o borrarlos manualmente desde la consola.

## Notas importantes

- **Free tier**: 750h/mes de EC2 + 750h/mes de RDS durante los primeros 12 meses de la cuenta. Dejar esto prendido 24/7 un mes cabe en el límite, siempre que no haya otras instancias EC2/RDS corriendo en la cuenta.
- **`user_data.sh.tpl`** solo corre una vez, al crear la instancia. Si se recrea la EC2, va a re-clonar `develop` y levantar todo de cero automáticamente (incluye el fix de SSL para RDS).
- **Password de Spring Security generada al boot**: en los logs de arranque aparece un `Using generated security password: <uuid>` — es el default de Spring Boot Security, no afecta si el proyecto usa JWT propio en los endpoints reales.
- **`ssh_cidr`** en `terraform.tfvars` está en `0.0.0.0/0` (abierto a todo internet) — restringir a la IP propia (`x.x.x.x/32`) si esto deja de ser solo una prueba.
- Para apagar todo: `terraform destroy` desde `infra/`.
