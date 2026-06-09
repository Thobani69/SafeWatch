package com.besafe.besafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.besafe.besafe.R;
import com.besafe.besafe.viewmodels.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private AuthViewModel authViewModel;

    private TextInputLayout tilRegName, tilRegEmail, tilRegPassword;
    private TextInputEditText etRegName, etRegEmail, etRegPassword;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin, tvPasswordStrength;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        tilRegName     = findViewById(R.id.tilRegName);
        tilRegEmail    = findViewById(R.id.tilRegEmail);
        tilRegPassword = findViewById(R.id.tilRegPassword);
        etRegName      = findViewById(R.id.etRegName);
        etRegEmail     = findViewById(R.id.etRegEmail);
        etRegPassword  = findViewById(R.id.etRegPassword);
        btnRegister    = findViewById(R.id.btnRegister);
        tvGoToLogin    = findViewById(R.id.tvGoToLogin);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);

        // Live password strength indicator
        etRegPassword.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int b, int c) {}
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });

        btnRegister.setOnClickListener(v -> attemptRegistration());

        tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void updatePasswordStrength(String pw) {
        int score = 0;
        if (pw.length() >= 8) score++;
        if (pw.matches(".*[A-Z].*")) score++;
        if (pw.matches(".*[0-9].*")) score++;
        if (pw.matches(".*[^A-Za-z0-9].*")) score++;

        String[] labels = {"", "Weak", "Fair", "Strong", "Very strong"};
        if (tvPasswordStrength != null && score > 0) {
            tvPasswordStrength.setText(labels[score]);
        } else if (tvPasswordStrength != null) {
            tvPasswordStrength.setText("");
        }
    }

    private void attemptRegistration() {
        // Clear previous errors
        tilRegName.setError(null);
        tilRegEmail.setError(null);
        tilRegPassword.setError(null);

        String fullName = etRegName.getText() != null ? etRegName.getText().toString().trim() : "";
        String email    = etRegEmail.getText() != null ? etRegEmail.getText().toString().trim() : "";
        String password = etRegPassword.getText() != null ? etRegPassword.getText().toString().trim() : "";

        boolean valid = true;

        if (fullName.isEmpty()) {
            tilRegName.setError("Please enter your full name");
            valid = false;
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilRegEmail.setError("Enter a valid email address");
            valid = false;
        }

        // We check for 6 characters here because your AuthViewModel strict rule requires at least 6
        if (password.length() < 6) {
            tilRegPassword.setError("Password must be at least 6 characters");
            valid = false;
        }

        if (!valid) return;

        // Split full name smoothly
        String firstName = fullName;
        String lastName  = "";
        if (fullName.contains(" ")) {
            firstName = fullName.substring(0, fullName.indexOf(" "));
            lastName  = fullName.substring(fullName.indexOf(" ") + 1);
        }

        // Default hidden fields that we don't ask the user for yet
        String gender = "Not Specified";
        String faculty = "Not Specified";

        btnRegister.setEnabled(false);
        btnRegister.setText("Creating Account...");

        // 🚀 We now pass exactly 6 arguments: email, password, firstName, lastName, gender, faculty
        authViewModel.registerUser(email, password, firstName, lastName, gender, faculty)
                .observe(this, result -> {
                    btnRegister.setEnabled(true);
                    btnRegister.setText("Continue");

                    if ("SUCCESS".equals(result)) {
                        Toast.makeText(this, "Account created! Please sign in.", Toast.LENGTH_LONG).show();
                        // Close the register screen and take them right back to Login
                        finish();
                    } else {
                        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
                    }
                });
    }
}