package com.wificaptive.campaigns.dto;

public class ApproveCampaignRequest {
    private String comment;
    private String rejectReason;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
}

