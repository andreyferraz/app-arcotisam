package com.arcotisam.app.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.arcotisam.app.service.UsuarioService;

@Component
public class AdminUserSeeder implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminUserSeeder.class);

    private final UsuarioService usuarioService;

    public AdminUserSeeder(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String username = "admin";
        String password = "senha123";

        try {
            var opt = usuarioService.buscarPorUsername(username);
            if (opt.isPresent()) {
                LOG.info("Admin user '{}' already exists, skipping seed.", username);
                return;
            }

            usuarioService.criarAdmin(username, password, null);
            LOG.info("Admin user '{}' created by seed.", username);
        } catch (Exception ex) {
            LOG.error("Failed to seed admin user: {}", ex.getMessage());
        }
    }
}
