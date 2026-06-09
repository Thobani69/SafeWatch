package com.besafe.besafe.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.besafe.besafe.R;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.utils.TokenManager;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etEditName, etEditPhone;
    private MaterialButton btnSaveEdits;
    private MaterialToolbar toolbar;
    private SupabaseService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        etEditName = findViewById(R.id.etEditName);
        etEditPhone = findViewById(R.id.etEditPhone);
        btnSaveEdits = findViewById(R.id.btnSaveEdits);
        toolbar = findViewById(R.id.toolbarEditProfile);

        apiService = ApiClient.getClient().create(SupabaseService.class);

        // 1. Pre-fill the data passed from the Profile Fragment
        String currentFirstName = getIntent().getStringExtra("FIRST_NAME");
        String currentLastName = getIntent().getStringExtra("LAST_NAME");
        String currentPhone = getIntent().getStringExtra("PHONE");

        if (currentFirstName != null && currentLastName != null) {
            etEditName.setText(currentFirstName + " " + currentLastName);
        }
        if (currentPhone != null) {
            etEditPhone.setText(currentPhone);
        }

        toolbar.setNavigationOnClickListener(v -> finish());

        // 2. Save logic
        btnSaveEdits.setOnClickListener(v -> saveProfileUpdates());
    }

    private void saveProfileUpdates() {
        String fullName = etEditName.getText().toString().trim();
        String newPhone = etEditPhone.getText().toString().trim();

        if (fullName.isEmpty() || newPhone.isEmpty()) {
            Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Split the full name back into first and last name safely
        String firstName = fullName;
        String lastName = "";
        if (fullName.contains(" ")) {
            firstName = fullName.substring(0, fullName.indexOf(" "));
            lastName = fullName.substring(fullName.indexOf(" ") + 1);
        }

        btnSaveEdits.setEnabled(false);
        btnSaveEdits.setText("Saving...");

        String token = "Bearer " + TokenManager.getToken(this);
        String email = TokenManager.getEmail(this);

        // Package the updates
        Map<String, String> updates = new HashMap<>();
        updates.put("first_name", firstName);
        updates.put("last_name", lastName);
        updates.put("phone", newPhone);

        // Send to Supabase
        apiService.updateUserProfileByEmail(token, "eq." + email, updates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to Profile screen
                } else {
                    btnSaveEdits.setEnabled(true);
                    btnSaveEdits.setText("Save Changes");
                    Toast.makeText(EditProfileActivity.this, "Failed to save updates.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                btnSaveEdits.setEnabled(true);
                btnSaveEdits.setText("Save Changes");
                Toast.makeText(EditProfileActivity.this, "Network error.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}