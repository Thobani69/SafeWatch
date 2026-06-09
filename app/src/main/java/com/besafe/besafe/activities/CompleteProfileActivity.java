package com.besafe.besafe.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.besafe.besafe.MainActivity;
import com.besafe.besafe.R;
import com.besafe.besafe.utils.TokenManager;
import com.besafe.besafe.viewmodels.AuthViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

// ✨ NEW: Import OneSignal
import com.onesignal.OneSignal;

public class CompleteProfileActivity extends AppCompatActivity {

    private TextInputEditText etStudentNumber, etPhone;
    private AutoCompleteTextView dropdownGender, dropdownFaculty;
    private MaterialButton btnSaveProfile;

    private String userEmail; // We will grab this from the Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complete_profile);

        // Grab the email passed from LoginActivity
        userEmail = getIntent().getStringExtra("USER_EMAIL");

        etStudentNumber = findViewById(R.id.etProfileStudentNumber);
        etPhone = findViewById(R.id.etProfilePhone);
        dropdownGender = findViewById(R.id.dropdownProfileGender);
        dropdownFaculty = findViewById(R.id.dropdownProfileFaculty);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);

        setupDropdowns();

        btnSaveProfile.setOnClickListener(v -> saveProfileData());
    }

    private void setupDropdowns() {
        String[] genders = new String[]{"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genders);
        dropdownGender.setAdapter(genderAdapter);

        String[] faculties = new String[]{
                "Accounting & Informatics",
                "Applied Sciences",
                "Arts & Design",
                "Engineering & Built Environment",
                "Health Sciences",
                "Management Sciences"
        };
        ArrayAdapter<String> facultyAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, faculties);
        dropdownFaculty.setAdapter(facultyAdapter);
    }

    private void saveProfileData() {
        String studentNumber = etStudentNumber.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String gender = dropdownGender.getText().toString();
        String faculty = dropdownFaculty.getText().toString();

        if (studentNumber.isEmpty() || phone.isEmpty() || gender.isEmpty() || faculty.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields to continue.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userEmail == null || userEmail.isEmpty()) {
            Toast.makeText(this, "Error: User email lost. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving to Database...");

        String token = "Bearer " + TokenManager.getToken(this);
        AuthViewModel authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // FIRE THE SUPABASE UPDATE
        authViewModel.completeUserProfile(studentNumber, phone, gender, faculty, token, userEmail).observe(this, result -> {
            if ("SUCCESS".equals(result)) {

                // ==========================================================
                // ✨ NEW: Tag the user in OneSignal as a Student! ✨
                // ==========================================================
                OneSignal.getUser().addTag("role", "student");

                Toast.makeText(this, "Profile secured!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(CompleteProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            } else {
                btnSaveProfile.setEnabled(true);
                btnSaveProfile.setText("Save & Continue");
                Toast.makeText(this, "Failed to save: " + result, Toast.LENGTH_LONG).show();
            }
        });
    }
}