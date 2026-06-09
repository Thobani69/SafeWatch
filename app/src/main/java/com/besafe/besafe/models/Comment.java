package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class Comment {
    @SerializedName("id")
    private String id;

    @SerializedName("incident_id")
    private String incidentId;

    @SerializedName("sender_role")
    private String senderRole;

    @SerializedName("message")
    private String message;

    @SerializedName("created_at")
    private String createdAt;

    public String getId() { return id; }
    public String getIncidentId() { return incidentId; }
    public String getSenderRole() { return senderRole; }
    public String getMessage() { return message; }
    public String getCreatedAt() { return createdAt; }
}