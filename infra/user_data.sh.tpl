#!/bin/bash
set -euxo pipefail

# swap: t2.micro/t3.micro solo tienen 1GB RAM, la build de Maven lo necesita
fallocate -l 1G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile none swap sw 0 0' >> /etc/fstab

dnf install -y docker git
systemctl enable --now docker

git clone --branch ${repo_branch} --depth 1 ${repo_url} /opt/app
cd /opt/app

docker build -t franquicias-api:latest .

docker run -d --name redis --restart unless-stopped --network host redis:7-alpine

# RDS exige SSL (rds.force_ssl=1 por defecto), por eso sslMode=require en la URL
docker run -d --name app --restart unless-stopped \
  --network host \
  -e SPRING_R2DBC_URL="r2dbc:postgresql://${db_host}:5432/${db_name}?sslMode=require" \
  -e SPRING_FLYWAY_URL="jdbc:postgresql://${db_host}:5432/${db_name}?ssl=true&sslmode=require" \
  -e DB_USER=${db_user} \
  -e DB_PASSWORD=${db_password} \
  -e REDIS_HOST=127.0.0.1 \
  -e REDIS_PORT=6379 \
  -e SERVER_PORT=8080 \
  franquicias-api:latest
