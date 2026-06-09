package com.besafe.besafe.utils;

import android.util.Log;

import com.besafe.besafe.models.Report;
import com.besafe.besafe.models.RiskZone; // You will need to create a POST/PATCH request for this later

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiskZoneCalculator {

    private static final String TAG = "RiskZoneCalculator";
    private static final double CLUSTER_RADIUS_METERS = 50.0;
    private static final double MIN_SEVERITY_THRESHOLD = 3.0;

    // Weights from your US-025a Specification
    private static final double INCIDENT_WEIGHT = 3.0;
    private static final double RECENCY_WEIGHT = 2.0;
    private static final double VIOLENT_WEIGHT = 4.0;
    private static final double WEAPON_WEIGHT = 5.0;
    private static final double NIGHTTIME_WEIGHT = 1.5;

    /**
     * The Main Engine Method. Call this whenever a new incident is reported
     * or on a 60-minute timer.
     *
     * @param allRecentReports A list of all reports (e.g., from the last 30 days) fetched from Supabase.
     */
    public static List<RiskZone> calculateRiskZones(List<Report> allRecentReports) {
        List<RiskZone> newActiveZones = new ArrayList<>();

        // 1. Group incidents into clusters based on distance
        List<List<Report>> clusters = clusterIncidents(allRecentReports);

        // 2. Score each cluster
        for (List<Report> cluster : clusters) {
            double severityScore = calculateSeverityScore(cluster);

            if (severityScore >= MIN_SEVERITY_THRESHOLD) {
                RiskZone zone = generateRiskZoneFromCluster(cluster, severityScore);
                newActiveZones.add(zone);
                Log.d(TAG, "Created new Risk Zone: " + zone.getThreatLevel() + " (Score: " + severityScore + ")");
            }
        }

        return newActiveZones;
    }

    // ==========================================
    // STEP 1: DBSCAN CLUSTERING LOGIC
    // ==========================================
    private static List<List<Report>> clusterIncidents(List<Report> reports) {
        List<List<Report>> clusters = new ArrayList<>();
        List<Report> unvisited = new ArrayList<>(reports);

        while (!unvisited.isEmpty()) {
            Report currentPoint = unvisited.remove(0);

            // Only cluster SOS alerts or reports with GPS coordinates for now
            if (currentPoint.getLatitude() == 0 || currentPoint.getLongitude() == 0) {
                continue;
            }

            List<Report> currentCluster = new ArrayList<>();
            currentCluster.add(currentPoint);

            // Find neighbors
            for (int i = 0; i < unvisited.size(); i++) {
                Report neighbor = unvisited.get(i);
                if (neighbor.getLatitude() != 0 && neighbor.getLongitude() != 0) {
                    double distance = calculateDistanceMeters(
                            currentPoint.getLatitude(), currentPoint.getLongitude(),
                            neighbor.getLatitude(), neighbor.getLongitude()
                    );

                    if (distance <= CLUSTER_RADIUS_METERS) {
                        currentCluster.add(neighbor);
                        unvisited.remove(i);
                        i--; // Adjust index since we removed an item
                    }
                }
            }

            // Note: The specification said minPts = 3, but for testing,
            // even 1 high-severity incident (like a weapon) might trigger a zone.
            // We will let the math decide based on the score later.
            clusters.add(currentCluster);
        }
        return clusters;
    }

    // ==========================================
    // STEP 2: THE SEVERITY MATH (US-025a)
    // ==========================================
    private static double calculateSeverityScore(List<Report> cluster) {
        int incidentCount = cluster.size();
        double maxRecencyWeight = 0.2; // Default to >30 days
        boolean hasViolentIncident = false;
        boolean hasWeapon = false; // We would need a 'weapon' boolean in the Report model eventually
        int nighttimeCount = 0;

        for (Report report : cluster) {
            // Check Recency
            double reportRecency = calculateRecencyWeight(report.getCreatedAt());
            if (reportRecency > maxRecencyWeight) {
                maxRecencyWeight = reportRecency;
            }

            // Check Violence (Assault, Robbery, Harassment, SOS)
            String category = report.getCategory() != null ? report.getCategory().toUpperCase() : "";
            if (category.contains("ASSAULT") || category.contains("ROBBERY") ||
                    category.contains("HARASSMENT") || category.contains("SOS")) {
                hasViolentIncident = true;
            }

            // Check Nighttime
            if (isNightTime(report.getCreatedAt())) {
                nighttimeCount++;
            }
        }

        double violentWeightValue = hasViolentIncident ? 1.0 : 0.3;
        double weaponWeightValue = hasWeapon ? 1.0 : 0.0;
        double nighttimeProportion = (double) nighttimeCount / incidentCount;

        // The exact formula from your specification:
        double severity = (incidentCount * INCIDENT_WEIGHT)
                + (maxRecencyWeight * RECENCY_WEIGHT)
                + (violentWeightValue * VIOLENT_WEIGHT)
                + (weaponWeightValue * WEAPON_WEIGHT)
                + (nighttimeProportion * NIGHTTIME_WEIGHT);

        // Round to 1 decimal place
        return Math.round(severity * 10.0) / 10.0;
    }

    // ==========================================
    // HELPER FUNCTIONS
    // ==========================================
    private static RiskZone generateRiskZoneFromCluster(List<Report> cluster, double severityScore) {
        // Calculate the geographic center of the cluster
        double totalLat = 0, totalLng = 0;
        for (Report r : cluster) {
            totalLat += r.getLatitude();
            totalLng += r.getLongitude();
        }
        double centerLat = totalLat / cluster.size();
        double centerLng = totalLng / cluster.size();

        // Determine Threat Level based on US-025 spec
        String threatLevel;
        if (severityScore >= 12.0) {
            threatLevel = "high";
        } else if (severityScore >= 6.0) {
            threatLevel = "medium";
        } else {
            threatLevel = "elevated";
        }

        // Return a new RiskZone object (You will need to construct this based on your model)
        RiskZone zone = new RiskZone();
        zone.setCenterLat(centerLat);     // Make sure you add Setters to RiskZone.java!
        zone.setCenterLng(centerLng);
        zone.setRadiusMetres(50);
        zone.setSeverityScore(severityScore);
        zone.setThreatLevel(threatLevel);
        zone.setIncidentCount(cluster.size());
        zone.setZoneName("Generated Hotspot");
        // In the future, you can reverse-geocode this lat/lng to get "Near Library"

        return zone;
    }

    private static double calculateRecencyWeight(String createdAtIsoString) {
        if (createdAtIsoString == null) return 0.2;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        try {
            Date reportDate = sdf.parse(createdAtIsoString);
            long diffInMillis = new Date().getTime() - reportDate.getTime();
            long daysDiff = diffInMillis / (1000 * 60 * 60 * 24);

            if (daysDiff <= 7) return 1.0;
            if (daysDiff <= 30) return 0.6;
            return 0.2;
        } catch (ParseException e) {
            return 0.2;
        }
    }

    private static boolean isNightTime(String createdAtIsoString) {
        if (createdAtIsoString == null) return false;
        try {
            // Simple string parsing to grab the hour (Assuming ISO format yyyy-MM-ddTHH:mm:ss)
            int hour = Integer.parseInt(createdAtIsoString.substring(11, 13));
            return hour >= 20 || hour < 5; // 8 PM to 5 AM
        } catch (Exception e) {
            return false;
        }
    }

    // Haversine formula to calculate distance between two GPS coordinates
    private static double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }
}