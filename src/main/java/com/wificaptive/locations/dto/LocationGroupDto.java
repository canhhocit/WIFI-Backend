package com.wificaptive.locations.dto;

import com.wificaptive.locations.entity.LocationGroup;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class LocationGroupDto {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String color;
    private int locationCount;
    private List<Long> locationIds;
    private Instant createdAt;
    private Instant updatedAt;

    public LocationGroupDto() {}

    public static LocationGroupDto from(LocationGroup group) {
        LocationGroupDto dto = new LocationGroupDto();
        dto.setId(group.getId());
        dto.setName(group.getName());
        dto.setCode(group.getCode());
        dto.setDescription(group.getDescription());
        dto.setColor(group.getColor());
        dto.setLocationCount(group.getLocations() != null ? group.getLocations().size() : 0);
        if (group.getLocations() != null) {
            dto.setLocationIds(group.getLocations().stream().map(l -> l.getId()).collect(Collectors.toList()));
        }
        dto.setCreatedAt(group.getCreatedAt());
        dto.setUpdatedAt(group.getUpdatedAt());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public int getLocationCount() {
        return locationCount;
    }

    public void setLocationCount(int locationCount) {
        this.locationCount = locationCount;
    }

    public List<Long> getLocationIds() {
        return locationIds;
    }

    public void setLocationIds(List<Long> locationIds) {
        this.locationIds = locationIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
