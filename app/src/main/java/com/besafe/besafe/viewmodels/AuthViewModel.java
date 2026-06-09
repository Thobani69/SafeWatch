package com.besafe.besafe.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.data.repository.AuthRepository;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthViewModel extends ViewModel {

    private AuthRepository authRepository;

    public AuthViewModel() {
        authRepository = new AuthRepository();
    }

    // ==========================================
    // 🛡️ FAST REGISTRATION (No Phone Required Here)
    // ==========================================
    public LiveData<String> registerUser(String email, String password, String firstName, String lastName, String gender, String faculty) {
        MutableLiveData<String> errorData = new MutableLiveData<>();
        String cleanEmail = email.trim().toLowerCase();

        // 1. Strict Rule: Exactly 8 digits followed by @dut4life.ac.za
        String emailPattern = "^\\d{8}@dut4life\\.ac\\.za$";
        if (!cleanEmail.matches(emailPattern)) {
            errorData.setValue("Error: Email must be 8 numbers followed by @dut4life.ac.za");
            return errorData;
        }

        if (password.length() < 6) {
            errorData.setValue("Error: Password must be at least 6 characters");
            return errorData;
        }

        // 2. ✨ AUTO-FILL MAGIC: Extract student number from the email!
        String autoStudentNumber = cleanEmail.substring(0, 8);

        // 3. Pass data to Repository (We pass an empty string "" for the phone number!)
        return authRepository.registerStudent(cleanEmail, password, autoStudentNumber, firstName, lastName, "", gender, faculty);
    }

    // ==========================================
    // LOGIN
    // ==========================================
    public LiveData<String> loginUser(String email, String password, Context context) {
        if (email.trim().isEmpty() || password.trim().isEmpty()) {
            MutableLiveData<String> errorData = new MutableLiveData<>();
            errorData.setValue("Error: Please fill in all fields.");
            return errorData;
        }
        return authRepository.loginStudent(email, password, context);
    }

    // ==========================================
    // 🛡️ GATEKEEPER LOGIC: CHECK PROFILE & SAVE ROLE
    // ==========================================
    public LiveData<Boolean> isProfileComplete(String token, String email, Context context) {
        MutableLiveData<Boolean> isComplete = new MutableLiveData<>();
        SupabaseService apiService = ApiClient.getClient().create(SupabaseService.class);

        apiService.getUserProfileByEmail(token, "*", "eq." + email).enqueue(new Callback<com.google.gson.JsonArray>() {
            @Override
            public void onResponse(Call<com.google.gson.JsonArray> call, Response<com.google.gson.JsonArray> response) {
                if (response.isSuccessful() && response.body() != null && response.body().size() > 0) {

                    com.google.gson.JsonObject user = response.body().get(0).getAsJsonObject();

                    // Extract role from the DB and save it locally!
                    if (user.has("role") && !user.get("role").isJsonNull()) {
                        String dbRole = user.get("role").getAsString().toLowerCase().trim();
                        com.besafe.besafe.utils.TokenManager.saveRole(context, dbRole);
                    } else {
                        com.besafe.besafe.utils.TokenManager.saveRole(context, "student");
                    }

                    boolean hasStudentNum = user.has("student_number") && !user.get("student_number").isJsonNull() && !user.get("student_number").getAsString().isEmpty();
                    boolean hasPhone = user.has("phone") && !user.get("phone").isJsonNull() && !user.get("phone").getAsString().isEmpty();

                    isComplete.setValue(hasStudentNum && hasPhone);
                } else {
                    isComplete.setValue(false);
                }
            }

            @Override
            public void onFailure(Call<com.google.gson.JsonArray> call, Throwable t) {
                isComplete.setValue(false);
            }
        });

        return isComplete;
    }

    // ==========================================
    // 🛡️ GATEKEEPER LOGIC: SAVE PROFILE
    // ==========================================
    public LiveData<String> completeUserProfile(String studentNumber, String phone, String gender, String faculty, String token, String email) {
        MutableLiveData<String> result = new MutableLiveData<>();
        SupabaseService apiService = ApiClient.getClient().create(SupabaseService.class);

        java.util.Map<String, String> updates = new java.util.HashMap<>();
        updates.put("student_number", studentNumber);
        updates.put("phone", phone);
        updates.put("gender", gender);
        updates.put("faculty", faculty);

        apiService.updateUserProfileByEmail(token, "eq." + email, updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    result.setValue("SUCCESS");
                } else {
                    result.setValue("Error saving profile code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                result.setValue("Network error: " + t.getMessage());
            }
        });

        return result;
    }
}