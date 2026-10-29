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

        // 1. Super Admin (admin / admin123)
        bootstrapOrUpdateAccount(bootstrapUsername, bootstrapPassword, bootstrapEmail, bootstrapFullName, AdminRole.ROLE_SUPER_ADMIN, null, null);

        // 2. Sở TT&TT Hải Phòng (sotttt_admin / password123)
        bootstrapOrUpdateAccount("sotttt_admin", "password123", "sotttt@haiphong.gov.vn", "Cán bộ Sở TT&TT Hải Phòng", AdminRole.ROLE_SOTTTT_ADMIN, "0901234567", null);

        // 3. UBND Quận Hồng Bàng (hongbang_officer / password123)
        bootstrapOrUpdateAccount("hongbang_officer", "password123", "canbo.hongbang@haiphong.gov.vn", "Cán bộ UBND Quận Hồng Bàng", AdminRole.ROLE_DISTRICT_OFFICER, "0912345678", 1L);

        // 4. Kỹ sư Viễn thông VNPT (vnpt_tech01 / password123)
        bootstrapOrUpdateAccount("vnpt_tech01", "password123", "kythuat.vnpt01@vnpt.vn", "Kỹ thuật viên Viễn thông VNPT", AdminRole.ROLE_VNPT_ENGINEER, "0987654321", null);
    }

    private void bootstrapOrUpdateAccount(String username, String rawPassword, String email, String fullName, AdminRole role, String phone, Long adminUnitId) {
        AdminUser user = adminUserRepository.findByUsername(username).orElse(null);
        if (user == null) {
            user = new AdminUser(
                    username,
                    passwordEncoder.encode(rawPassword),
                    email,
                    fullName,
                    role,
                    true
            );
            if (phone != null) user.setPhoneNumber(phone);
            if (adminUnitId != null) user.setAdminUnitId(adminUnitId);
            adminUserRepository.save(user);
            log.info("Initial government administrator created: username='{}', role='{}'", username, role);
        } else {
            // Check if password hash is valid or needs to be synchronized with raw password
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                user.setEnabled(true);
                adminUserRepository.save(user);
                log.info("Synchronized and activated password hash for government account: username='{}'", username);
            }
        }
    }
}
