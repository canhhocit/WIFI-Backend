package com.wificaptive.authentication.service;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    private PasswordEncoder passwordEncoder;
    private AdminBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        bootstrapService = new AdminBootstrapService(adminUserRepository, passwordEncoder);
        ReflectionTestUtils.setField(bootstrapService, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrapService, "bootstrapUsername", "admin");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapPassword", "admin123");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapEmail", "admin@captive.local");
        ReflectionTestUtils.setField(bootstrapService, "bootstrapFullName", "System Administrator");
    }

    @Test
    @DisplayName("Should bootstrap all 4 government demo accounts with verifiable passwords")
    void shouldBootstrapAllGovernmentAccounts() {
        // Given no users exist
        lenient().when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        // When
        bootstrapService.run();

        // Then 4 accounts should be saved
        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserRepository, times(4)).save(captor.capture());

        var savedUsers = captor.getAllValues();
        assertThat(savedUsers).hasSize(4);

        // Verify admin
        AdminUser admin = savedUsers.stream().filter(u -> u.getUsername().equals("admin")).findFirst().orElseThrow();
        assertThat(admin.getRole()).isEqualTo(AdminRole.ROLE_SUPER_ADMIN);
        assertThat(passwordEncoder.matches("admin123", admin.getPasswordHash())).isTrue();

        // Verify sotttt_admin
        AdminUser sotttt = savedUsers.stream().filter(u -> u.getUsername().equals("sotttt_admin")).findFirst().orElseThrow();
        assertThat(sotttt.getRole()).isEqualTo(AdminRole.ROLE_SOTTTT_ADMIN);
        assertThat(passwordEncoder.matches("password123", sotttt.getPasswordHash())).isTrue();

        // Verify hongbang_officer
        AdminUser hongbang = savedUsers.stream().filter(u -> u.getUsername().equals("hongbang_officer")).findFirst().orElseThrow();
        assertThat(hongbang.getRole()).isEqualTo(AdminRole.ROLE_DISTRICT_OFFICER);
        assertThat(passwordEncoder.matches("password123", hongbang.getPasswordHash())).isTrue();

        // Verify vnpt_tech01
        AdminUser vnpt = savedUsers.stream().filter(u -> u.getUsername().equals("vnpt_tech01")).findFirst().orElseThrow();
        assertThat(vnpt.getRole()).isEqualTo(AdminRole.ROLE_VNPT_ENGINEER);
        assertThat(passwordEncoder.matches("password123", vnpt.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Should update password hash if existing user has invalid or outdated hash")
    void shouldUpdateInvalidPasswordHash() {
        // Given existing user with dummy/corrupted hash
        AdminUser corruptedUser = new AdminUser(
                "hongbang_officer",
                "$2a$10$invalidcorruptedhashplaceholder",
                "canbo.hongbang@haiphong.gov.vn",
                "Cán bộ UBND",
                AdminRole.ROLE_DISTRICT_OFFICER,
                true
        );
        lenient().when(adminUserRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        lenient().when(adminUserRepository.findByUsername("hongbang_officer")).thenReturn(Optional.of(corruptedUser));

        // When
        bootstrapService.run();

        // Then corruptedUser should have its password hash updated
        assertThat(passwordEncoder.matches("password123", corruptedUser.getPasswordHash())).isTrue();
        assertThat(corruptedUser.getFailedLoginAttempts()).isEqualTo(0);
        assertThat(corruptedUser.isAccountNonLocked()).isTrue();
    }
}
