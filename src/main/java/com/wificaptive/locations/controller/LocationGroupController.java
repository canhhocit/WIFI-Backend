package com.wificaptive.locations.controller;

import com.wificaptive.common.dto.ApiResponse;
import com.wificaptive.locations.dto.CreateLocationGroupRequest;
import com.wificaptive.locations.dto.LocationGroupDto;
import com.wificaptive.locations.service.LocationGroupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/location-groups", "/v1/admin/location-groups"})
public class LocationGroupController {

    private final LocationGroupService groupService;

    public LocationGroupController(LocationGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ApiResponse<List<LocationGroupDto>> getGroups() {
        return ApiResponse.ok(groupService.getAllGroups());
    }

    @GetMapping("/{id}")
    public ApiResponse<LocationGroupDto> getGroupById(@PathVariable Long id) {
        return ApiResponse.ok(groupService.getGroupById(id));
    }

    @PostMapping
    public ApiResponse<LocationGroupDto> createGroup(@Valid @RequestBody CreateLocationGroupRequest req) {
        return ApiResponse.ok("Tạo nhóm điểm tùy biến thành công", groupService.createGroup(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<LocationGroupDto> updateGroup(@PathVariable Long id, @Valid @RequestBody CreateLocationGroupRequest req) {
        return ApiResponse.ok("Cập nhật nhóm điểm thành công", groupService.updateGroup(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteGroup(@PathVariable Long id) {
        groupService.deleteGroup(id);
        return ApiResponse.ok("Xóa nhóm điểm thành công", null);
    }

    @PostMapping("/{id}/assign")
    public ApiResponse<LocationGroupDto> assignLocations(@PathVariable Long id, @RequestBody List<Long> locationIds) {
        return ApiResponse.ok("Gán điểm vào nhóm thành công", groupService.assignLocationsToGroup(id, locationIds));
    }
}
