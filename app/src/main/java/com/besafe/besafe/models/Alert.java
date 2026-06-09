package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class Alert {

    @SerializedName("id")
    private String id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("status")
    private String status;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("reporter_email")
    private String reporterEmail;

    // NEW: Guard coordinates
    // We use Double (Object) instead of double (primitive) so it can safely handle 'null'
    // if a guard hasn't been dispatched yet.
    @SerializedName("guard_latitude")
    private Double guardLatitude;

    @SerializedName("guard_longitude")
    private Double guardLongitude;

    // Existing Getters
    public String getId() { return id; }
    public String getUserId() { return userId; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
    public String getReporterEmail() { return reporterEmail; }

    // NEW: Getters for the guard's location to fix the MapFragment errors
    public Double getGuardLatitude() { return guardLatitude; }
    public Double getGuardLongitude() { return guardLongitude; }
}