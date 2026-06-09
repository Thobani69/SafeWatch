package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class UpdateAlertRequest {

    @SerializedName("status")
    private String status;

    // These @SerializedName tags MUST match your Supabase columns exactly!
    @SerializedName("guard_latitude")
    private Double guardLatitude;

    @SerializedName("guard_longitude")
    private Double guardLongitude;

    public UpdateAlertRequest(String status) {
        this.status = status;
    }

    // Attaches the guard's location to the database update
    public void setGuardLocation(Double lat, Double lon) {
        this.guardLatitude = lat;
        this.guardLongitude = lon;
    }

    public String getStatus() {
        return status;
    }
}