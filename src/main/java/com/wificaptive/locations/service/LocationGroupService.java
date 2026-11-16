package com.wificaptive.locations.service;

import com.wificaptive.common.exception.BadRequestException;
import com.wificaptive.common.exception.ResourceNotFoundException;
import com.wificaptive.locations.dto.CreateLocationGroupRequest;
import com.wificaptive.locations.dto.LocationGroupDto;
import com.wificaptive.locations.entity.LocationGroup;
import com.wificaptive.locations.entity.WifiLocation;
import com.wificaptive.locations.repository.LocationGroupRepository;
import com.wificaptive.locations.repository.WifiLocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class LocationGroupService {

    private final LocationGroupRepository groupRepository;
    private final WifiLocationRepository locationRepository;

    public LocationGroupService(LocationGroupRepository groupRepository, WifiLocationRepository locationRepository) {
        this.groupRepository = groupRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional(readOnly = true)
    public List<LocationGroupDto> getAllGroups() {
        return groupRepository.findAll().stream()
                .map(LocationGroupDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LocationGroupDto getGroupById(Long id) {
        LocationGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationGroup not found with id: " + id));
        return LocationGroupDto.from(group);
    }

    public LocationGroupDto createGroup(CreateLocationGroupRequest req) {
        if (groupRepository.existsByCode(req.getCode())) {
            throw new BadRequestException("Mã nhóm đã tồn tại: " + req.getCode());
        }
        if (groupRepository.existsByName(req.getName())) {
            throw new BadRequestException("Tên nhóm đã tồn tại: " + req.getName());
        }

        LocationGroup group = new LocationGroup(req.getName(), req.getCode(), req.getDescription(), req.getColor());

        if (req.getLocationIds() != null && !req.getLocationIds().isEmpty()) {
            List<WifiLocation> locs = locationRepository.findAllById(req.getLocationIds());
            group.setLocations(new HashSet<>(locs));
        }

        LocationGroup saved = groupRepository.save(group);
        return LocationGroupDto.from(saved);
    }

    public LocationGroupDto updateGroup(Long id, CreateLocationGroupRequest req) {
        LocationGroup group = groupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LocationGroup not found with id: " + id));

        group.setName(req.getName());
        group.setDescription(req.getDescription());
        if (req.getColor() != null) {
            group.setColor(req.getColor());
        }
        group.setUpdatedAt(Instant.now());

        if (req.getLocationIds() != null) {
            List<WifiLocation> locs = locationRepository.findAllById(req.getLocationIds());
            group.setLocations(new HashSet<>(locs));
        }

        LocationGroup saved = groupRepository.save(group);
        return LocationGroupDto.from(saved);
    }

    public void deleteGroup(Long id) {
        if (!groupRepository.existsById(id)) {
            throw new ResourceNotFoundException("LocationGroup not found with id: " + id);
        }
        groupRepository.deleteById(id);
    }

    public LocationGroupDto assignLocationsToGroup(Long groupId, List<Long> locationIds) {
        LocationGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("LocationGroup not found with id: " + groupId));

        if (locationIds != null && !locationIds.isEmpty()) {
            List<WifiLocation> locs = locationRepository.findAllById(locationIds);
            group.getLocations().addAll(locs);
        }
        group.setUpdatedAt(Instant.now());
        return LocationGroupDto.from(groupRepository.save(group));
    }
}
