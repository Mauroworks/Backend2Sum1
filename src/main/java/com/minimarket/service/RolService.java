package com.minimarket.service;

import com.minimarket.entity.Rol;

import java.util.Optional;

public interface RolService {
    Optional<Rol> findByNombre(String nombre);
}
