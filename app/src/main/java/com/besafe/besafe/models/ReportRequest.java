package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class ReportRequest {

    @SerializedName("category")
    private String category;

    @SerializedName("description")
    private String description;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_anonymous")
    private boolean isAnonymous;

    @SerializedName("student_id")
    private String studentId;

    // ✨ NEW: The email of the student submitting the report
    @SerializedName("reporter_email")
    private String reporterEmail;

    // ✨ UPDATE: Added reporterEmail to the constructor
    public ReportRequest(String category, String description, double latitude, double longitude, String imageUrl, boolean isAnonymous, String studentId, String reporterEmail) {
        this.category = category;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.isAnonymous = isAnonymous;
        this.studentId = studentId;
        this.reporterEmail = reporterEmail; // Save it
    }

    public String getCategory() { return category; }
    public String getDescription() { return description; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getImageUrl() { return imageUrl; }
    public boolean isAnonymous() { return isAnonymous; }
    public String getStudentId() { return studentId; }
    public String getReporterEmail() { return reporterEmail; } // ✨ NEW
}