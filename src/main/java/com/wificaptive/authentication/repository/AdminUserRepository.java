package com.wificaptive.authentication.repository;

import com.wificaptive.authentication.entity.AdminRole;
import com.wificaptive.authentication.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
    Optional<AdminUser> findByUsername(String username);
    Optional<AdminUser> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    List<AdminUser> findByRole(AdminRole role);
    List<AdminUser> findByAdminUnitId(Long adminUnitId);
    Page<AdminUser> findByFullNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String fullName, String username, Pageable pageable);
}
