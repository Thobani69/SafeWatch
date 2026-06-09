package com.besafe.besafe.data.remote;

import android.content.Context;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL     = "https://xbmitdvkxbpinghvpgzv.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_27rSWmjgZA6sz2rMRvJmIg_FSFvgqV4";

    private static Retrofit retrofit = null;

    // Context-aware client used for token refresh
    private static Context appContext = null;

    public static void init(Context ctx) {
        appContext = ctx.getApplicationContext();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    // Interceptor 1: attach API key and user JWT to every request
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request original = chain.request();
                            Request.Builder builder = original.newBuilder()
                                    .header("apikey", SUPABASE_KEY)
                                    .header("Content-Type", "application/json");

                            if (original.header("Authorization") == null) {
                                builder.header("Authorization", "Bearer " + SUPABASE_KEY);
                            }

                            return chain.proceed(builder.build());
                        }
                    })
                    // Interceptor 2: auto-refresh JWT on 401
                    .addInterceptor(new Interceptor() {
                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Response response = chain.proceed(chain.request());

                            // If unauthorized and we have a context + refresh token, try to refresh
                            if (response.code() == 401 && appContext != null) {
                                String refreshToken = com.besafe.besafe.utils.TokenManager
                                        .getRefreshToken(appContext);

                                if (refreshToken != null && !refreshToken.isEmpty()) {
                                    // Call Supabase refresh endpoint directly (OkHttp, not Retrofit)
                                    String newToken = refreshAccessToken(refreshToken);

                                    if (newToken != null) {
                                        // Save new token and retry the original request
                                        com.besafe.besafe.utils.TokenManager
                                                .saveToken(appContext, newToken);

                                        response.close();
                                        Request retryRequest = chain.request().newBuilder()
                                                .header("Authorization", "Bearer " + newToken)
                                                .build();
                                        return chain.proceed(retryRequest);
                                    }
                                }
                            }
                            return response;
                        }
                    })
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    /**
     * Synchronously calls Supabase /auth/v1/token?grant_type=refresh_token
     * Returns the new access_token string, or null if it fails.
     */
    private static String refreshAccessToken(String refreshToken) {
        try {
            okhttp3.OkHttpClient refreshClient = new okhttp3.OkHttpClient();
            String json = "{\"refresh_token\":\"" + refreshToken + "\"}";
            RequestBody body = RequestBody.create(json,
                    MediaType.parse("application/json; charset=utf-8"));

            Request request = new Request.Builder()
                    .url(BASE_URL + "/auth/v1/token?grant_type=refresh_token")
                    .post(body)
                    .addHeader("apikey", SUPABASE_KEY)
                    .addHeader("Content-Type", "application/json")
                    .build();

            Response response = refreshClient.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                String bodyStr = response.body().string();
                // Simple JSON parse for access_token
                org.json.JSONObject obj = new org.json.JSONObject(bodyStr);
                String newAccess  = obj.optString("access_token", null);
                String newRefresh = obj.optString("refresh_token", null);

                // Persist new refresh token too if provided
                if (newRefresh != null && appContext != null) {
                    com.besafe.besafe.utils.TokenManager.saveRefreshToken(appContext, newRefresh);
                }
                return newAccess;
            }
        } catch (Exception e) {
            android.util.Log.e("ApiClient", "Token refresh failed: " + e.getMessage());
        }
        return null;
    }

    // Reset singleton so tests / logout can force a new client
    public static void reset() {
        retrofit = null;
    }
}