package com.minimarket.config;

import com.minimarket.entity.Rol;
import com.minimarket.entity.Usuario;
import com.minimarket.repository.RolRepository;
import com.minimarket.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (rolRepository.count() > 0) return;

        Rol rolCliente = new Rol();
        rolCliente.setNombre("ROLE_CLIENTE");
        rolRepository.save(rolCliente);

        Rol rolEmpleado = new Rol();
        rolEmpleado.setNombre("ROLE_EMPLEADO");
        rolRepository.save(rolEmpleado);

        Rol rolAdmin = new Rol();
        rolAdmin.setNombre("ROLE_ADMINISTRADOR");
        rolRepository.save(rolAdmin);

        Usuario admin = new Usuario();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of(rolAdmin));
        usuarioRepository.save(admin);

        Usuario empleado = new Usuario();
        empleado.setUsername("empleado");
        empleado.setPassword(passwordEncoder.encode("empleado123"));
        empleado.setRoles(Set.of(rolEmpleado));
        usuarioRepository.save(empleado);

        Usuario cliente = new Usuario();
        cliente.setUsername("cliente");
        cliente.setPassword(passwordEncoder.encode("cliente123"));
        cliente.setRoles(Set.of(rolCliente));
        usuarioRepository.save(cliente);
    }
}
