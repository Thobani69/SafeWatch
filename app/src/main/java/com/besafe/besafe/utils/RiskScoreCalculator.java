package com.besafe.besafe.utils;

import com.besafe.besafe.models.RiskZone;
import java.util.List;

public class RiskScoreCalculator {

    /**
     * AI Risk Engine: Calculates a threat score (0-100) based on incident data.
     */
    public static void applyScore(RiskZone zone, int incidentCount, float severityWeight, float recencyMultiplier) {
        // Base math for the risk score
        float rawScore = (incidentCount * 15f) + (severityWeight * 0.5f);

        // Apply recency multiplier (recent events score higher)
        float finalScore = rawScore * recencyMultiplier;

        // Cap the score at 100
        if (finalScore > 100f) finalScore = 100f;

        // Update the zone's threat level based on the AI score
        if (finalScore >= 80f) {
            zone.setThreatLevel("high");
        } else if (finalScore >= 50f) {
            zone.setThreatLevel("medium");
        } else {
            zone.setThreatLevel("low");
        }
    }

    /**
     * Finds the absolute closest Danger Zone to the student's current location.
     */
    public static RiskZone nearestZone(double studentLat, double studentLon, List<RiskZone> activeZones) {
        if (activeZones == null || activeZones.isEmpty()) return null;

        RiskZone nearest = null;
        double shortestDistance = Double.MAX_VALUE;

        for (RiskZone zone : activeZones) {
            double distance = HaversineUtil.distanceMeters(studentLat, studentLon, zone.getLatitude(), zone.getLongitude());
            if (distance < shortestDistance) {
                shortestDistance = distance;
                nearest = zone;
            }
        }
        return nearest;
    }
}