package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class Report {

    @SerializedName("id")
    private String id;

    @SerializedName("category")
    private String category;

    @SerializedName("description")
    private String description;

    @SerializedName("status")
    private String status;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("latitude")
    private double latitude;

    @SerializedName("longitude")
    private double longitude;

    // Getters
    public String getId()          { return id; }
    public String getCategory()    { return category; }
    public String getDescription() { return description; }
    public String getStatus()      { return status; }
    public String getImageUrl()    { return imageUrl; }
    public String getCreatedAt()   { return createdAt; }
    public double getLatitude()    { return latitude; }
    public double getLongitude()   { return longitude; }

    // Empty constructor for Gson
    public Report() {}

    /**
     * Constructor used to convert a regular (non-urgent) report from Supabase.
     */
    public Report(String id, String category, String description,
                  String status, String createdAt) {
        this.id          = id;
        this.category    = category;
        this.description = description;
        this.status      = status;
        this.createdAt   = createdAt;
    }


    public Report(String id, String category, String description,
                  String status, String createdAt, double latitude, double longitude) {
        this.id          = id;
        this.category    = category;
        this.description = description;
        this.status      = status;
        this.createdAt   = createdAt;
        this.latitude    = latitude;
        this.longitude   = longitude;
    }
}