package com.besafe.besafe.data.remote;

import com.besafe.besafe.models.AlertRequest;
import com.besafe.besafe.models.AuthResponse;
import com.besafe.besafe.models.LoginRequest;
import com.besafe.besafe.models.RegisterRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface SupabaseService {


    @POST("auth/v1/signup")
    Call<AuthResponse> registerUser(@Body RegisterRequest request);


    @POST("auth/v1/token?grant_type=password")
    Call<AuthResponse> loginUser(@Body LoginRequest request);


    @Headers("Prefer: return=minimal")
    @POST("rest/v1/alerts")
    Call<Void> sendEmergencyAlert(
            @Header("Authorization") String token,
            @Body AlertRequest request
    );


    @GET("rest/v1/users?select=role")
    Call<java.util.List<com.besafe.besafe.models.UserData>> getUserProfile(
            @Header("Authorization") String token
    );


    @GET("rest/v1/users")
    Call<com.google.gson.JsonArray> getUserProfileByEmail(
            @Header("Authorization") String token,
            @retrofit2.http.Query("select") String select,
            @retrofit2.http.Query("email") String emailMatch
    );


    @PATCH("rest/v1/users")
    Call<Void> updateUserProfileByEmail(
            @Header("Authorization") String token,
            @retrofit2.http.Query("email") String emailMatch,
            @Body java.util.Map<String, String> updateData
    );


    @GET("rest/v1/alerts?status=eq.ACTIVE&order=created_at.desc")
    Call<java.util.List<com.besafe.besafe.models.Alert>> getActiveAlerts(
            @Header("Authorization") String token
    );


    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/alerts")
    Call<Void> updateAlertStatus(
            @Header("Authorization") String token,
            @retrofit2.http.Query("id") String exactId,
            @Body com.besafe.besafe.models.UpdateAlertRequest request
    );


    @GET("rest/v1/alerts?order=created_at.desc&limit=1")
    Call<java.util.List<com.besafe.besafe.models.Alert>> getMyLatestAlert(
            @Header("Authorization") String token
    );


    @GET("rest/v1/alerts?status=eq.RESOLVED&order=created_at.desc")
    Call<java.util.List<com.besafe.besafe.models.Alert>> getResolvedAlerts(
            @Header("Authorization") String token
    );



    // 1. Send the text report to the database
    @POST("rest/v1/reports")
    retrofit2.Call<Void> submitReport(
            @Header("Authorization") String token,
            @Body com.besafe.besafe.models.ReportRequest request
    );

    // 2. Upload incident photo to the report_images bucket
    @POST("storage/v1/object/report_images/{fileName}")
    retrofit2.Call<okhttp3.ResponseBody> uploadReportImage(
            @Header("Authorization") String token,
            @Header("Content-Type") String contentType,
            @retrofit2.http.Path("fileName") String fileName,
            @Body okhttp3.RequestBody file
    );


    @POST("storage/v1/object/avatars/{fileName}")
    retrofit2.Call<okhttp3.ResponseBody> uploadAvatar(
            @Header("Authorization") String token,
            @Header("Content-Type") String contentType,
            @retrofit2.http.Path("fileName") String fileName,
            @Body okhttp3.RequestBody file
    );

    // 3. Fetch all reports from the database (Ordered by newest first)
    @GET("rest/v1/reports?select=*&order=created_at.desc")
    retrofit2.Call<java.util.List<com.besafe.besafe.models.Report>> getReports(
            @Header("Authorization") String token
    );

    // 4. Fetch all SOS Alerts from the database
    @GET("rest/v1/alerts?select=*&order=created_at.desc")
    retrofit2.Call<java.util.List<com.besafe.besafe.models.Alert>> getAlerts(
            @Header("Authorization") String token
    );



    // Fetch comments for a specific report, ordered oldest to newest
    @GET("rest/v1/comments?select=*&order=created_at.asc")
    retrofit2.Call<java.util.List<com.besafe.besafe.models.Comment>> getComments(
            @Header("Authorization") String token,
            @retrofit2.http.Query("incident_id") String exactIncidentId
    );

    // Post a new comment
    @POST("rest/v1/comments")
    retrofit2.Call<Void> postComment(
            @Header("Authorization") String token,
            @retrofit2.http.Body com.besafe.besafe.models.CommentRequest request
    );

    // Allows Security to update the status of Non-Urgent Reports
    @Headers("Prefer: return=minimal")
    @PATCH("rest/v1/reports")
    Call<Void> updateReportStatus(
            @Header("Authorization") String token,
            @retrofit2.http.Query("id") String exactId,
            @retrofit2.http.Body com.besafe.besafe.models.UpdateReportRequest request
    );


    @GET("rest/v1/risk_zones?is_active=eq.true")
    retrofit2.Call<java.util.List<com.besafe.besafe.models.RiskZone>> getActiveRiskZones(
            @Header("Authorization") String token
    );

    // ✨ ADMIN STATS: Get total number of registered users
    @GET("rest/v1/users?select=id")
    Call<com.google.gson.JsonArray> getTotalUsersCount(@Header("Authorization") String token);

    // ✨ ADMIN STATS: Get total number of alerts for a specific date
    @GET("rest/v1/alerts?select=id")
    Call<com.google.gson.JsonArray> getSosTodayCount(@Header("Authorization") String token, @Query("created_at") String dateFilter);
}