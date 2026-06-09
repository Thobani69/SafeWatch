package com.besafe.besafe.utils;

import android.util.Log;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NotificationHelper {

    private static final String ONESIGNAL_APP_ID = "656207f7-97cf-4cee-99fa-b8b27e041fc8";
    private static final String REST_API_KEY     = "os_v2_app_mvrap54xz5go5gp2xczh4ba7zdg2csdfi3yeqpvtzlf6y7e4txalnzbovfhumxmx7qj4tmfhewun5l64s5cu7aps2wymvxkelpfacmi";

    private static final OkHttpClient client = new OkHttpClient();

    // SOS to security with deep-link data
    public static void sendSosToSecurity(String studentName, String locationDetails,
                                         String alertId, double lat, double lon) {
        String jsonBody = "{"
                + "\"app_id\": \"" + ONESIGNAL_APP_ID + "\","
                + "\"target_channel\": \"push\","
                + "\"filters\": [{\"field\": \"tag\", \"key\": \"role\", \"relation\": \"=\", \"value\": \"security\"}],"
                + "\"headings\": {\"en\": \"SOS — STUDENT IN DANGER\"},"
                + "\"contents\": {\"en\": \"" + studentName + " needs help at " + locationDetails + ". Tap to respond.\"},"
                + "\"data\": {"
                +   "\"type\": \"SOS_ALERT\","
                +   "\"alert_id\": \"" + (alertId != null ? alertId : "") + "\","
                +   "\"latitude\": " + lat + ","
                +   "\"longitude\": " + lon
                + "},"
                + "\"android_vibration\": 1,"
                + "\"android_led_color\": \"FFDC3545\","
                + "\"android_sound\": \"notification\","
                + "\"priority\": 10,"
                + "\"android_visibility\": 1"
                + "}";
        postToOneSignal(jsonBody);
    }

    public static void sendSosToSecurity(String studentName, String locationDetails) {
        sendSosToSecurity(studentName, locationDetails, null, 0.0, 0.0);
    }

    /**
     * Notifies the student that their incident has been resolved.
     * Targets by email tag — set on the student's OneSignal profile at login.
     */
    public static void sendResolutionToStudent(String studentEmail) {
        if (studentEmail == null || studentEmail.isEmpty()) return;
        String jsonBody = "{"
                + "\"app_id\": \"" + ONESIGNAL_APP_ID + "\","
                + "\"target_channel\": \"push\","
                + "\"filters\": [{\"field\": \"tag\", \"key\": \"email\", \"relation\": \"=\", \"value\": \"" + studentEmail + "\"}],"
                + "\"headings\": {\"en\": \"Incident Resolved\"},"
                + "\"contents\": {\"en\": \"Your emergency has been resolved by campus security. Stay safe.\"},"
                + "\"data\": {\"type\": \"RESOLVED\"},"
                + "\"priority\": 7"
                + "}";
        postToOneSignal(jsonBody);
    }

    public static void sendCampusBroadcast(String title, String message) {
        String jsonBody = "{"
                + "\"app_id\": \"" + ONESIGNAL_APP_ID + "\","
                + "\"target_channel\": \"push\","
                + "\"included_segments\": [\"Total Subscriptions\"],"
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + message + "\"},"
                + "\"data\": {\"type\": \"BROADCAST\"}"
                + "}";
        postToOneSignal(jsonBody);
    }

    public static void sendTargetedBroadcast(String title, String message, String targetGroup) {
        String targetJson;
        if ("Students Only".equals(targetGroup)) {
            targetJson = "\"filters\": [{\"field\": \"tag\", \"key\": \"role\", \"relation\": \"=\", \"value\": \"student\"}],";
        } else if ("Security Only".equals(targetGroup)) {
            targetJson = "\"filters\": [{\"field\": \"tag\", \"key\": \"role\", \"relation\": \"=\", \"value\": \"security\"}],";
        } else {
            targetJson = "\"included_segments\": [\"Total Subscriptions\"],";
        }
        String jsonBody = "{"
                + "\"app_id\": \"" + ONESIGNAL_APP_ID + "\","
                + "\"target_channel\": \"push\","
                + targetJson
                + "\"headings\": {\"en\": \"" + title + "\"},"
                + "\"contents\": {\"en\": \"" + message + "\"},"
                + "\"data\": {\"type\": \"BROADCAST\"}"
                + "}";
        postToOneSignal(jsonBody);
    }

    private static void postToOneSignal(String jsonBody) {
        RequestBody body = RequestBody.create(
                jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url("https://api.onesignal.com/notifications?c=push")
                .post(body)
                .addHeader("Authorization", "Basic " + REST_API_KEY)
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json")
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e("OneSignal", "Failed: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (response.body() != null) Log.d("OneSignal", response.body().string());
            }
        });
    }
}