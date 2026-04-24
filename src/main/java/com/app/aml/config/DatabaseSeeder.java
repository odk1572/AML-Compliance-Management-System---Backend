package com.app.aml.config;

import com.app.aml.feature.platformuser.entity.PlatformUser;
import com.app.aml.feature.platformuser.repository.PlatformUserRepository;
import com.app.aml.security.rbac.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseSeeder implements CommandLineRunner {

    private final PlatformUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedSuperAdmin();
    }

    private void seedSuperAdmin() {
        String adminEmail = "admin@amlsystem.internal";

        if (!userRepository.existsByEmail(adminEmail)) {
            log.info("Seeding Super Admin user...");

            PlatformUser admin = new PlatformUser();
            // Using the fixed ID from your SQL if you want to maintain consistency
            admin.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
            admin.setEmail(adminEmail);
            admin.setFullName("AML System Administrator");
            admin.setRole(Role.SUPER_ADMIN);

            // Password: Admin@AML2024!
            admin.setPasswordHash(passwordEncoder.encode("Admin@AML2024!"));

            admin.setLocked(false);
            admin.setFailedLoginAttempts(0);

            userRepository.save(admin);
            log.info("Super Admin seeded successfully.");
        } else {
            log.debug("Super Admin already exists. Skipping seeding.");
        }
    }
}