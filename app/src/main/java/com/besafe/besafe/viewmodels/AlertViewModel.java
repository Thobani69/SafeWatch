package com.besafe.besafe.viewmodels;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.besafe.besafe.data.repository.AlertRepository;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AlertViewModel extends ViewModel {

    private AlertRepository alertRepository;
    private final OkHttpClient client = new OkHttpClient();

    // ✅ FIXED: Base URL only — no table path here
    private static final String SUPABASE_BASE_URL = "https://xbmitdvkxbpinghvpgzv.supabase.co";
    private static final String SUPABASE_ANON_KEY = "sb_publishable_27rSWmjgZA6sz2rMRvJmIg_FSFvgqV4";

    public AlertViewModel() {
        alertRepository = new AlertRepository();
    }

    public LiveData<String> sendEmergencyAlert(double latitude, double longitude, Context context) {
        return alertRepository.sendAlert(latitude, longitude, context);
    }

    // ── Save broadcast ────────────────────────────────────────────────────────
    public void saveBroadcastToDatabase(String title, String message, String audience) {
        new Thread(() -> {
            try {
                String jsonBody = "{"
                        + "\"title\": \"" + title + "\","
                        + "\"message\": \"" + message + "\","
                        + "\"target_audience\": \"" + audience + "\""
                        + "}";

                RequestBody body = RequestBody.create(
                        jsonBody, MediaType.parse("application/json; charset=utf-8"));

                // ✅ FIXED: correct single URL
                Request request = new Request.Builder()
                        .url(SUPABASE_BASE_URL + "/rest/v1/campus_broadcasts")
                        .post(body)
                        .addHeader("apikey",        SUPABASE_ANON_KEY)
                        .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                        .addHeader("Content-Type",  "application/json")
                        .addHeader("Prefer",        "return=minimal")
                        .build();

                client.newCall(request).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // ── Fetch broadcasts ──────────────────────────────────────────────────────
    public LiveData<List<String>> fetchBroadcasts(String userRole) {
        MutableLiveData<List<String>> broadcastsLiveData = new MutableLiveData<>();

        String filter;
        if ("security".equals(userRole)) {
            filter = "&target_audience=in.(Everyone,Security Only)";
        } else {
            filter = "&target_audience=in.(Everyone,Students Only)";
        }

        // ✅ FIXED: correct single URL
        Request request = new Request.Builder()
                .url(SUPABASE_BASE_URL + "/rest/v1/campus_broadcasts?select=*"
                        + filter + "&order=created_at.desc")
                .get()
                .addHeader("apikey",        SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                broadcastsLiveData.postValue(new ArrayList<>());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String responseData = response.body().string();
                        JSONArray jsonArray  = new JSONArray(responseData);
                        List<String> list    = new ArrayList<>();

                        for (int i = 0; i < jsonArray.length(); i++) {
                            JSONObject obj = jsonArray.getJSONObject(i);
                            String title   = obj.getString("title");
                            String msg     = obj.getString("message");
                            // ✅ FIXED: Convert UTC timestamp to South African Standard Time (UTC+2)
                            String rawDate = obj.getString("created_at");
                            String sastDate = convertToSAST(rawDate);

                            list.add("📢 " + title + "\n\n" + msg + "\n\n🕐 " + sastDate);
                        }
                        broadcastsLiveData.postValue(list);
                    } catch (Exception e) {
                        e.printStackTrace();
                        broadcastsLiveData.postValue(new ArrayList<>());
                    }
                } else {
                    broadcastsLiveData.postValue(new ArrayList<>());
                }
            }
        });

        return broadcastsLiveData;
    }

    /**
     * Converts a Supabase UTC timestamp (e.g. "2026-05-10T22:07:00.123456+00:00")
     * to a human-readable South African Standard Time string (UTC+2).
     * e.g. "10 May 2026, 00:07 SAST"
     */
    private String convertToSAST(String utcTimestamp) {
        try {
            // Supabase returns ISO 8601 — parse it
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Strip microseconds and timezone suffix for clean parsing
            String cleanTimestamp = utcTimestamp.length() > 19
                    ? utcTimestamp.substring(0, 19)
                    : utcTimestamp;

            Date date = inputFormat.parse(cleanTimestamp);

            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm");
            outputFormat.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg")); // SAST = UTC+2

            return outputFormat.format(date) + " SAST";
        } catch (Exception e) {
            // If parsing fails, just return the raw string trimmed to date only
            return utcTimestamp.length() >= 10 ? utcTimestamp.substring(0, 10) : utcTimestamp;
        }
    }
}