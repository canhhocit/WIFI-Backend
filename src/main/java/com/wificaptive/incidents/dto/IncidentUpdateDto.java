package com.wificaptive.incidents.dto;

import com.wificaptive.incidents.entity.IncidentUpdate;
import java.time.Instant;

public class IncidentUpdateDto {
    private Long id;
    private Long ticketId;
    private String updatedBy;
    private String action;
    private String note;
    private Instant createdAt;

    public static IncidentUpdateDto from(IncidentUpdate u) {
        if (u == null) return null;
        IncidentUpdateDto dto = new IncidentUpdateDto();
        dto.setId(u.getId());
        dto.setTicketId(u.getTicketId());
        dto.setUpdatedBy(u.getActor());
        dto.setAction(u.getAction());
        dto.setNote(u.getNotes());
        dto.setCreatedAt(u.getCreatedAt());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
