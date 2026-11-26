package com.wificaptive.authentication.service;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired(required = false)
    private AdministrativeUnitRepository administrativeUnitRepository;

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

        // 2. Sở KH&CN Hải Phòng (sotttt_admin / password123)
        bootstrapOrUpdateAccount("sotttt_admin", "password123", "sotttt@haiphong.gov.vn", "Cán bộ Sở KH&CN Hải Phòng", AdminRole.ROLE_SOTTTT_ADMIN, "0901234567", null);

        // 3. UBND Phường Hồng Bàng (hongbang_officer / password123)
        Long hongBangUnitId = administrativeUnitRepository != null
                ? administrativeUnitRepository.findByCode("HP_P_HONGBANG").map(u -> u.getId()).orElse(2L)
                : 2L;
        bootstrapOrUpdateAccount("hongbang_officer", "password123", "canbo.hongbang@haiphong.gov.vn", "Cán bộ UBND Phường Hồng Bàng", AdminRole.ROLE_COMMUNE_OFFICER, "0912345678", hongBangUnitId);

        // (Đã bỏ tài khoản Kỹ sư Viễn thông viettel_tech01 theo mô hình 3 cấp tài khoản)
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
            boolean changed = false;
            if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
                user.setPasswordHash(passwordEncoder.encode(rawPassword));
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                user.setEnabled(true);
                changed = true;
            }
            if (fullName != null && !fullName.equals(user.getFullName())) {
                user.setFullName(fullName);
                changed = true;
            }
            if (role != null && role != user.getRole()) {
                user.setRole(role);
                changed = true;
            }
            if (changed) {
                adminUserRepository.save(user);
                log.info("Synchronized account profile & role for government account: username='{}'", username);
            }
        }
    }
}
