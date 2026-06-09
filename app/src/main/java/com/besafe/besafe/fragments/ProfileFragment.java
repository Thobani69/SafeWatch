package com.besafe.besafe.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.besafe.besafe.R;
import com.besafe.besafe.activities.EditProfileActivity;
import com.besafe.besafe.activities.LoginActivity;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.utils.TokenManager;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ImageView      ivAvatar;
    private TextView       tvName, tvEmail, tvPhone;
    private MaterialButton btnLogout, btnEditProfile, btnSetPin;
    private LinearLayout   layoutSosHistory, layoutPagination;
    private SupabaseService apiService;

    private String loadedFirstName = "";
    private String loadedLastName  = "";
    private String loadedPhone     = "";

    private List<com.besafe.besafe.models.Alert> allAlerts;
    private int currentPage = 0;
    private static final int ITEMS_PER_PAGE = 10;

    private static final String SUPABASE_STORAGE_BASE_URL =
            "https://xbmitdvkxbpinghvpgzv.supabase.co/storage/v1/object/public/";

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    ivAvatar.setImageURI(uri);
                    ivAvatar.setImageTintList(null);
                    uploadProfilePicture(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        ivAvatar       = view.findViewById(R.id.iv_avatar);
        tvName         = view.findViewById(R.id.tv_profile_name);
        tvEmail        = view.findViewById(R.id.tv_profile_email);
        tvPhone        = view.findViewById(R.id.tv_profile_phone);
        btnLogout      = view.findViewById(R.id.btn_logout);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnSetPin      = view.findViewById(R.id.btn_set_pin);
        layoutSosHistory = view.findViewById(R.id.layout_sos_history);
        layoutPagination = view.findViewById(R.id.layout_pagination);

        apiService = ApiClient.getClient().create(SupabaseService.class);

        tvName.setText("Loading...");
        tvEmail.setText("Loading email...");

        ivAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditProfileActivity.class);
            intent.putExtra("FIRST_NAME", loadedFirstName);
            intent.putExtra("LAST_NAME",  loadedLastName);
            intent.putExtra("PHONE",      loadedPhone);
            startActivity(intent);
        });

        // Set / change SafeWalk PIN
        if (btnSetPin != null) {
            String currentPin = TokenManager.getSafeWalkPin(requireContext());
            btnSetPin.setText("SafeWalk PIN: " + currentPin);
            btnSetPin.setOnClickListener(v -> showSetPinDialog());
        }

        btnLogout.setOnClickListener(v -> performLogout());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchUserProfile();
        fetchSosHistory();
    }

    // ── Fetch profile ─────────────────────────────────────────────────────────
    private void fetchUserProfile() {
        String token = "Bearer " + TokenManager.getToken(requireContext());
        String email = TokenManager.getEmail(requireContext());
        tvEmail.setText(email);

        apiService.getUserProfileByEmail(token, "*", "eq." + email)
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                        if (response.isSuccessful() && response.body() != null
                                && response.body().size() > 0) {
                            JsonObject user = response.body().get(0).getAsJsonObject();

                            loadedFirstName = getStr(user, "first_name", "Student");
                            loadedLastName  = getStr(user, "last_name",  "");
                            loadedPhone     = getStr(user, "phone",      "No Phone Set");

                            tvName.setText(loadedFirstName + " " + loadedLastName);
                            tvPhone.setText(loadedPhone);

                            String avatarUrl = getStr(user, "avatar_url", "");
                            if (!avatarUrl.isEmpty()) {
                                ivAvatar.setImageTintList(null);
                                Glide.with(requireContext())
                                        .load(SUPABASE_STORAGE_BASE_URL + avatarUrl)
                                        .circleCrop()
                                        .into(ivAvatar);
                            }
                        }
                    }
                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ── SOS history ───────────────────────────────────────────────────────────
    private void fetchSosHistory() {
        if (layoutSosHistory == null) return;
        String token = "Bearer " + TokenManager.getToken(requireContext());

        apiService.getAlerts(token).enqueue(
                new Callback<List<com.besafe.besafe.models.Alert>>() {
                    @Override
                    public void onResponse(Call<List<com.besafe.besafe.models.Alert>> call,
                                           Response<List<com.besafe.besafe.models.Alert>> response) {
                        if (!isAdded()) return;

                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {

                            String myEmail = TokenManager.getEmail(requireContext());

                            // Filter only this user's alerts
                            allAlerts = new ArrayList<>();
                            for (com.besafe.besafe.models.Alert alert : response.body()) {
                                if (myEmail != null && myEmail.equals(alert.getReporterEmail())) {
                                    allAlerts.add(alert);
                                }
                            }

                            currentPage = 0;
                            showPage();
                        } else {
                            showEmptyHistory();
                        }
                    }
                    @Override
                    public void onFailure(
                            Call<List<com.besafe.besafe.models.Alert>> call, Throwable t) {
                        showEmptyHistory();
                    }
                });
    }

    private void showPage() {
        if (!isAdded() || allAlerts == null || allAlerts.isEmpty()) {
            showEmptyHistory();
            return;
        }

        layoutSosHistory.removeAllViews();

        int totalPages = (int) Math.ceil((double) allAlerts.size() / ITEMS_PER_PAGE);
        int start = currentPage * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, allAlerts.size());

        for (int i = start; i < end; i++) {
            com.besafe.besafe.models.Alert alert = allAlerts.get(i);

            MaterialCardView card = new MaterialCardView(requireContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, 16);
            card.setLayoutParams(lp);
            card.setCardElevation(2f);
            card.setRadius(16f);

            // Status colour
            String status = alert.getStatus() != null
                    ? alert.getStatus() : "UNKNOWN";
            int statusColor = "RESOLVED".equals(status)
                    ? Color.parseColor("#D4EDDA")
                    : Color.parseColor("#FFF3CD");
            card.setCardBackgroundColor(statusColor);

            LinearLayout inner = new LinearLayout(requireContext());
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setPadding(32, 24, 32, 24);

            TextView tvDate = new TextView(requireContext());
            tvDate.setText(utcToSast(alert.getCreatedAt()));
            tvDate.setTextSize(12f);
            tvDate.setTextColor(Color.GRAY);

            TextView tvStatus = new TextView(requireContext());
            tvStatus.setText(status);
            tvStatus.setTextSize(14f);
            tvStatus.setTypeface(null, android.graphics.Typeface.BOLD);
            tvStatus.setTextColor("RESOLVED".equals(status)
                    ? Color.parseColor("#155724")
                    : Color.parseColor("#856404"));

            TextView tvLoc = new TextView(requireContext());
            tvLoc.setText("Lat: " + alert.getLatitude()
                    + "  Lon: " + alert.getLongitude());
            tvLoc.setTextSize(12f);
            tvLoc.setTextColor(Color.GRAY);

            inner.addView(tvDate);
            inner.addView(tvStatus);
            inner.addView(tvLoc);
            card.addView(inner);
            layoutSosHistory.addView(card);
        }

        // Update pagination controls
        updatePagination(totalPages);
    }

    private void updatePagination(int totalPages) {
        if (layoutPagination == null) return;
        layoutPagination.removeAllViews();

        if (totalPages <= 1) {
            layoutPagination.setVisibility(View.GONE);
            return;
        }
        layoutPagination.setVisibility(View.VISIBLE);

        // Previous Button
        MaterialButton btnPrev = new MaterialButton(requireContext());
        btnPrev.setText("Previous");
        btnPrev.setTextSize(14f);
        btnPrev.setEnabled(currentPage > 0);
        btnPrev.setTextColor(currentPage > 0 ? Color.parseColor("#0D6EFD") : Color.GRAY);
        btnPrev.setBackgroundColor(Color.TRANSPARENT);
        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                showPage();
            }
        });

        // Page Indicator
        TextView tvPageIndicator = new TextView(requireContext());
        tvPageIndicator.setText("Page " + (currentPage + 1) + " of " + totalPages);
        tvPageIndicator.setTextSize(14f);
        tvPageIndicator.setTextColor(Color.parseColor("#495057"));
        tvPageIndicator.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams pageLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tvPageIndicator.setLayoutParams(pageLp);

        // Next Button
        MaterialButton btnNext = new MaterialButton(requireContext());
        btnNext.setText("Next");
        btnNext.setTextSize(14f);
        btnNext.setEnabled(currentPage < totalPages - 1);
        btnNext.setTextColor(currentPage < totalPages - 1 ? Color.parseColor("#0D6EFD") : Color.GRAY);
        btnNext.setBackgroundColor(Color.TRANSPARENT);
        btnNext.setOnClickListener(v -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                showPage();
            }
        });

        layoutPagination.addView(btnPrev);
        layoutPagination.addView(tvPageIndicator);
        layoutPagination.addView(btnNext);
    }

    private void showEmptyHistory() {
        if (!isAdded()) return;
        layoutSosHistory.removeAllViews();
        if (layoutPagination != null) {
            layoutPagination.setVisibility(View.GONE);
        }

        TextView empty = new TextView(requireContext());
        empty.setText("No SOS alerts in your history.");
        empty.setTextSize(13f);
        empty.setTextColor(Color.GRAY);
        layoutSosHistory.addView(empty);
    }

    private String utcToSast(String utc) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            in.setTimeZone(TimeZone.getTimeZone("UTC"));
            String clean = utc.length() > 19 ? utc.substring(0, 19) : utc;
            Date d = in.parse(clean);
            SimpleDateFormat out = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
            out.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));
            return out.format(d) + " SAST";
        } catch (Exception e) {
            return utc != null && utc.length() >= 10 ? utc.substring(0, 10) : "";
        }
    }

    // ── Set SafeWalk PIN dialog ───────────────────────────────────────────────
    private void showSetPinDialog() {
        TextInputEditText input = new TextInputEditText(requireContext());
        input.setHint("Enter a 4-digit PIN");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setPadding(48, 32, 48, 32);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Set SafeWalk PIN")
                .setMessage("This PIN cancels the SafeWalk safety timer. Choose something you will remember.")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String pin = input.getText() != null
                            ? input.getText().toString().trim() : "";
                    if (pin.length() < 4) {
                        Toast.makeText(getContext(),
                                "PIN must be at least 4 digits.", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    TokenManager.saveSafeWalkPin(requireContext(), pin);
                    if (btnSetPin != null) btnSetPin.setText("SafeWalk PIN: " + pin);
                    Toast.makeText(getContext(), "PIN saved!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Avatar upload (uses avatars bucket) ───────────────────────────────────
    private void uploadProfilePicture(Uri imageUri) {
        Toast.makeText(getContext(), "Uploading photo...", Toast.LENGTH_SHORT).show();
        String token = "Bearer " + TokenManager.getToken(requireContext());
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(imageUri);
            File f = new File(requireContext().getCacheDir(),
                    "avatar_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(f);
            byte[] buf = new byte[1024]; int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            fos.flush(); fos.close(); is.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), f);
            String uniqueName = UUID.randomUUID().toString() + ".jpg";

            // Uses the dedicated avatars bucket endpoint
            apiService.uploadAvatar(token, "image/jpeg", uniqueName, requestFile)
                    .enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call,
                                               Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                saveAvatarUrlToDatabase("avatars/" + uniqueName);
                            } else {
                                Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(getContext(), "Network error", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveAvatarUrlToDatabase(String imageUrl) {
        String token = "Bearer " + TokenManager.getToken(requireContext());
        String email = TokenManager.getEmail(requireContext());
        Map<String, String> updates = new HashMap<>();
        updates.put("avatar_url", imageUrl);
        apiService.updateUserProfileByEmail(token, "eq." + email, updates)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful())
                            Toast.makeText(getContext(), "Profile picture saved!", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    private void performLogout() {
        TokenManager.clearToken(requireContext());
        Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private String getStr(JsonObject obj, String key, String fallback) {
        return (obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString() : fallback;
    }
}