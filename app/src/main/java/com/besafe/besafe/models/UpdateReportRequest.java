package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class UpdateReportRequest {
    @SerializedName("status")
    private String status;

    public UpdateReportRequest(String status) {
        this.status = status;
    }
}