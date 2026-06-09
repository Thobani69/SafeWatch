package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class CommentRequest {
    @SerializedName("incident_id")
    private String incidentId;

    @SerializedName("sender_role")
    private String senderRole;

    @SerializedName("message")
    private String message;

    public CommentRequest(String incidentId, String senderRole, String message) {
        this.incidentId = incidentId;
        this.senderRole = senderRole;
        this.message = message;
    }
}