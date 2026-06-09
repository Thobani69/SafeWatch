package com.besafe.besafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.besafe.besafe.MainActivity;
import com.besafe.besafe.R;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.UserData;
import com.besafe.besafe.utils.TokenManager;
import com.besafe.besafe.viewmodels.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin;
    private TextView tvGoToRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Persistent session — skip login if already signed in
        String savedToken = TokenManager.getToken(this);
        if (savedToken != null && !savedToken.isEmpty()) {
            goToMain();
            return;
        }

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        etEmail        = findViewById(R.id.etLoginEmail);
        etPassword     = findViewById(R.id.etLoginPassword);
        btnLogin       = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> attemptLogin());
        tvGoToRegister.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class)));
    }

    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Signing In...");

        authViewModel.loginUser(email, password, this).observe(this, result -> {
            if (!"SUCCESS".equals(result)) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Sign In");
                Toast.makeText(LoginActivity.this, result, Toast.LENGTH_LONG).show();
                return;
            }

            // Token is saved — now fetch the role fresh from Supabase
            // Do NOT read TokenManager.getRole() here — it may not be set yet
            // because AuthRepository's async profile fetch may still be running
            TokenManager.saveEmail(LoginActivity.this, email);
            fetchRoleThenRoute(email);
        });
    }

    /**
     * Fetches the user's role directly from Supabase users table.
     * This guarantees we have the real role before deciding where to send them.
     */
    private void fetchRoleThenRoute(String email) {
        String token = "Bearer " + TokenManager.getToken(LoginActivity.this);
        SupabaseService api = ApiClient.getClient().create(SupabaseService.class);

        api.getUserProfileByEmail(token, "role,student_number,first_name",
                        "eq." + email)
                .enqueue(new Callback<com.google.gson.JsonArray>() {
                    @Override
                    public void onResponse(Call<com.google.gson.JsonArray> call,
                                           Response<com.google.gson.JsonArray> response) {

                        btnLogin.setEnabled(true);
                        btnLogin.setText("Sign In");

                        String role = "student"; // safe default

                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().size() > 0) {

                            com.google.gson.JsonObject user =
                                    response.body().get(0).getAsJsonObject();

                            if (user.has("role") && !user.get("role").isJsonNull()) {
                                role = user.get("role").getAsString();
                            }

                            // Persist the real role so the rest of the app can read it
                            TokenManager.saveRole(LoginActivity.this, role);
                        }

                        // ── Route by role ──────────────────────────────────
                        if ("admin".equals(role) || "security".equals(role)) {
                            // Non-students skip the profile completeness check entirely
                            Toast.makeText(LoginActivity.this,
                                    "Welcome back!", Toast.LENGTH_SHORT).show();
                            goToMain();

                        } else {
                            // Students must have completed their profile
                            authViewModel.isProfileComplete(token, email, LoginActivity.this)
                                    .observe(LoginActivity.this, isComplete -> {
                                        if (isComplete != null && isComplete) {
                                            Toast.makeText(LoginActivity.this,
                                                    "Authentication Successful",
                                                    Toast.LENGTH_SHORT).show();
                                            goToMain();
                                        } else {
                                            Toast.makeText(LoginActivity.this,
                                                    "Please complete your profile to continue.",
                                                    Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(LoginActivity.this,
                                                    CompleteProfileActivity.class);
                                            intent.putExtra("USER_EMAIL", email);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onFailure(Call<com.google.gson.JsonArray> call, Throwable t) {
                        // Network error — fall back to whatever role was saved
                        btnLogin.setEnabled(true);
                        btnLogin.setText("Sign In");
                        String savedRole = TokenManager.getRole(LoginActivity.this);
                        if ("admin".equals(savedRole) || "security".equals(savedRole)) {
                            goToMain();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Network error. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void goToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}