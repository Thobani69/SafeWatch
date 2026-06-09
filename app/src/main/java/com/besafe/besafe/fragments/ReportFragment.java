package com.besafe.besafe.fragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.besafe.besafe.R;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.ReportRequest;
import com.besafe.besafe.utils.TokenManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportFragment extends Fragment {

    private Spinner spinnerCategory;
    private EditText etDescription;
    private ImageView ivPhotoPreview;
    private Button btnTakePhoto, btnSubmitReport;
    private CheckBox cbAnonymous;

    private Bitmap capturedImageBitmap = null;

    private FusedLocationProviderClient fusedLocationClient;
    private SupabaseService apiService;

    // The Camera Launcher
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    if (extras != null) {
                        capturedImageBitmap = (Bitmap) extras.get("data");
                        ivPhotoPreview.setVisibility(View.VISIBLE);
                        ivPhotoPreview.setImageBitmap(capturedImageBitmap);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        spinnerCategory = view.findViewById(R.id.spinner_category);
        etDescription = view.findViewById(R.id.et_description);
        ivPhotoPreview = view.findViewById(R.id.iv_photo_preview);
        btnTakePhoto = view.findViewById(R.id.btn_take_photo);
        btnSubmitReport = view.findViewById(R.id.btn_submit_report);
        cbAnonymous = view.findViewById(R.id.cb_anonymous);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        apiService = ApiClient.getClient().create(SupabaseService.class);

        String[] categories = {"Facilities / Maintenance", "Suspicious Activity", "Theft / Lost Property", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);

        btnTakePhoto.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        btnSubmitReport.setOnClickListener(v -> startSubmissionProcess());

        return view;
    }

    private void startSubmissionProcess() {
        String description = etDescription.getText().toString().trim();
        if (description.isEmpty()) {
            Toast.makeText(getContext(), "Please provide a description", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitReport.setEnabled(false);
        btnSubmitReport.setText("Getting Location...");

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getContext(), "Location permission required", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                if (capturedImageBitmap != null) {
                    uploadImageToStorage(location.getLatitude(), location.getLongitude());
                } else {
                    saveReportToDatabase(location.getLatitude(), location.getLongitude(), null);
                }
            } else {
                Toast.makeText(getContext(), "Could not find your location.", Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void uploadImageToStorage(double lat, double lon) {
        btnSubmitReport.setText("Uploading Photo...");
        String token = "Bearer " + TokenManager.getToken(requireContext());

        try {
            File imageFile = new File(requireContext().getCacheDir(), "report_img_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(imageFile);
            capturedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush(); fos.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
            String uniqueFileName = UUID.randomUUID().toString() + ".jpg";

            apiService.uploadReportImage(token, "image/jpeg", uniqueFileName, requestFile).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        String finalImageUrl = "report_images/" + uniqueFileName;
                        saveReportToDatabase(lat, lon, finalImageUrl);
                    } else {
                        try {
                            String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown Error";
                            Toast.makeText(getContext(), "Upload Failed: " + errorMsg, Toast.LENGTH_LONG).show();
                        } catch (IOException e) {
                            Toast.makeText(getContext(), "Upload Failed", Toast.LENGTH_SHORT).show();
                        }
                        resetButton();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    resetButton();
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
            resetButton();
        }
    }

    private void saveReportToDatabase(double lat, double lon, String imageUrl) {
        btnSubmitReport.setText("Saving Report...");
        String token = "Bearer " + TokenManager.getToken(requireContext());

        String currentUserId = TokenManager.getUserId(requireContext());
        if (currentUserId != null && currentUserId.isEmpty()) {
            currentUserId = null;
        }

        // ✨ NEW: Grab the email from the phone's memory!
        String userEmail = TokenManager.getEmail(requireContext());

        String category = spinnerCategory.getSelectedItem().toString();
        String description = etDescription.getText().toString().trim();
        boolean isAnonymous = cbAnonymous.isChecked();

        // ✨ UPDATE: If they checked anonymous, send "Anonymous", otherwise send their email!
        String finalEmailToSend = isAnonymous ? "Anonymous" : userEmail;

        // ✨ UPDATE: Pass finalEmailToSend as the very last parameter!
        ReportRequest request = new ReportRequest(category, description, lat, lon, imageUrl, isAnonymous, currentUserId, finalEmailToSend);

        apiService.submitReport(token, request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "✅ Report Submitted Successfully!", Toast.LENGTH_LONG).show();
                    etDescription.setText("");
                    ivPhotoPreview.setVisibility(View.GONE);
                    capturedImageBitmap = null;
                    cbAnonymous.setChecked(false);
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown DB Error";
                        Toast.makeText(getContext(), "DB Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Failed to save report.", Toast.LENGTH_SHORT).show();
                    }
                }
                resetButton();
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnSubmitReport.setEnabled(true);
        btnSubmitReport.setText("Submit Report");
    }
}