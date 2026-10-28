package com.wificaptive.geography;

import com.wificaptive.geography.dto.AdminUnitCreateRequest;
import com.wificaptive.geography.dto.AdminUnitDto;
import com.wificaptive.geography.dto.MergeUnitsRequest;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.geography.service.AdministrativeUnitService;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdministrativeUnitServiceTest {

    @Mock
    private AdministrativeUnitRepository unitRepository;

    @Mock
    private WifiLocationRepository locationRepository;

    @InjectMocks
    private AdministrativeUnitService unitService;

    private AdministrativeUnit city;
    private AdministrativeUnit district;

    @BeforeEach
    void setUp() {
        city = new AdministrativeUnit("HP", "Thành phố Hải Phòng", 1, "/HP/");
        city.setId(1L);

        district = new AdministrativeUnit("HP_HB", "Quận Hồng Bàng", 2, "/HP/HP_HB/");
        district.setId(2L);
        district.setParent(city);
    }

    @Test
    void testCreateUnit_successWithPathGeneration() {
        AdminUnitCreateRequest req = new AdminUnitCreateRequest();
        req.setCode("HP_HB_P01");
        req.setName("Phường Hoàng Văn Thụ");
        req.setLevel(3);
        req.setParentId(2L);

        when(unitRepository.findByCode("HP_HB_P01")).thenReturn(Optional.empty());
        when(unitRepository.findById(2L)).thenReturn(Optional.of(district));
        when(unitRepository.save(any(AdministrativeUnit.class))).thenAnswer(invocation -> {
            AdministrativeUnit u = invocation.getArgument(0);
            u.setId(3L);
            return u;
        });

        AdminUnitDto result = unitService.createUnit(req);

        assertThat(result).isNotNull();
        assertThat(result.getCode()).isEqualTo("HP_HB_P01");
        assertThat(result.getPath()).isEqualTo("/HP/HP_HB/HP_HB_P01/");
        assertThat(result.getLevel()).isEqualTo(3);
        verify(unitRepository).save(any(AdministrativeUnit.class));
    }

    @Test
    void testMergeUnits_success() {
        AdministrativeUnit sourceUnit = new AdministrativeUnit("HP_HB_OLD", "Xã Cũ", 3, "/HP/HP_HB/OLD/");
        sourceUnit.setId(10L);

        AdministrativeUnit targetUnit = new AdministrativeUnit("HP_HB_NEW", "Xã Mới", 3, "/HP/HP_HB/NEW/");
        targetUnit.setId(20L);

        MergeUnitsRequest req = new MergeUnitsRequest();
        req.setSourceUnitIds(List.of(10L));
        req.setTargetUnitId(20L);

        when(unitRepository.findById(20L)).thenReturn(Optional.of(targetUnit));
        when(unitRepository.findById(10L)).thenReturn(Optional.of(sourceUnit));

        unitService.mergeUnits(req);

        assertThat(sourceUnit.getStatus()).isEqualTo("MERGED");
        assertThat(sourceUnit.getMergedTo()).isEqualTo(targetUnit);
        verify(unitRepository).save(sourceUnit);
        verify(locationRepository).reassignLocationsAdminUnit(10L, 20L);
    }
}

