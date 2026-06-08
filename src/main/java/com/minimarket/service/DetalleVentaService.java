package com.minimarket.service;

import com.minimarket.entity.DetalleVenta;

import java.util.List;

public interface DetalleVentaService {
    List<DetalleVenta> findAll();
    DetalleVenta findById(Long id);
    DetalleVenta save(DetalleVenta detalleVenta);
    void deleteById(Long id);
    List<DetalleVenta> findByVentaId(Long ventaId);
}
