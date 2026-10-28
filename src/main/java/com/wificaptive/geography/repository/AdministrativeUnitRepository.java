package com.wificaptive.geography.repository;

import com.wificaptive.geography.entity.AdministrativeUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdministrativeUnitRepository extends JpaRepository<AdministrativeUnit, Long> {

    Optional<AdministrativeUnit> findByCode(String code);

    Optional<AdministrativeUnit> findByNameIgnoreCase(String name);

    Optional<AdministrativeUnit> findByNameIgnoreCaseAndLevel(String name, Integer level);

    List<AdministrativeUnit> findByLevelOrderByCodeAsc(Integer level);

    List<AdministrativeUnit> findByParentIdOrderByCodeAsc(Long parentId);

    List<AdministrativeUnit> findByParentIsNullOrderByCodeAsc();

    @Query("SELECT u FROM AdministrativeUnit u WHERE u.path LIKE :pathPrefix% AND u.status = 'ACTIVE' ORDER BY u.level ASC, u.code ASC")
    List<AdministrativeUnit> findByPathStartingWith(@Param("pathPrefix") String pathPrefix);
}

