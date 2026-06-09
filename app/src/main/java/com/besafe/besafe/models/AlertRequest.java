package com.besafe.besafe.models;

public class AlertRequest {
    private double latitude;
    private double longitude;
    private String status;

    public AlertRequest(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "ACTIVE"; // The alarm is active as soon as it's created!
    }
}