package com.besafe.besafe.data.repository;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.besafe.besafe.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AlertRepository {

    private final OkHttpClient client = new OkHttpClient();

    private static final String SUPABASE_URL      = "https://xbmitdvkxbpinghvpgzv.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_27rSWmjgZA6sz2rMRvJmIg_FSFvgqV4";

    /**
     * Sends an SOS alert.
     *
     * Uses the authenticated user's JWT (not the anon key) so Supabase RLS
     * can attach the correct reporter identity. Falls back to anon key if no
     * token is stored (should never happen in practice).
     *
     * Returns "SUCCESS:{alertId}" so callers can embed the ID in the
     * OneSignal push notification for direct deep-linking.
     */
    public LiveData<String> sendAlert(double latitude, double longitude, Context context) {
        MutableLiveData<String> result = new MutableLiveData<>();

        String userEmail = TokenManager.getEmail(context);
        String userToken = TokenManager.getToken(context);
        // Use the user's own JWT so RLS rules recognise them; fall back to anon
        String authHeader = (userToken != null && !userToken.isEmpty())
                ? "Bearer " + userToken
                : "Bearer " + SUPABASE_ANON_KEY;

        String jsonBody = "{"
                + "\"latitude\": " + latitude + ","
                + "\"longitude\": " + longitude + ","
                + "\"status\": \"ACTIVE\","
                + "\"reporter_email\": \"" + userEmail + "\""
                + "}";

        RequestBody body = RequestBody.create(
                jsonBody, MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(SUPABASE_URL + "/rest/v1/alerts")
                .post(body)
                .addHeader("apikey",        SUPABASE_ANON_KEY)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type",  "application/json")
                // return=representation → Supabase sends back the created row including its UUID
                .addHeader("Prefer",        "return=representation")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                result.postValue("Network Error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String raw     = response.body().string();
                        JSONArray arr  = new JSONArray(raw);
                        String alertId = arr.length() > 0
                                ? arr.getJSONObject(0).optString("id", "")
                                : "";
                        result.postValue("SUCCESS:" + alertId);
                    } catch (Exception e) {
                        result.postValue("SUCCESS:");
                    }
                } else {
                    result.postValue("Failed to send alert.");
                }
            }
        });

        return result;
    }
}