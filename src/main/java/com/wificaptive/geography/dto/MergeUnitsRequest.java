package com.wificaptive.geography.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class MergeUnitsRequest {

    @NotEmpty(message = "Danh sách đơn vị nguồn sáp nhập không được để trống")
    private List<Long> sourceUnitIds;

    @NotNull(message = "Đơn vị đích sáp nhập không được để trống")
    private Long targetUnitId;

    private String notes;

    public MergeUnitsRequest() {}

    public List<Long> getSourceUnitIds() { return sourceUnitIds; }
    public void setSourceUnitIds(List<Long> sourceUnitIds) { this.sourceUnitIds = sourceUnitIds; }

    public Long getTargetUnitId() { return targetUnitId; }
    public void setTargetUnitId(Long targetUnitId) { this.targetUnitId = targetUnitId; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}

