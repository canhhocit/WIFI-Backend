package com.wificaptive.geography.service;

import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.geography.dto.AdminUnitCreateRequest;
import com.wificaptive.geography.dto.AdminUnitDto;
import com.wificaptive.geography.dto.MergeUnitsRequest;
import com.wificaptive.geography.entity.AdministrativeUnit;
import com.wificaptive.geography.repository.AdministrativeUnitRepository;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdministrativeUnitService {

    private final AdministrativeUnitRepository unitRepository;
    private final WifiLocationRepository locationRepository;

    public AdministrativeUnitService(AdministrativeUnitRepository unitRepository,
                                     WifiLocationRepository locationRepository) {
        this.unitRepository = unitRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUnitDto> getAllTree() {
        List<AdministrativeUnit> roots = unitRepository.findByParentIsNullOrderByCodeAsc();
        return roots.stream().map(this::mapToTreeDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminUnitDto> getByLevel(Integer level) {
        return unitRepository.findByLevelOrderByCodeAsc(level).stream()
                .map(this::mapToFlatDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AdminUnitDto> getChildren(Long parentId) {
        return unitRepository.findByParentIdOrderByCodeAsc(parentId).stream()
                .map(this::mapToFlatDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminUnitDto getById(Long id) {
        AdministrativeUnit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdministrativeUnit", "id", id));
        return mapToTreeDto(unit);
    }

    public AdminUnitDto createUnit(AdminUnitCreateRequest request) {
        if (unitRepository.findByCode(request.getCode()).isPresent()) {
            throw new BadRequestException("Mã đơn vị địa bàn đã tồn tại: " + request.getCode());
        }

        AdministrativeUnit unit = new AdministrativeUnit();
        unit.setCode(request.getCode().trim().toUpperCase());
        unit.setName(request.getName().trim());
        unit.setLevel(request.getLevel());
        unit.setNotes(request.getNotes());

        if (request.getParentId() != null) {
            AdministrativeUnit parent = unitRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("AdministrativeUnit", "parentId", request.getParentId()));
            unit.setParent(parent);
            unit.setPath(parent.getPath() + unit.getCode() + "/");
        } else {
            unit.setPath("/" + unit.getCode() + "/");
        }

        AdministrativeUnit saved = unitRepository.save(unit);
        return mapToFlatDto(saved);
    }

    public AdminUnitDto updateUnit(Long id, AdminUnitCreateRequest request) {
        AdministrativeUnit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AdministrativeUnit", "id", id));

        unit.setName(request.getName().trim());
        unit.setNotes(request.getNotes());
        return mapToFlatDto(unitRepository.save(unit));
    }

    public void mergeUnits(MergeUnitsRequest request) {
        AdministrativeUnit target = unitRepository.findById(request.getTargetUnitId())
                .orElseThrow(() -> new ResourceNotFoundException("AdministrativeUnit", "targetUnitId", request.getTargetUnitId()));

        for (Long sourceId : request.getSourceUnitIds()) {
            if (sourceId.equals(target.getId())) continue;

            AdministrativeUnit source = unitRepository.findById(sourceId)
                    .orElseThrow(() -> new ResourceNotFoundException("AdministrativeUnit", "sourceId", sourceId));

            source.setStatus("MERGED");
            source.setMergedTo(target);
            source.setNotes((source.getNotes() != null ? source.getNotes() + " | " : "") + "Đã sáp nhập vào " + target.getName());
            unitRepository.save(source);

            // Re-assign all locations from source unit to target unit
            locationRepository.reassignLocationsAdminUnit(sourceId, target.getId());
        }
    }

    private AdminUnitDto mapToTreeDto(AdministrativeUnit unit) {
        AdminUnitDto dto = mapToFlatDto(unit);
        if (unit.getChildren() != null && !unit.getChildren().isEmpty()) {
            dto.setChildren(unit.getChildren().stream()
                    .filter(c -> "ACTIVE".equals(c.getStatus()))
                    .map(this::mapToTreeDto)
                    .collect(Collectors.toList()));
        } else {
            dto.setChildren(new ArrayList<>());
        }
        return dto;
    }

    public AdminUnitDto mapToFlatDto(AdministrativeUnit unit) {
        AdminUnitDto dto = new AdminUnitDto();
        dto.setId(unit.getId());
        if (unit.getParent() != null) {
            dto.setParentId(unit.getParent().getId());
            dto.setParentName(unit.getParent().getName());
        }
        dto.setCode(unit.getCode());
        dto.setName(unit.getName());
        dto.setLevel(unit.getLevel());
        dto.setPath(unit.getPath());
        dto.setStatus(unit.getStatus());
        dto.setNotes(unit.getNotes());
        dto.setCreatedAt(unit.getCreatedAt());
        return dto;
    }
}
