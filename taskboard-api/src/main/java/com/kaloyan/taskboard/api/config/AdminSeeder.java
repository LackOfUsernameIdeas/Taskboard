package com.kaloyan.taskboard.api.config;

import com.kaloyan.taskboard.core.model.Role;
import com.kaloyan.taskboard.core.model.User;
import com.kaloyan.taskboard.core.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Provisions the initial ADMIN account on application startup.
 *
 * This is the only place an ADMIN user gets created. Registration
 * (AuthController#register) always creates a plain USER - there is no
 * self-service or first-user-wins path to admin rights.
 *
 * If an ADMIN already exists, this does nothing. Otherwise it creates one
 * from app.admin.username / app.admin.password (backed by the
 * ADMIN_USERNAME / ADMIN_PASSWORD env vars). If a non-admin account already
 * has that username, it is promoted instead of creating a duplicate.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByRole(Role.ADMIN)) {
            return;
        }

        User admin = userRepository.findByUsername(adminUsername).orElseGet(User::new);
        boolean isNewUser = admin.getId() == null;

        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);

        if (isNewUser) {
            log.info("Seeded initial admin account '{}'", adminUsername);
        } else {
            log.info("Promoted existing account '{}' to admin", adminUsername);
        }
    }
}
