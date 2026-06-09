package com.besafe.besafe.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class StudentActivityReceiver extends BroadcastReceiver {

    public static final String ACTION_ACTIVITY_UPDATE = "ACTION_ACTIVITY_UPDATE";
    public static final String EXTRA_ACTIVITY_TYPE = "EXTRA_ACTIVITY_TYPE";
    public static final String EXTRA_CONFIDENCE = "EXTRA_CONFIDENCE";

    public static final String ACTIVITY_RUNNING = "RUNNING";
    public static final String ACTIVITY_STATIONARY = "STATIONARY";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            if (result != null) {
                // Get the most probable activity the student is doing right now
                DetectedActivity mostProbableActivity = result.getMostProbableActivity();

                int activityType = mostProbableActivity.getType();
                int confidence = mostProbableActivity.getConfidence();

                String activityName = getActivityString(activityType);
                Log.d("ActivityReceiver", "AI Detected: " + activityName + " (" + confidence + "%)");

                // Broadcast this result to our MapFragment so it can react!
                Intent localIntent = new Intent(ACTION_ACTIVITY_UPDATE);
                localIntent.putExtra(EXTRA_ACTIVITY_TYPE, activityName);
                localIntent.putExtra(EXTRA_CONFIDENCE, confidence);
                LocalBroadcastManager.getInstance(context).sendBroadcast(localIntent);
            }
        }
    }

    // Translates Google's integer codes into readable text
    private String getActivityString(int detectedActivityType) {
        switch (detectedActivityType) {
            case DetectedActivity.IN_VEHICLE: return "IN_VEHICLE";
            case DetectedActivity.ON_BICYCLE: return "ON_BICYCLE";
            case DetectedActivity.ON_FOOT: return "ON_FOOT";
            case DetectedActivity.RUNNING: return ACTIVITY_RUNNING;
            case DetectedActivity.STILL: return ACTIVITY_STATIONARY;
            case DetectedActivity.WALKING: return "WALKING";
            default: return "UNKNOWN";
        }
    }
}