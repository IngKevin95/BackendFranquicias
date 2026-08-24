package com.franquicias.infrastructure.persistence.repository;

import java.util.UUID;

public interface ProductoMaxStockRow {
    UUID getSucursalId();
    String getSucursalNombre();
    UUID getProductoId();
    String getProductoNombre();
    Integer getStock();
}
