package com.besafe.besafe.data.repository;

import android.content.Context;
import androidx.lifecycle.MutableLiveData;

import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.AuthResponse;
import com.besafe.besafe.models.LoginRequest;
import com.besafe.besafe.models.RegisterRequest;
import com.besafe.besafe.models.UserData;
import com.besafe.besafe.utils.TokenManager;
import com.onesignal.OneSignal;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {

    private final SupabaseService apiService;

    public AuthRepository() {
        apiService = ApiClient.getClient().create(SupabaseService.class);
    }

    // ── Register ──────────────────────────────────────────────────────────────
    public MutableLiveData<String> registerStudent(String email, String password,
                                                   String studentNumber, String firstName, String lastName,
                                                   String phone, String gender, String faculty) {

        MutableLiveData<String> result = new MutableLiveData<>();
        UserData userData = new UserData(studentNumber, "student",
                firstName, lastName, gender, faculty, phone);
        RegisterRequest request = new RegisterRequest(email, password, userData);

        apiService.registerUser(request).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(Call<AuthResponse> call, Response<AuthResponse> response) {
                result.setValue(response.isSuccessful() && response.body() != null
                        ? "SUCCESS"
                        : "Error: Registration failed. Email might already be taken.");
            }
            @Override
            public void onFailure(Call<AuthResponse> call, Throwable t) {
                result.setValue("Error: " + t.getMessage());
            }
        });
        return result;
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    public MutableLiveData<String> loginStudent(String email, String password, Context context) {
        MutableLiveData<String> loginResult = new MutableLiveData<>();

        apiService.loginUser(new LoginRequest(email, password))
                .enqueue(new Callback<AuthResponse>() {
                    @Override
                    public void onResponse(Call<AuthResponse> call,
                                           Response<AuthResponse> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().getAccessToken() != null) {

                            String accessToken  = response.body().getAccessToken();
                            String refreshToken = response.body().getRefreshToken();

                            // Save both tokens so we can refresh silently later
                            TokenManager.saveToken(context, accessToken);
                            if (refreshToken != null)
                                TokenManager.saveRefreshToken(context, refreshToken);

                            // Fetch role from users table
                            apiService.getUserProfile("Bearer " + accessToken)
                                    .enqueue(new Callback<java.util.List<UserData>>() {
                                        @Override
                                        public void onResponse(
                                                Call<java.util.List<UserData>> c,
                                                Response<java.util.List<UserData>> r) {
                                            String role = "student";
                                            if (r.isSuccessful() && r.body() != null
                                                    && !r.body().isEmpty()) {
                                                role = r.body().get(0).getRole();
                                            }
                                            TokenManager.saveRole(context, role);

                                            // Tag the user in OneSignal with their email
                                            // so we can send them personal push notifications
                                            OneSignal.getUser().addTag("email", email);
                                            OneSignal.getUser().addTag("role", role);

                                            loginResult.setValue("SUCCESS");
                                        }

                                        @Override
                                        public void onFailure(
                                                Call<java.util.List<UserData>> c, Throwable t) {
                                            TokenManager.saveRole(context, "student");
                                            loginResult.setValue("SUCCESS");
                                        }
                                    });
                        } else {
                            loginResult.setValue("Error: Invalid email or password.");
                        }
                    }

                    @Override
                    public void onFailure(Call<AuthResponse> call, Throwable t) {
                        loginResult.setValue("Network Error: " + t.getMessage());
                    }
                });

        return loginResult;
    }
}