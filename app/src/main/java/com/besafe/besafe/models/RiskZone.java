package com.besafe.besafe.models;

import com.google.gson.annotations.SerializedName;

public class RiskZone {
    @SerializedName("id")
    private String id;

    @SerializedName("zone_name")
    private String zoneName;

    @SerializedName("cluster_center_lat")
    private double centerLat;

    @SerializedName("cluster_center_lng")
    private double centerLng;

    @SerializedName("radius_metres")
    private int radiusMetres;

    @SerializedName("severity_score")
    private double severityScore;

    @SerializedName("threat_level")
    private String threatLevel;

    @SerializedName("incident_count")
    private int incidentCount;

    @SerializedName("dominant_incident_type")
    private String dominantIncidentType;

    // Empty constructor for Gson deserialization from Supabase
    public RiskZone() {}


    public RiskZone(String zoneName, String dominantIncidentType, int incidentCount,
                    double lat, double lon, float radiusMetres, String threatLevel) {
        this.zoneName             = zoneName;
        this.dominantIncidentType = dominantIncidentType;
        this.incidentCount        = incidentCount;
        this.centerLat            = lat;   // ← was missing
        this.centerLng            = lon;   // ← was missing
        this.radiusMetres         = (int) radiusMetres;
        this.threatLevel          = threatLevel;
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String getId()                    { return id; }
    public String getZoneName()              { return zoneName; }
    public double getCenterLat()             { return centerLat; }
    public double getCenterLng()             { return centerLng; }
    public int    getRadiusMetres()          { return radiusMetres; }
    public double getSeverityScore()         { return severityScore; }
    public String getThreatLevel()           { return threatLevel; }
    public int    getIncidentCount()         { return incidentCount; }
    public String getDominantIncidentType()  { return dominantIncidentType; }


    public double getLatitude()  { return centerLat; }
    public double getLongitude() { return centerLng; }

    // ── Setters ───────────────────────────────────────────────────────────────
    public void setZoneName(String zoneName)                       { this.zoneName = zoneName; }
    public void setCenterLat(double centerLat)                     { this.centerLat = centerLat; }
    public void setCenterLng(double centerLng)                     { this.centerLng = centerLng; }
    public void setRadiusMetres(int radiusMetres)                  { this.radiusMetres = radiusMetres; }
    public void setSeverityScore(double severityScore)             { this.severityScore = severityScore; }
    public void setThreatLevel(String threatLevel)                 { this.threatLevel = threatLevel; }
    public void setIncidentCount(int incidentCount)                { this.incidentCount = incidentCount; }
    public void setDominantIncidentType(String dominantIncidentType) { this.dominantIncidentType = dominantIncidentType; }
}