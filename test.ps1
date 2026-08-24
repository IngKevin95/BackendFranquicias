$ErrorActionPreference = "Stop"

$BASE_URL = "http://localhost:8080/api/v1"

Write-Host "0. Obtener Token JWT"
$AUTH_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/auth/login" -ContentType "application/json" -Body '{"username": "admin", "password": "admin123"}'
$TOKEN = $AUTH_RES.token
Write-Host "Token JWT obtenido con éxito"

$HEADERS = @{
    "Authorization" = "Bearer $TOKEN"
    "Content-Type" = "application/json"
}

Write-Host "`n1. Crear Franquicia"
$F_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/franquicias" -Headers $HEADERS -Body '{"nombre": "McDonalds"}'
$F_ID = $F_RES.id
Write-Host "Franquicia ID: $F_ID"

Write-Host "`n2. Crear Sucursal"
$S_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/franquicias/$F_ID/sucursales" -Headers $HEADERS -Body '{"nombre": "Sede Norte"}'
$S_ID = $S_RES.id
Write-Host "Sucursal ID: $S_ID"

Write-Host "`n3. Crear Producto (Nace con Stock 0)"
$P_RES = Invoke-RestMethod -Method Post -Uri "$BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos" -Headers $HEADERS -Body '{"nombre": "Hamburguesa"}'
$P_ID = $P_RES.id
Write-Host "Producto ID: $P_ID"

Write-Host "`n4. Kardex ENTRADA (Con Idempotency-Key)"
$IDEMP_HEADER = $HEADERS.Clone()
$IDEMP_HEADER.Add("Idempotency-Key", [guid]::NewGuid().ToString())
$ENTRADA_RES = Invoke-RestMethod -Method Patch -Uri "$BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos/$P_ID/stock" -Headers $IDEMP_HEADER -Body '{"tipo": "ENTRADA", "cantidad": 20}'
$ENTRADA_RES | ConvertTo-Json

Write-Host "`n5. Kardex SALIDA"
$SALIDA_RES = Invoke-RestMethod -Method Patch -Uri "$BASE_URL/franquicias/$F_ID/sucursales/$S_ID/productos/$P_ID/stock" -Headers $HEADERS -Body '{"tipo": "SALIDA", "cantidad": 5}'
$SALIDA_RES | ConvertTo-Json

Write-Host "`n6. Max Stock"
$MAX_RES = Invoke-RestMethod -Method Get -Uri "$BASE_URL/franquicias/$F_ID/productos/max-stock?limit=10&offset=0" -Headers $HEADERS
$MAX_RES | ConvertTo-Json

Write-Host "`nDone!"
