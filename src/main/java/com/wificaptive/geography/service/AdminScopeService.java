package com.wificaptive.geography.service;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import com.wificaptive.authentication.repository.AdminUserRepository;
import com.wificaptive.common.security.UserPrincipal;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Dịch vụ phân giải phạm vi địa bàn hành chính 3 cấp (Thành phố -> Quận/Huyện -> Phường/Xã)
 * phục vụ lọc dữ liệu (Data Scoping) theo tài khoản cán bộ đang đăng nhập.
 */
@Service
@Transactional(readOnly = true)
public class AdminScopeService {

    private final AdminUserRepository adminUserRepository;
    private final AdministrativeUnitRepository unitRepository;
    private final WifiLocationRepository locationRepository;

    public AdminScopeService(AdminUserRepository adminUserRepository,
                             AdministrativeUnitRepository unitRepository,
                             WifiLocationRepository locationRepository) {
        this.adminUserRepository = adminUserRepository;
        this.unitRepository = unitRepository;
        this.locationRepository = locationRepository;
    }

    /**
     * Mở rộng một đơn vị hành chính sang tập ID bao gồm chính nó và mọi đơn vị cấp dưới trực thuộc.
     * - Cấp 1 (Thành phố): bao gồm toàn bộ Quận/Huyện (Cấp 2) và Phường/Xã (Cấp 3)
     * - Cấp 2 (Quận/Huyện): bao gồm Quận/Huyện đó và toàn bộ Phường/Xã (Cấp 3) trực thuộc
     * - Cấp 3 (Phường/Xã): chỉ gồm Phường/Xã đó
     */
    public Set<Long> expandUnitAndDescendantIds(Long unitId) {
        if (unitId == null) {
            return Set.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(unitId);

        unitRepository.findById(unitId).ifPresent(unit -> {
            if (unit.getPath() != null && !unit.getPath().isBlank()) {
                List<AdministrativeUnit> descendants = unitRepository.findByPathStartingWith(unit.getPath());
                for (AdministrativeUnit d : descendants) {
                    if (d.getId() != null) {
                        ids.add(d.getId());
                    }
                }
            }
            List<AdministrativeUnit> directChildren = unitRepository.findByParentIdOrderByCodeAsc(unitId);
            for (AdministrativeUnit child : directChildren) {
                if (child.getId() != null) {
                    ids.add(child.getId());
                    List<AdministrativeUnit> grandChildren = unitRepository.findByParentIdOrderByCodeAsc(child.getId());
                    for (AdministrativeUnit gc : grandChildren) {
                        if (gc.getId() != null) {
                            ids.add(gc.getId());
                        }
                    }
                }
            }
        });
        return ids;
    }

    /**
     * Trả về tập ID đơn vị hành chính mà cán bộ hiện tại được phép quản lý/truy cập.
     * Trả về {@code null} nếu cán bộ có quyền toàn thành phố (không giới hạn địa bàn).
     */
    public Set<Long> getCurrentUserScopedUnitIds() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }

        AdminUser user = adminUserRepository.findById(principal.getId()).orElse(null);
        if (user == null) {
            return null;
        }

        // Quản trị cấp cao và Sở TT&TT luôn có tầm nhìn toàn thành phố
        if (user.getRole() == AdminRole.ROLE_SUPER_ADMIN || user.getRole() == AdminRole.ROLE_SOTTTT_ADMIN) {
            return null;
        }

        Long assignedUnitId = user.getAdminUnitId();
        if (assignedUnitId == null) {
            return null;
        }

        AdministrativeUnit unit = unitRepository.findById(assignedUnitId).orElse(null);
        if (unit == null) {
            return null;
        }

        // Nếu gán vào Cấp 1 (Thành phố Hải Phòng) -> phạm vi toàn thành phố
        if (unit.getLevel() != null && unit.getLevel() <= 1) {
            return null;
        }

        return expandUnitAndDescendantIds(unit.getId());
    }

    /**
     * Trả về tập ID điểm phát Wi-Fi thuộc phạm vi địa bàn của cán bộ hiện tại,
     * hoặc {@code null} nếu cán bộ có quyền toàn thành phố.
     */
    public Set<Long> getCurrentUserScopedLocationIds() {
        Set<Long> scopedUnitIds = getCurrentUserScopedUnitIds();
        if (scopedUnitIds == null) {
            return null;
        }
        if (scopedUnitIds.isEmpty()) {
            return Set.of();
        }
        return locationRepository.findByAdminUnit_IdInAndDeletedFalse(scopedUnitIds).stream()
                .map(WifiLocation::getId)
                .collect(Collectors.toSet());
    }

    /**
     * Định dạng nhãn hiển thị địa bàn 3 cấp rõ ràng (kèm Quận/Huyện cha nếu là Cấp 3).
     */
    public static String formatAdminUnitDisplayName(AdministrativeUnit unit) {
        if (unit == null) {
            return null;
        }
        Integer level = unit.getLevel();
        if (level != null && level == 3) {
            String parentName = unit.getParent() != null ? unit.getParent().getName() : null;
            if (parentName != null && !parentName.isBlank()) {
                return unit.getName() + " - " + parentName;
            }
        }
        return unit.getName();
    }
}
