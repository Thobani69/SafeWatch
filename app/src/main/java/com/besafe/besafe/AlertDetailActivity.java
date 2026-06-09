package com.besafe.besafe;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.Alert;
import com.besafe.besafe.models.UpdateAlertRequest;
import com.besafe.besafe.utils.TokenManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.card.MaterialCardView;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertDetailActivity extends AppCompatActivity implements OnMapReadyCallback {

    private String alertId, currentStatus, userRole, reporterEmail;
    private double alertLat, alertLon;
    private LatLng studentLocation;

    private TextView tvStatus;
    private Button btnDispatch, btnResolve;
    private View bottomControlsCard;
    private GoogleMap mMap;

    // ✅ FIXED: declared as MaterialCardView (not LinearLayout) to match the XML
    private MaterialCardView studentProfileCard;
    private TextView tvStudentName, tvStudentEmail, tvStudentPhone, tvStudentNumber;
    private ImageView ivStudentAvatar;

    private SupabaseService apiService;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback guardLocationCallback;

    private android.os.Handler heartbeatHandler = new android.os.Handler();
    private Runnable heartbeatRunnable;
    private Marker guardMarker;
    private boolean hasAnimatedToGuard = false;

    private static final String SUPABASE_STORAGE_BASE_URL =
            "https://xbmitdvkxbpinghvpgzv.supabase.co/storage/v1/object/public/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);

        alertId       = getIntent().getStringExtra("ALERT_ID");
        alertLat      = getIntent().getDoubleExtra("ALERT_LAT", 0.0);
        alertLon      = getIntent().getDoubleExtra("ALERT_LON", 0.0);
        currentStatus = getIntent().getStringExtra("ALERT_STATUS");
        reporterEmail = getIntent().getStringExtra("REPORTER_EMAIL");
        userRole      = TokenManager.getRole(this);
        studentLocation = new LatLng(alertLat, alertLon);

        tvStatus           = findViewById(R.id.tv_alert_status);
        btnDispatch        = findViewById(R.id.btn_dispatch);
        btnResolve         = findViewById(R.id.btn_resolve);
        bottomControlsCard = findViewById(R.id.bottom_controls_card);

        // Student profile card — hidden until data loads
        studentProfileCard = findViewById(R.id.student_profile_card);
        tvStudentName      = findViewById(R.id.tv_student_name);
        tvStudentEmail     = findViewById(R.id.tv_student_email);
        tvStudentPhone     = findViewById(R.id.tv_student_phone);
        tvStudentNumber    = findViewById(R.id.tv_student_number);
        ivStudentAvatar    = findViewById(R.id.iv_student_avatar);

        // Start hidden — shown only when profile data has loaded
        if (studentProfileCard != null) studentProfileCard.setVisibility(View.GONE);

        apiService          = ApiClient.getClient().create(SupabaseService.class);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        tvStatus.setText("STATUS: " + currentStatus);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.detail_map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        // Role-based access control
        if ("RESOLVED".equals(currentStatus)) {
            bottomControlsCard.setVisibility(View.GONE);
            tvStatus.setText("STATUS: RESOLVED (ARCHIVED)");
            tvStatus.setTextColor(Color.parseColor("#198754"));

        } else if ("student".equals(userRole)) {
            bottomControlsCard.setVisibility(View.GONE);
            tvStatus.setText("WAITING FOR SECURITY UPDATES...");
            startStudentReassuranceLoop();

        } else {
            // Security guard — show controls and load student profile
            bottomControlsCard.setVisibility(View.VISIBLE);
            setupSecurityControls();

            // Fetch student profile to show in the card
            if (reporterEmail != null && !reporterEmail.isEmpty()) {
                fetchStudentProfile(reporterEmail);
            } else if (alertId != null) {
                fetchAlertThenProfile();
            }
        }
    }

    // ── Security controls ─────────────────────────────────────────────────────
    private void setupSecurityControls() {
        if ("DISPATCHED".equals(currentStatus)) {
            lockDispatchButton();
            startBroadcastingGuardLocation();
        }

        btnDispatch.setOnClickListener(v -> {
            updateStatusInDatabase("DISPATCHED", null);
            lockDispatchButton();
            startBroadcastingGuardLocation();
        });

        btnResolve.setOnClickListener(v -> {
            updateStatusInDatabase("RESOLVED", null);
            stopBroadcastingGuardLocation();
        });
    }

    private void lockDispatchButton() {
        btnDispatch.setEnabled(false);
        btnDispatch.setText("UNIT DISPATCHED - TRACKING ACTIVE");
        btnDispatch.setBackgroundColor(Color.parseColor("#0D6EFD"));
    }

    // ── Fetch alert first to get reporter email ───────────────────────────────
    private void fetchAlertThenProfile() {
        String token = "Bearer " + TokenManager.getToken(this);
        apiService.getActiveAlerts(token).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Alert a : response.body()) {
                        if (alertId.equals(a.getId())) {
                            String email = a.getReporterEmail();
                            if (email != null && !email.isEmpty()) {
                                fetchStudentProfile(email);
                            }
                            break;
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {}
        });
    }

    // ── Fetch student profile and populate card ───────────────────────────────
    private void fetchStudentProfile(String email) {
        String token = "Bearer " + TokenManager.getToken(this);

        apiService.getUserProfileByEmail(token, "*", "eq." + email)
                .enqueue(new Callback<JsonArray>() {
                    @Override
                    public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && response.body().size() > 0) {

                            JsonObject user = response.body().get(0).getAsJsonObject();

                            String firstName  = getStr(user, "first_name",     "Unknown");
                            String lastName   = getStr(user, "last_name",      "");
                            String phone      = getStr(user, "phone",          "Not provided");
                            String studentNo  = getStr(user, "student_number", "");
                            String avatarUrl  = getStr(user, "avatar_url",     "");

                            runOnUiThread(() -> {
                                // Show the card now that we have data
                                if (studentProfileCard != null)
                                    studentProfileCard.setVisibility(View.VISIBLE);

                                if (tvStudentName != null)
                                    tvStudentName.setText(firstName + " " + lastName);
                                if (tvStudentEmail != null)
                                    tvStudentEmail.setText(email);
                                if (tvStudentPhone != null)
                                    tvStudentPhone.setText("📞 " + phone);
                                if (tvStudentNumber != null && !studentNo.isEmpty())
                                    tvStudentNumber.setText("Student #" + studentNo);

                                // ✅ Load avatar with Glide
                                // Shows a person placeholder while loading and on error
                                if (ivStudentAvatar != null) {
                                    if (!avatarUrl.isEmpty()) {
                                        Glide.with(AlertDetailActivity.this)
                                                .load(SUPABASE_STORAGE_BASE_URL + avatarUrl)
                                                .circleCrop()
                                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                                .placeholder(android.R.drawable.ic_menu_myplaces)
                                                .error(android.R.drawable.ic_menu_myplaces)
                                                .into(ivStudentAvatar);
                                    } else {
                                        // No avatar set — show a clean person icon
                                        ivStudentAvatar.setImageResource(
                                                android.R.drawable.ic_menu_myplaces);
                                        ivStudentAvatar.setColorFilter(
                                                Color.parseColor("#DC3545"));
                                    }
                                }
                            });
                        }
                    }

                    @Override
                    public void onFailure(Call<JsonArray> call, Throwable t) {
                        // Profile fetch failed — show card with just the email
                        runOnUiThread(() -> {
                            if (studentProfileCard != null)
                                studentProfileCard.setVisibility(View.VISIBLE);
                            if (tvStudentName != null)
                                tvStudentName.setText("Student");
                            if (tvStudentEmail != null)
                                tvStudentEmail.setText(email);
                            if (ivStudentAvatar != null)
                                ivStudentAvatar.setImageResource(
                                        android.R.drawable.ic_menu_myplaces);
                        });
                    }
                });
    }

    private String getStr(JsonObject obj, String key, String fallback) {
        return (obj.has(key) && !obj.get(key).isJsonNull())
                ? obj.get(key).getAsString()
                : fallback;
    }

    // ── Map ───────────────────────────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.addMarker(new MarkerOptions()
                .position(studentLocation)
                .title("Emergency Origin")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(studentLocation, 18f));
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    // ── Guard location broadcasting ───────────────────────────────────────────
    private void startBroadcastingGuardLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            return;

        LocationRequest request = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateDistanceMeters(2f).build();

        guardLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location guardLocation = result.getLastLocation();
                if (guardLocation != null) {
                    float[] r = new float[1];
                    Location.distanceBetween(
                            guardLocation.getLatitude(), guardLocation.getLongitude(),
                            alertLat, alertLon, r);
                    tvStatus.setText("DISTANCE TO STUDENT: " + Math.round(r[0]) + "m");
                    updateStatusInDatabase("DISPATCHED", guardLocation);
                }
            }
        };
        fusedLocationClient.requestLocationUpdates(request, guardLocationCallback, null);
    }

    private void stopBroadcastingGuardLocation() {
        if (fusedLocationClient != null && guardLocationCallback != null)
            fusedLocationClient.removeLocationUpdates(guardLocationCallback);
    }

    private void updateStatusInDatabase(String newStatus, Location guardLocation) {
        String token = "Bearer " + TokenManager.getToken(this);
        UpdateAlertRequest request = new UpdateAlertRequest(newStatus);
        if (guardLocation != null)
            request.setGuardLocation(guardLocation.getLatitude(), guardLocation.getLongitude());

        apiService.updateAlertStatus(token, "eq." + alertId, request)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (response.isSuccessful() && "RESOLVED".equals(newStatus)) finish();
                    }
                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {}
                });
    }

    // ── Student reassurance loop ──────────────────────────────────────────────
    private void startStudentReassuranceLoop() {
        String token = "Bearer " + TokenManager.getToken(this);
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                apiService.getMyLatestAlert(token).enqueue(new Callback<List<Alert>>() {
                    @Override
                    public void onResponse(Call<List<Alert>> call,
                                           Response<List<Alert>> response) {
                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {

                            Alert alert = response.body().get(0);

                            if ("DISPATCHED".equals(alert.getStatus())
                                    && alert.getGuardLatitude() != null) {

                                LatLng guardLatLng = new LatLng(
                                        alert.getGuardLatitude(),
                                        alert.getGuardLongitude());

                                float[] results = new float[1];
                                Location.distanceBetween(
                                        studentLocation.latitude,
                                        studentLocation.longitude,
                                        guardLatLng.latitude,
                                        guardLatLng.longitude, results);

                                tvStatus.setText("SECURITY DISPATCHED: "
                                        + Math.round(results[0]) + "m AWAY");
                                tvStatus.setTextColor(Color.parseColor("#0D6EFD"));

                                if (mMap != null) {
                                    if (guardMarker == null) {
                                        guardMarker = mMap.addMarker(new MarkerOptions()
                                                .position(guardLatLng)
                                                .title("Security Guard")
                                                .icon(BitmapDescriptorFactory.defaultMarker(
                                                        BitmapDescriptorFactory.HUE_BLUE)));
                                    } else {
                                        guardMarker.setPosition(guardLatLng);
                                    }

                                    if (!hasAnimatedToGuard) {
                                        LatLngBounds.Builder b = new LatLngBounds.Builder();
                                        b.include(studentLocation);
                                        b.include(guardLatLng);
                                        mMap.animateCamera(
                                                CameraUpdateFactory.newLatLngBounds(
                                                        b.build(), 150));
                                        hasAnimatedToGuard = true;
                                    }
                                }

                            } else if ("RESOLVED".equals(alert.getStatus())) {
                                tvStatus.setText("INCIDENT RESOLVED");
                                tvStatus.setTextColor(Color.parseColor("#198754"));
                                if (guardMarker != null) guardMarker.remove();
                                heartbeatHandler.removeCallbacks(heartbeatRunnable);
                                return;
                            }
                        }
                        heartbeatHandler.postDelayed(heartbeatRunnable, 3000);
                    }

                    @Override
                    public void onFailure(Call<List<Alert>> call, Throwable t) {
                        heartbeatHandler.postDelayed(heartbeatRunnable, 3000);
                    }
                });
            }
        };
        heartbeatHandler.post(heartbeatRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopBroadcastingGuardLocation();
        if (heartbeatHandler != null && heartbeatRunnable != null)
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }
}