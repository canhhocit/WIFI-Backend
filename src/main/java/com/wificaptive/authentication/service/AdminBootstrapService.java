package com.wificaptive.authentication.service;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBootstrapService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapService.class);

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.admin.username:admin}")
    private String bootstrapUsername;

    @Value("${app.bootstrap.admin.password:admin123}")
    private String bootstrapPassword;

    @Value("${app.bootstrap.admin.email:admin@captive.local}")
    private String bootstrapEmail;

    @Value("${app.bootstrap.admin.full-name:System Administrator}")
    private String bootstrapFullName;

    public AdminBootstrapService(AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!bootstrapEnabled) {
            log.info("Admin bootstrap is disabled via configuration.");
            return;
        }

        if (adminUserRepository.count() == 0) {
            log.info("No administrative accounts detected. Bootstrapping default initial administrator '{}' for local development.", bootstrapUsername);
            AdminUser admin = new AdminUser(
                    bootstrapUsername,
                    passwordEncoder.encode(bootstrapPassword),
                    bootstrapEmail,
                    bootstrapFullName,
                    AdminRole.ROLE_SUPER_ADMIN,
                    true
            );
            adminUserRepository.save(admin);
            log.info("Initial development administrator successfully created: username='{}', email='{}'", bootstrapUsername, bootstrapEmail);
        } else {
            log.debug("Administrative accounts exist. Skipping bootstrap.");
        }
    }
}
