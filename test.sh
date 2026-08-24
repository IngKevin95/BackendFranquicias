#!/bin/bash
set -e

BASE_URL="http://localhost:8089/api/v1"

echo "1. Crear Franquicia"
FRANQUICIA_RES=$(curl -s -X POST $BASE_URL/franquicias \
  -H "Content-Type: application/json" \
  -d '{"nombre": "McDonalds"}')
echo $FRANQUICIA_RES
F_ID=$(echo $FRANQUICIA_RES | jq -r '.id')
echo "Franquicia ID: $F_ID"

echo -e "\n2. Crear Sucursal"
SUCURSAL_RES=$(curl -s -X POST $BASE_URL/franquicias/$F_ID/sucursales \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Sede Norte"}')
echo $SUCURSAL_RES
S_ID=$(echo $SUCURSAL_RES | jq -r '.id')
echo "Sucursal ID: $S_ID"

echo -e "\n3. Crear Producto (Nace con Stock 0)"
PRODUCTO_RES=$(curl -s -X POST $BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Hamburguesa"}')
echo $PRODUCTO_RES
P_ID=$(echo $PRODUCTO_RES | jq -r '.id')
echo "Producto ID: $P_ID"

echo -e "\n4. Kardex ENTRADA"
curl -s -X PATCH $BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos/$P_ID/stock \
  -H "Content-Type: application/json" \
  -d '{"tipo": "ENTRADA", "cantidad": 20}'

echo -e "\n5. Kardex SALIDA"
curl -s -X PATCH $BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos/$P_ID/stock \
  -H "Content-Type: application/json" \
  -d '{"tipo": "SALIDA", "cantidad": 5}'

echo -e "\n6. Max Stock"
curl -s -X GET "$BASE_URL/franquicias/$F_ID/productos/max-stock?limit=10&offset=0"

echo -e "\nDone!"
