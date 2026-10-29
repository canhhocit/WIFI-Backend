package com.wificaptive.authentication;

import com.wificaptive.audit.service.AuditService;
import com.wificaptive.authentication.dto.AdminUserDto;
import com.wificaptive.authentication.dto.CreateAdminUserRequest;
import com.wificaptive.authentication.dto.UpdateAdminUserRequest;
import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.authentication.service.AdminUserService;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdministrativeUnitRepository administrativeUnitRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;

    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserService = new AdminUserService(
                adminUserRepository,
                administrativeUnitRepository,
                passwordEncoder,
                auditService
        );
    }

    @Test
    void testGetUsers_ReturnsPage() {
        AdminUser user = new AdminUser("admin_test", "hashed", "admin@hp.gov.vn", "Test User", AdminRole.ROLE_ADMIN, true);
        user.setId(10L);
        when(adminUserRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(user)));

        Page<AdminUserDto> page = adminUserService.getUsers(null, PageRequest.of(0, 10));

        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("admin_test", page.getContent().get(0).getUsername());
    }

    @Test
    void testCreateUser_Success() {
        CreateAdminUserRequest req = new CreateAdminUserRequest();
        req.setUsername("officer01");
        req.setPassword("pass12345");
        req.setEmail("officer01@hp.gov.vn");
        req.setFullName("Nguyen Van A");
        req.setRole(AdminRole.ROLE_DISTRICT_OFFICER);

        when(adminUserRepository.existsByUsername("officer01")).thenReturn(false);
        when(adminUserRepository.existsByEmail("officer01@hp.gov.vn")).thenReturn(false);
        when(passwordEncoder.encode("pass12345")).thenReturn("encodedSecret");

        AdminUser saved = new AdminUser("officer01", "encodedSecret", "officer01@hp.gov.vn", "Nguyen Van A", AdminRole.ROLE_DISTRICT_OFFICER, true);
        saved.setId(101L);
        when(adminUserRepository.save(any(AdminUser.class))).thenReturn(saved);

        AdminUserDto dto = adminUserService.createUser(req, "superadmin");

        assertNotNull(dto);
        assertEquals("officer01", dto.getUsername());
        assertEquals(AdminRole.ROLE_DISTRICT_OFFICER, dto.getRole());
        verify(adminUserRepository).save(any(AdminUser.class));
    }

    @Test
    void testUnlockUser_Success() {
        AdminUser user = new AdminUser("locked_user", "hash", "locked@hp.gov.vn", "Locked", AdminRole.ROLE_ADMIN, true);
        user.setId(20L);
        user.setFailedLoginAttempts(5);
        when(adminUserRepository.findById(20L)).thenReturn(Optional.of(user));
        when(adminUserRepository.save(any(AdminUser.class))).thenAnswer(i -> i.getArgument(0));

        AdminUserDto dto = adminUserService.unlockUser(20L, "superadmin");

        assertNotNull(dto);
        assertEquals(0, dto.getFailedLoginAttempts());
        assertFalse(dto.isLocked());
    }
}

