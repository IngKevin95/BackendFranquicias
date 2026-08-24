#!/bin/bash
# Redeploy manual sin recrear la instancia: git pull + rebuild + restart del contenedor app.
# Correr DESDE la EC2 (ssh ec2-user@<ip>, luego: sudo /opt/app/infra/deploy.sh)
set -euo pipefail
cd /opt/app

git pull

docker build -t franquicias-api:latest .

# ponytail: reusa las env vars del contenedor viejo via docker inspect en vez de
# volver a pasar DB_PASSWORD etc a mano. Si cambian las credenciales, recrear el
# contenedor una vez con el comando docker run completo (ver docs/cloud.md).
ENV_ARGS=$(docker inspect app --format '{{range .Config.Env}}-e {{.}} {{end}}')

docker rm -f app
docker run -d --name app --restart unless-stopped --network host $ENV_ARGS franquicias-api:latest

echo "Deploy listo. Esperando a que la app levante..."
for i in $(seq 1 24); do
  HEALTH=$(curl -s --max-time 3 http://localhost:8080/actuator/health || true)
  if echo "$HEALTH" | grep -q '"status":"UP"'; then
    echo "$HEALTH"
    exit 0
  fi
  sleep 5
done
echo "La app no respondio UP a tiempo. Ultimo intento: $HEALTH" >&2
exit 1
