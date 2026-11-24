package com.wificaptive.geography;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.common.security.UserPrincipal;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.geography.service.AdminScopeService;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminScopeServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private AdministrativeUnitRepository unitRepository;

    @Mock
    private WifiLocationRepository locationRepository;

    private AdminScopeService adminScopeService;

    private AdministrativeUnit city;
    private AdministrativeUnit districtHongBang;
    private AdministrativeUnit wardHoangVanThu;
    private AdministrativeUnit wardThuongLy;

    @BeforeEach
    void setUp() {
        adminScopeService = new AdminScopeService(adminUserRepository, unitRepository, locationRepository);

        city = new AdministrativeUnit("HP", "Thành phố Hải Phòng", 1, "/HP/");
        city.setId(1L);

        districtHongBang = new AdministrativeUnit("HP_HB", "Quận Hồng Bàng", 2, "/HP/HP_HB/");
        districtHongBang.setId(2L);
        districtHongBang.setParent(city);

        wardHoangVanThu = new AdministrativeUnit("HP_HB_HVTHU", "Phường Hoàng Văn Thụ", 3, "/HP/HP_HB/HP_HB_HVTHU/");
        wardHoangVanThu.setId(17L);
        wardHoangVanThu.setParent(districtHongBang);

        wardThuongLy = new AdministrativeUnit("HP_HB_THUONGLY", "Phường Thượng Lý", 3, "/HP/HP_HB/HP_HB_THUONGLY/");
        wardThuongLy.setId(22L);
        wardThuongLy.setParent(districtHongBang);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testExpandUnitAndDescendantIds_DistrictIncludesChildWards() {
        when(unitRepository.findById(2L)).thenReturn(Optional.of(districtHongBang));
        when(unitRepository.findByPathStartingWith("/HP/HP_HB/"))
                .thenReturn(List.of(districtHongBang, wardHoangVanThu, wardThuongLy));
        when(unitRepository.findByParentIdOrderByCodeAsc(2L))
                .thenReturn(List.of(wardHoangVanThu, wardThuongLy));

        Set<Long> expanded = adminScopeService.expandUnitAndDescendantIds(2L);

        assertThat(expanded).containsExactly(2L, 17L, 22L);
    }

    @Test
    void testGetCurrentUserScopedUnitIds_DistrictOfficerSeesDistrictAndChildWards() {
        AdminUser officer = new AdminUser("hongbang_officer", "hash", "hb@hp.gov.vn", "Cán bộ Hồng Bàng", AdminRole.ROLE_DISTRICT_OFFICER, true);
        officer.setId(10L);
        officer.setAdminUnitId(2L);

        UserPrincipal principal = UserPrincipal.create(officer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(adminUserRepository.findById(10L)).thenReturn(Optional.of(officer));
        when(unitRepository.findById(2L)).thenReturn(Optional.of(districtHongBang));
        when(unitRepository.findByPathStartingWith("/HP/HP_HB/"))
                .thenReturn(List.of(districtHongBang, wardHoangVanThu, wardThuongLy));
        when(unitRepository.findByParentIdOrderByCodeAsc(2L))
                .thenReturn(List.of(wardHoangVanThu, wardThuongLy));

        Set<Long> scopedIds = adminScopeService.getCurrentUserScopedUnitIds();

        assertThat(scopedIds).containsExactly(2L, 17L, 22L);
    }

    @Test
    void testGetCurrentUserScopedUnitIds_CommuneOfficerSeesOnlyAssignedWard() {
        AdminUser communeOfficer = new AdminUser("hvthu_officer", "hash", "hvt@hp.gov.vn", "Cán bộ Hoàng Văn Thụ", AdminRole.ROLE_COMMUNE_OFFICER, true);
        communeOfficer.setId(11L);
        communeOfficer.setAdminUnitId(17L);

        UserPrincipal principal = UserPrincipal.create(communeOfficer);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(adminUserRepository.findById(11L)).thenReturn(Optional.of(communeOfficer));
        when(unitRepository.findById(17L)).thenReturn(Optional.of(wardHoangVanThu));
        when(unitRepository.findByPathStartingWith("/HP/HP_HB/HP_HB_HVTHU/"))
                .thenReturn(List.of(wardHoangVanThu));
        when(unitRepository.findByParentIdOrderByCodeAsc(17L)).thenReturn(List.of());

        Set<Long> scopedIds = adminScopeService.getCurrentUserScopedUnitIds();

        assertThat(scopedIds).containsExactly(17L);
    }

    @Test
    void testGetCurrentUserScopedUnitIds_SottttAdminIsUnrestricted() {
        AdminUser sotttt = new AdminUser("sotttt_admin", "hash", "sotttt@hp.gov.vn", "Cán bộ Sở TT&TT", AdminRole.ROLE_SOTTTT_ADMIN, true);
        sotttt.setId(12L);
        sotttt.setAdminUnitId(1L);

        UserPrincipal principal = UserPrincipal.create(sotttt);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(adminUserRepository.findById(12L)).thenReturn(Optional.of(sotttt));

        assertThat(adminScopeService.getCurrentUserScopedUnitIds()).isNull();
    }

    @Test
    void testFormatAdminUnitDisplayName_FormatsAllThreeTiers() {
        assertThat(AdminScopeService.formatAdminUnitDisplayName(city))
                .isEqualTo("Thành phố Hải Phòng");
        assertThat(AdminScopeService.formatAdminUnitDisplayName(districtHongBang))
                .isEqualTo("Quận Hồng Bàng");
        assertThat(AdminScopeService.formatAdminUnitDisplayName(wardHoangVanThu))
                .isEqualTo("Phường Hoàng Văn Thụ - Quận Hồng Bàng");
    }
}
