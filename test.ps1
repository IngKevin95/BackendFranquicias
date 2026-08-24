$ErrorActionPreference = "Stop"

$BASE_URL = "http://localhost:8089/api/v1"

Write-Host "1. Crear Franquicia"
$F_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/franquicias" -ContentType "application/json" -Body '{"nombre": "McDonalds"}'
$F_ID = $F_RES.id
Write-Host "Franquicia ID: $F_ID"

Write-Host "`n2. Crear Sucursal"
$S_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/franquicias/$F_ID/sucursales" -ContentType "application/json" -Body '{"nombre": "Sede Norte"}'
$S_ID = $S_RES.id
Write-Host "Sucursal ID: $S_ID"

Write-Host "`n3. Crear Producto (Nace con Stock 0)"
$P_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos" -ContentType "application/json" -Body '{"nombre": "Hamburguesa"}'
$P_ID = $P_RES.id
Write-Host "Producto ID: $P_ID"

Write-Host "`n4. Kardex ENTRADA"
$ENTRADA_RES = Invoke-RestMethod -Method Patch -Uri "$BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos/$P_ID/stock" -ContentType "application/json" -Body '{"tipo": "ENTRADA", "cantidad": 20}'
$ENTRADA_RES | ConvertTo-Json

Write-Host "`n5. Kardex SALIDA"
$SALIDA_RES = Invoke-RestMethod -Method Patch -Uri "$BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos/$P_ID/stock" -ContentType "application/json" -Body '{"tipo": "SALIDA", "cantidad": 5}'
$SALIDA_RES | ConvertTo-Json

Write-Host "`n6. Max Stock"
$MAX_RES = Invoke-RestMethod -Method Get -Uri "$BASE_URL/franquicias/$F_ID/productos/max-stock?limit=10&offset=0"
$MAX_RES | ConvertTo-Json

Write-Host "`nDone!"
