package com.minimarket.service;

import com.minimarket.entity.Inventario;

import java.util.List;

public interface InventarioService {
    List<Inventario> findAll();
    Inventario findById(Long id);
    Inventario save(Inventario inventario);
    void deleteById(Long id);
    List<Inventario> findByProductoId(Long productoId);
}
