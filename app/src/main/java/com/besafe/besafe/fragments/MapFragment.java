package com.besafe.besafe.fragments;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.besafe.besafe.R;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.RiskZone;
import com.besafe.besafe.utils.HaversineUtil;
import com.besafe.besafe.utils.ProximityAlertManager;
import com.besafe.besafe.utils.RiskScoreCalculator;
import com.besafe.besafe.utils.StudentActivityReceiver;
import com.besafe.besafe.utils.TokenManager;
import com.besafe.besafe.viewmodels.AlertViewModel;
import com.google.android.gms.location.ActivityRecognition;
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
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "MapFragment";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private AlertViewModel alertViewModel;

    private android.os.Handler heartbeatHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable heartbeatRunnable;
    private LatLng currentStudentLocation;
    private Marker guardMarker;
    private boolean hasAnimatedToGuard = false;

    // UI
    private TextView tvZoneStatus;
    private Button btnSOS, btnShareLocation;

    // Danger zone bottom sheet
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private MaterialCardView cardThreatLevel;
    private TextView tvThreatLevel, tvZoneName, tvIncidentCount, tvDominantType, tvZoneTrend;
    private Button btnSafeRoute, btnSafetyTimer;



    // SafeWalk setup sheet
    private BottomSheetBehavior<View> safeWalkSheetBehavior;
    private Slider sliderTimer;
    private TextView tvDialTime;
    private android.widget.AutoCompleteTextView actvDestination;
    private Button btnStartSafeWalk;

    // Active timer widget
    private MaterialCardView cardActiveSafeWalk;
    private TextView tvActiveDestination, tvActiveCountdown;
    private Button btnImSafe;
    private android.os.CountDownTimer uiCountdownTimer;

    // Route drawing
    private List<Polyline> alternativePolylines = new ArrayList<>();
    private Polyline selectedPolyline;
    private Map<String, LatLng> campusDestinations = new HashMap<>();

    // Risk zones
    private List<RiskZone> activeRiskZonesList = new ArrayList<>();

    // AI proximity + activity
    private ProximityAlertManager proximityAlertManager;
    private android.content.BroadcastReceiver activityUpdateReceiver;

    // Wellness check state
    private boolean isWellnessCheckActive = false;
    private android.os.CountDownTimer wellnessTimer;

    // Adaptive SafeWalk state
    private boolean isTimerPausedForCheckIn = false;
    private long pausedTimeRemainingMs = 0;
    private android.os.CountDownTimer checkInCountdownTimer;

    // Campus constants
    private static final LatLng[] CAMPUS_ZONE_VERTICES = {
            new LatLng(-29.8558, 31.0025), new LatLng(-29.8515, 31.0030),
            new LatLng(-29.8500, 31.0110), new LatLng(-29.8520, 31.0140),
            new LatLng(-29.8545, 31.0130), new LatLng(-29.8562, 31.0065)
    };
    private static final LatLng STEVE_BIKO        = new LatLng(-29.8541, 31.0043);
    private static final LatLng RITSON            = new LatLng(-29.8536, 31.0065);
    private static final LatLng ML_SULTAN         = new LatLng(-29.8521, 31.0118);
    private static final LatLng FAKE_TEST_LOCATION = new LatLng(-29.8550, 31.0040);

    // ── onCreateView ──────────────────────────────────────────────────────────
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_map, container, false);

        tvZoneStatus     = view.findViewById(R.id.tv_zone_status);
        btnSOS           = view.findViewById(R.id.btn_sos);
        btnShareLocation = view.findViewById(R.id.btn_share_location);

        fusedLocationClient   = LocationServices.getFusedLocationProviderClient(requireActivity());
        alertViewModel        = new ViewModelProvider(this).get(AlertViewModel.class);
        proximityAlertManager = new ProximityAlertManager(requireContext());

        btnSOS.setOnClickListener(v -> sendSOSAlert());
        btnShareLocation.setOnClickListener(v -> shareLocationWithSecurity());

        setupBottomSheet(view);
        setupSafeWalkEngine(view);
        setupAIReceiver();

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.google_map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        return view;
    }

    // ── ML Kit Activity Recognition setup ─────────────────────────────────────
    private void setupAIReceiver() {
        // Listen for activity broadcasts from StudentActivityReceiver
        activityUpdateReceiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(android.content.Context context, android.content.Intent intent) {
                String activityType = intent.getStringExtra(StudentActivityReceiver.EXTRA_ACTIVITY_TYPE);
                int confidence      = intent.getIntExtra(StudentActivityReceiver.EXTRA_CONFIDENCE, 0);
                if (activityType != null && confidence >= 60) {
                    processAIActivity(activityType);
                }
            }
        };
        LocalBroadcastManager.getInstance(requireContext())
                .registerReceiver(activityUpdateReceiver,
                        new android.content.IntentFilter(StudentActivityReceiver.ACTION_ACTIVITY_UPDATE));

        // Request ACTIVITY_RECOGNITION permission on Android 10+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(),
                        new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 1001);
                return; // Will retry next time fragment opens after permission granted
            }
        }

        startGoogleActivityRecognition();
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private void startGoogleActivityRecognition() {
        Intent intent = new Intent(requireContext(), StudentActivityReceiver.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }

        // Check every 3 seconds
        ActivityRecognition.getClient(requireContext())
                .requestActivityUpdates(3000, pendingIntent)
                .addOnSuccessListener(v -> Log.d(TAG, "ML Kit Activity Recognition started."))
                .addOnFailureListener(e -> Log.e(TAG, "ML Kit failed to start: " + e.getMessage()));
    }

    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    private void stopGoogleActivityRecognition() {
        Intent intent = new Intent(requireContext(), StudentActivityReceiver.class);
        PendingIntent pendingIntent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
        ActivityRecognition.getClient(requireContext()).removeActivityUpdates(pendingIntent);
    }

    private void processAIActivity(String activityType) {
        if (activeRiskZonesList == null || activeRiskZonesList.isEmpty()
                || currentStudentLocation == null) return;

        RiskZone nearest = RiskScoreCalculator.nearestZone(
                currentStudentLocation.latitude,
                currentStudentLocation.longitude,
                activeRiskZonesList);
        if (nearest == null) return;

        double distance = HaversineUtil.distanceMeters(
                currentStudentLocation.latitude, currentStudentLocation.longitude,
                nearest.getLatitude(), nearest.getLongitude());

        // AI Panic: student is running near a danger zone → escalate immediately
        if (StudentActivityReceiver.ACTIVITY_RUNNING.equals(activityType)
                && distance <= ProximityAlertManager.TIER_2_METERS) {
            Log.d(TAG, "AI: Running near risk zone — escalating check-in");
            triggerAdaptiveCheckIn(nearest.getZoneName());
        }

        // AI Wellness: student is stationary inside a danger zone
        if (StudentActivityReceiver.ACTIVITY_STATIONARY.equals(activityType)
                && distance <= ProximityAlertManager.TIER_3_METERS) {
            if (!isWellnessCheckActive) startWellnessCheck();
        } else {
            cancelWellnessCheck(); // moving again — cancel the wellness timer
        }
    }

    // ── Wellness check ────────────────────────────────────────────────────────
    // Fires if the student stands still inside a red zone for 60 seconds
    private void startWellnessCheck() {
        isWellnessCheckActive = true;
        Log.d(TAG, "Wellness check started — student stationary in danger zone");

        wellnessTimer = new android.os.CountDownTimer(60_000, 1000) {
            @Override public void onTick(long ms) {}

            @Override
            public void onFinish() {
                // 60 seconds of being still in a red zone with no interaction → check-in
                triggerAdaptiveCheckIn("Unknown location (stationary too long)");
                isWellnessCheckActive = false;
            }
        }.start();
    }

    private void cancelWellnessCheck() {
        if (isWellnessCheckActive && wellnessTimer != null) {
            wellnessTimer.cancel();
            isWellnessCheckActive = false;
        }
    }

    // ── Bottom sheet ──────────────────────────────────────────────────────────
    private void setupBottomSheet(View view) {
        View bottomSheet = view.findViewById(R.id.bottom_sheet_risk_zone);
        if (bottomSheet == null) return;

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        cardThreatLevel = view.findViewById(R.id.card_threat_level);
        tvThreatLevel   = view.findViewById(R.id.tv_threat_level);
        tvZoneName      = view.findViewById(R.id.tv_zone_name);
        tvIncidentCount = view.findViewById(R.id.tv_incident_count);
        tvDominantType  = view.findViewById(R.id.tv_dominant_type);
        tvZoneTrend     = view.findViewById(R.id.tv_zone_trend);
        btnSafeRoute    = view.findViewById(R.id.btn_safe_route);
        btnSafetyTimer  = view.findViewById(R.id.btn_safety_timer);

        btnSafeRoute.setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            if (safeWalkSheetBehavior != null) {
                safeWalkSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                Toast.makeText(getContext(), "Select a safe destination.", Toast.LENGTH_LONG).show();
            }
        });

        if (btnSafetyTimer != null) {
            btnSafetyTimer.setOnClickListener(v -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                if (safeWalkSheetBehavior != null)
                    safeWalkSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            });
        }
    }

    // ── SafeWalk engine ───────────────────────────────────────────────────────
    private void setupSafeWalkEngine(View view) {
        View safeWalkSheet = view.findViewById(R.id.bottom_sheet_safewalk);
        if (safeWalkSheet == null) return;

        safeWalkSheetBehavior = BottomSheetBehavior.from(safeWalkSheet);
        safeWalkSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

        sliderTimer         = view.findViewById(R.id.slider_timer);
        tvDialTime          = view.findViewById(R.id.tv_dial_time);
        actvDestination     = view.findViewById(R.id.actv_destination);
        btnStartSafeWalk    = view.findViewById(R.id.btn_start_safewalk);
        cardActiveSafeWalk  = view.findViewById(R.id.card_active_safewalk);
        tvActiveDestination = view.findViewById(R.id.tv_active_destination);
        tvActiveCountdown   = view.findViewById(R.id.tv_active_countdown);
        btnImSafe           = view.findViewById(R.id.btn_im_safe);

        campusDestinations.put("Steve Biko Main Library", new LatLng(-29.8541, 31.0043));
        campusDestinations.put("Ritson Parking Lot",      new LatLng(-29.8536, 31.0065));
        campusDestinations.put("ML Sultan Walkway",        new LatLng(-29.8521, 31.0118));
        campusDestinations.put("DUT Sports Centre",        new LatLng(-29.8510, 31.0090));
        campusDestinations.put("City Hospital Taxi Rank",  new LatLng(-29.8495, 31.0050));
        campusDestinations.put("Berea Centre Mall",        new LatLng(-29.8570, 31.0010));
        campusDestinations.put("Botanic Gardens Walk",     new LatLng(-29.8480, 31.0020));

        if (actvDestination != null) {
            String[] names = campusDestinations.keySet().toArray(new String[0]);
            android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_dropdown_item_1line, names);
            actvDestination.setAdapter(adapter);
        }

        if (sliderTimer != null) {
            sliderTimer.addOnChangeListener((slider, value, fromUser) -> {
                if (tvDialTime != null) tvDialTime.setText(String.valueOf(Math.max(1, Math.round(value))));
            });
        }

        if (btnStartSafeWalk != null) {
            btnStartSafeWalk.setOnClickListener(v -> {
                String dest = actvDestination != null ?
                        actvDestination.getText().toString().trim() : "";
                if (dest.isEmpty() || !campusDestinations.containsKey(dest)) {
                    Toast.makeText(getContext(), "Please select a valid destination.", Toast.LENGTH_SHORT).show();
                    return;
                }

                int minutes = (sliderTimer != null) ? Math.round(sliderTimer.getValue()) : 15;
                safeWalkSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);

                // ── FIX: startSafetyTimerService is now fully implemented below ──
                startSafetyTimerService(minutes);
                startActiveWidgetCountdown(dest, minutes);

                LatLng destCoords = campusDestinations.get(dest);
                LatLng origin     = (currentStudentLocation != null) ?
                        currentStudentLocation : FAKE_TEST_LOCATION;
                if (destCoords != null) fetchAndDrawRoute(origin, destCoords);
            });
        }

        if (btnImSafe != null) {
            btnImSafe.setOnClickListener(v -> {
                cancelSafeWalk();
                Toast.makeText(getContext(), "SafeWalk complete!", Toast.LENGTH_LONG).show();
            });
        }
    }

    private void cancelSafeWalk() {
        try {
            Intent cancelIntent = new Intent(requireContext(),
                    com.besafe.besafe.services.SafetyTimerService.class);
            cancelIntent.setAction("ACTION_CANCEL");
            requireContext().startService(cancelIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling safety timer service", e);
        }
        if (uiCountdownTimer != null)    uiCountdownTimer.cancel();
        if (cardActiveSafeWalk != null)  cardActiveSafeWalk.setVisibility(View.GONE);
        if (checkInCountdownTimer != null) checkInCountdownTimer.cancel();
        isTimerPausedForCheckIn = false;
        pausedTimeRemainingMs   = 0;
        clearAllRoutes();
    }

    // ── FIX: This method was empty — now it actually starts the service ───────
    private void startSafetyTimerService(int minutes) {
        try {
            Intent serviceIntent = new Intent(requireContext(),
                    com.besafe.besafe.services.SafetyTimerService.class);
            serviceIntent.putExtra("MINUTES", minutes);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                requireActivity().startForegroundService(serviceIntent);
            } else {
                requireActivity().startService(serviceIntent);
            }
            Log.d(TAG, "SafetyTimerService started for " + minutes + " minutes");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start SafetyTimerService: " + e.getMessage());
        }
    }

    // ── Countdown widget ──────────────────────────────────────────────────────
    private void startActiveWidgetCountdown(String destination, int minutes) {
        if (cardActiveSafeWalk != null)  cardActiveSafeWalk.setVisibility(View.VISIBLE);
        if (tvActiveDestination != null) tvActiveDestination.setText("Walking to: " + destination);
        if (uiCountdownTimer != null)    uiCountdownTimer.cancel();
        launchCountdown(minutes * 60 * 1000L);
    }

    private void launchCountdown(long timeInMillis) {
        uiCountdownTimer = new android.os.CountDownTimer(timeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (isTimerPausedForCheckIn) {
                    pausedTimeRemainingMs = millisUntilFinished;
                    cancel();
                    return;
                }
                int mins = (int) (millisUntilFinished / 1000) / 60;
                int secs = (int) (millisUntilFinished / 1000) % 60;
                if (tvActiveCountdown != null) {
                    tvActiveCountdown.setText(
                            String.format(Locale.getDefault(), "%02d:%02d", mins, secs));
                    tvActiveCountdown.setTextColor(
                            mins == 0 ? Color.parseColor("#DC3545")
                                    : Color.parseColor("#FFC107"));
                }
            }
            @Override
            public void onFinish() {
                if (cardActiveSafeWalk != null) cardActiveSafeWalk.setVisibility(View.GONE);
            }
        }.start();
    }

    // ── Adaptive check-in dialog ──────────────────────────────────────────────
    private void triggerAdaptiveCheckIn(String zoneName) {
        if (isTimerPausedForCheckIn) return; // already showing
        if (!isAdded() || getContext() == null) return;

        isTimerPausedForCheckIn = true;

        if (tvActiveCountdown != null) {
            tvActiveCountdown.setText("CHECK-IN!");
            tvActiveCountdown.setTextColor(Color.parseColor("#DC3545"));
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Risk Zone Alert — Are you safe?")
                .setMessage("You triggered a safety alert near " + zoneName
                        + ".\n\nYou have 60 seconds to confirm you are okay, "
                        + "or an SOS will be sent automatically.")
                .setCancelable(false)
                .setPositiveButton("I'm okay — resume timer", (dialog, which) -> {
                    dialog.dismiss();
                    resumeAfterCheckIn();
                })
                .setNegativeButton("Send SOS now", (dialog, which) -> {
                    dialog.dismiss();
                    sendSOSAlert();
                })
                .show();

        // Auto-SOS after 60 seconds of no response
        if (checkInCountdownTimer != null) checkInCountdownTimer.cancel();
        checkInCountdownTimer = new android.os.CountDownTimer(60_000, 1000) {
            @Override public void onTick(long ms) {}
            @Override
            public void onFinish() {
                if (isTimerPausedForCheckIn) {
                    Log.w(TAG, "Check-in timeout — auto-firing SOS");
                    sendSOSAlert();
                }
            }
        }.start();
    }

    private void resumeAfterCheckIn() {
        isTimerPausedForCheckIn = false;
        if (checkInCountdownTimer != null) checkInCountdownTimer.cancel();
        if (pausedTimeRemainingMs > 0) {
            launchCountdown(pausedTimeRemainingMs);
            pausedTimeRemainingMs = 0;
        }
    }

    // ── Map ready ─────────────────────────────────────────────────────────────
    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        addCampusMarkers();
        drawCampusZone();
        fitCameraToZone();
        startLocationUpdates();
        fetchRiskZones();
        mMap.setOnCircleClickListener(circle -> {
            RiskZone z = (RiskZone) circle.getTag();
            if (z != null) showZoneDetails(z);
        });
    }

    // ── Location update loop ──────────────────────────────────────────────────
    private void onUserLocationChanged(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        currentStudentLocation = userLatLng;
        boolean insideZone = isPointInsidePolygon(userLatLng, CAMPUS_ZONE_VERTICES);
        updateZoneStatusUI(insideZone, userLatLng);
        checkProximityToRiskZones(location.getLatitude(), location.getLongitude());
    }

    private void checkProximityToRiskZones(double lat, double lon) {
        if (activeRiskZonesList == null || activeRiskZonesList.isEmpty()) return;

        RiskZone nearest = RiskScoreCalculator.nearestZone(lat, lon, activeRiskZonesList);
        if (nearest == null) { proximityAlertManager.reset(); return; }

        double distanceMeters = HaversineUtil.distanceMeters(
                lat, lon, nearest.getLatitude(), nearest.getLongitude());

        // Adaptive SafeWalk check-in — only when timer is running
        if (distanceMeters <= ProximityAlertManager.TIER_2_METERS
                && cardActiveSafeWalk != null
                && cardActiveSafeWalk.getVisibility() == View.VISIBLE) {
            triggerAdaptiveCheckIn(nearest.getZoneName());
        }

        // Fire the tier alert (vibration + notification)
        proximityAlertManager.evaluate(distanceMeters, nearest.getZoneName());

        // Update the status banner
        updateProximityStatusBanner(distanceMeters, nearest);
    }

    private void updateProximityStatusBanner(double distanceMeters, RiskZone nearest) {
        if (tvZoneStatus == null) return;
        if (distanceMeters <= ProximityAlertManager.TIER_3_METERS) {
            tvZoneStatus.setText("DANGER: Inside risk zone — " + nearest.getZoneName());
            tvZoneStatus.setBackgroundColor(Color.parseColor("#DC3545"));
        } else if (distanceMeters <= ProximityAlertManager.TIER_2_METERS) {
            tvZoneStatus.setText(String.format(Locale.getDefault(),
                    "WARNING: %.0fm from risk zone", distanceMeters));
            tvZoneStatus.setBackgroundColor(Color.parseColor("#FD7E14"));
        } else if (distanceMeters <= ProximityAlertManager.TIER_1_METERS) {
            tvZoneStatus.setText(String.format(Locale.getDefault(),
                    "Caution: Risk zone %.0fm ahead", distanceMeters));
            tvZoneStatus.setBackgroundColor(Color.parseColor("#FFC107"));
        }
    }

    // ── Risk zone fetching + clustering (unchanged) ───────────────────────────
    private void fetchRiskZones() {
        String token = "Bearer " + TokenManager.getToken(requireContext());
        SupabaseService apiService = ApiClient.getClient().create(SupabaseService.class);
        apiService.getActiveAlerts(token).enqueue(new Callback<List<com.besafe.besafe.models.Alert>>() {
            @Override
            public void onResponse(Call<List<com.besafe.besafe.models.Alert>> call,
                                   Response<List<com.besafe.besafe.models.Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    calculateAndDrawDynamicZones(response.body());
                } else if (activeRiskZonesList != null) {
                    activeRiskZonesList.clear();
                }
            }
            @Override
            public void onFailure(Call<List<com.besafe.besafe.models.Alert>> call, Throwable t) {}
        });
    }

    private void calculateAndDrawDynamicZones(List<com.besafe.besafe.models.Alert> alerts) {
        int THRESHOLD = 2;
        float RADIUS  = 150f;
        if (activeRiskZonesList != null) activeRiskZonesList.clear();
        List<com.besafe.besafe.models.Alert> processed = new ArrayList<>();

        for (com.besafe.besafe.models.Alert alert : alerts) {
            if (processed.contains(alert)) continue;
            List<com.besafe.besafe.models.Alert> cluster = new ArrayList<>();
            cluster.add(alert);
            for (com.besafe.besafe.models.Alert other : alerts) {
                if (alert == other || processed.contains(other)) continue;
                float[] d = new float[1];
                Location.distanceBetween(alert.getLatitude(), alert.getLongitude(),
                        other.getLatitude(), other.getLongitude(), d);
                if (d[0] <= RADIUS) cluster.add(other);
            }
            if (cluster.size() >= THRESHOLD) {
                drawDynamicRiskZone(alert.getLatitude(), alert.getLongitude(),
                        RADIUS, cluster.size());
                processed.addAll(cluster);
            }
        }
    }

    private void drawDynamicRiskZone(double lat, double lon, float radius, int count) {
        int fillColor   = count >= 4 ? Color.parseColor("#80DC3545") : Color.parseColor("#59FD7E14");
        int strokeColor = count >= 4 ? Color.parseColor("#DC3545")   : Color.parseColor("#FD7E14");
        String threat   = count >= 4 ? "high" : "medium";

        Circle circle = mMap.addCircle(new CircleOptions()
                .center(new LatLng(lat, lon)).radius(radius)
                .strokeColor(strokeColor).strokeWidth(4f)
                .fillColor(fillColor).clickable(true));

        RiskZone zone = new RiskZone("Dynamic Hotspot", "Multiple SOS Alerts",
                count, lat, lon, radius, threat);
        RiskScoreCalculator.applyScore(zone, count, 60f, 2f);
        circle.setTag(zone);

        if (activeRiskZonesList == null) activeRiskZonesList = new ArrayList<>();
        activeRiskZonesList.add(zone);
    }

    private void showZoneDetails(RiskZone zone) {
        if (tvZoneName != null)
            tvZoneName.setText(zone.getZoneName() != null ? zone.getZoneName() : "Risk Area");
        if (tvIncidentCount != null)
            tvIncidentCount.setText("Based on " + zone.getIncidentCount() + " incidents.");
        if (tvDominantType != null)
            tvDominantType.setText(zone.getDominantIncidentType() != null ?
                    "Mostly: " + zone.getDominantIncidentType() : "Multiple types detected");

        if (cardThreatLevel != null && tvThreatLevel != null) {
            if ("high".equalsIgnoreCase(zone.getThreatLevel())) {
                cardThreatLevel.setCardBackgroundColor(Color.parseColor("#DC3545"));
                tvThreatLevel.setText("HIGH THREAT");
            } else if ("medium".equalsIgnoreCase(zone.getThreatLevel())) {
                cardThreatLevel.setCardBackgroundColor(Color.parseColor("#FD7E14"));
                tvThreatLevel.setText("MEDIUM THREAT");
            } else {
                cardThreatLevel.setCardBackgroundColor(Color.parseColor("#FFC107"));
                tvThreatLevel.setText("ELEVATED THREAT");
            }
        }
        if (bottomSheetBehavior != null)
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    // ── Zone status + SOS (unchanged) ────────────────────────────────────────
    private void updateZoneStatusUI(boolean insideZone, LatLng userLocation) {
        if (insideZone) {
            tvZoneStatus.setText("You are inside the campus safe zone");
            tvZoneStatus.setBackgroundColor(Color.parseColor("#198754"));
            btnSOS.setVisibility(View.VISIBLE);
            btnShareLocation.setVisibility(View.GONE);
        } else {
            tvZoneStatus.setText("You are outside the campus zone — stay alert");
            tvZoneStatus.setBackgroundColor(Color.parseColor("#DC3545"));
            btnSOS.setVisibility(View.VISIBLE);
            btnShareLocation.setVisibility(View.VISIBLE);
        }
    }

    private void sendSOSAlert() {
        btnSOS.setEnabled(false);
        btnSOS.setText("Sending...");
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            double lat = location != null ? location.getLatitude()
                    : (currentStudentLocation != null ? currentStudentLocation.latitude : 0.0);
            double lon = location != null ? location.getLongitude()
                    : (currentStudentLocation != null ? currentStudentLocation.longitude : 0.0);

            if (lat != 0.0 && lon != 0.0) {
                alertViewModel.sendEmergencyAlert(lat, lon, requireContext())
                        .observe(getViewLifecycleOwner(), dbResult -> {
                            if ("SUCCESS".equals(dbResult)) {
                                tvZoneStatus.setBackgroundColor(Color.parseColor("#DC3545"));
                                tvZoneStatus.setText("ALERT SENT — WAITING FOR SECURITY...");
                                btnSOS.setText("ACTIVE EMERGENCY");
                                hasAnimatedToGuard = false;
                                startReassuranceLoop();
                            } else {
                                Toast.makeText(getContext(), dbResult, Toast.LENGTH_LONG).show();
                                btnSOS.setEnabled(true);
                                btnSOS.setText("SOS");
                            }
                        });
            } else {
                Toast.makeText(getContext(), "Unable to lock GPS location.", Toast.LENGTH_SHORT).show();
                btnSOS.setEnabled(true);
                btnSOS.setText("SOS");
            }
        });
    }

    private void startReassuranceLoop() {
        String token = "Bearer " + TokenManager.getToken(requireContext());
        SupabaseService apiService = ApiClient.getClient().create(SupabaseService.class);
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                apiService.getMyLatestAlert(token).enqueue(new Callback<List<com.besafe.besafe.models.Alert>>() {
                    @Override
                    public void onResponse(Call<List<com.besafe.besafe.models.Alert>> call,
                                           Response<List<com.besafe.besafe.models.Alert>> response) {
                        if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                            String status   = response.body().get(0).getStatus();
                            Double guardLat = response.body().get(0).getGuardLatitude();
                            Double guardLon = response.body().get(0).getGuardLongitude();

                            if ("DISPATCHED".equals(status)) {
                                tvZoneStatus.setBackgroundColor(Color.parseColor("#0D6EFD"));
                                if (guardLat != null && guardLon != null && currentStudentLocation != null) {
                                    LatLng guardLatLng = new LatLng(guardLat, guardLon);
                                    float[] r = new float[1];
                                    Location.distanceBetween(currentStudentLocation.latitude,
                                            currentStudentLocation.longitude, guardLat, guardLon, r);
                                    int dm = Math.round(r[0]);
                                    int em = (dm / 3) / 60;
                                    tvZoneStatus.setText("SECURITY IS " + dm + "m AWAY\nETA: "
                                            + (em > 0 ? em + " min" : "< 1 min"));
                                    if (guardMarker == null) {
                                        guardMarker = mMap.addMarker(new MarkerOptions()
                                                .position(guardLatLng).title("Security Guard")
                                                .icon(BitmapDescriptorFactory.defaultMarker(
                                                        BitmapDescriptorFactory.HUE_BLUE)));
                                    } else { guardMarker.setPosition(guardLatLng); }
                                    if (!hasAnimatedToGuard) {
                                        LatLngBounds.Builder b = new LatLngBounds.Builder();
                                        b.include(currentStudentLocation); b.include(guardLatLng);
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 150));
                                        hasAnimatedToGuard = true;
                                    }
                                } else {
                                    tvZoneStatus.setText("SECURITY DISPATCHED! WAITING FOR GUARD GPS...");
                                }
                            } else if ("RESOLVED".equals(status)) {
                                tvZoneStatus.setBackgroundColor(Color.parseColor("#198754"));
                                tvZoneStatus.setText("EMERGENCY RESOLVED. YOU ARE SAFE.");
                                btnSOS.setEnabled(true); btnSOS.setText("SOS");
                                if (guardMarker != null) { guardMarker.remove(); guardMarker = null; }
                                hasAnimatedToGuard = false;
                                heartbeatHandler.removeCallbacks(heartbeatRunnable);
                                return;
                            }
                        }
                        heartbeatHandler.postDelayed(heartbeatRunnable, 3000);
                    }
                    @Override
                    public void onFailure(Call<List<com.besafe.besafe.models.Alert>> call, Throwable t) {
                        heartbeatHandler.postDelayed(heartbeatRunnable, 3000);
                    }
                });
            }
        };
        heartbeatHandler.post(heartbeatRunnable);
    }

    // ── Route drawing ─────────────────────────────────────────────────────────
    private void fetchAndDrawRoute(LatLng origin, LatLng destination) {
        clearAllRoutes();
        selectedPolyline = mMap.addPolyline(new PolylineOptions()
                .add(origin).add(destination).width(12f)
                .color(Color.parseColor("#0D6EFD")).geodesic(true).clickable(false));
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        b.include(origin); b.include(destination);
        try { mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 150)); }
        catch (Exception e) { e.printStackTrace(); }
    }

    private void clearAllRoutes() {
        if (selectedPolyline != null) { selectedPolyline.remove(); selectedPolyline = null; }
        for (Polyline p : alternativePolylines) p.remove();
        alternativePolylines.clear();
    }

    // ── Campus helpers ────────────────────────────────────────────────────────
    private void addCampusMarkers() {
        MarkerOptions gp = new MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mMap.addMarker(gp.position(STEVE_BIKO).title("DUT Steve Biko Campus"));
        mMap.addMarker(gp.position(RITSON).title("DUT Ritson Campus"));
        mMap.addMarker(gp.position(ML_SULTAN).title("DUT ML Sultan Campus"));
    }

    private void drawCampusZone() {
        PolygonOptions o = new PolygonOptions();
        for (LatLng v : CAMPUS_ZONE_VERTICES) o.add(v);
        o.strokeColor(Color.parseColor("#198754")).strokeWidth(6f)
                .fillColor(Color.parseColor("#33198754"));
        mMap.addPolygon(o);
    }

    private void fitCameraToZone() {
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        b.include(STEVE_BIKO); b.include(RITSON); b.include(ML_SULTAN);
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 150));
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000).setMinUpdateDistanceMeters(10f).build();
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (result.getLastLocation() != null) onUserLocationChanged(result.getLastLocation());
            }
        };
        fusedLocationClient.requestLocationUpdates(request, locationCallback, null);
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private boolean isPointInsidePolygon(LatLng point, LatLng[] polygon) {
        int intersections = 0, n = polygon.length;
        for (int i = 0; i < n; i++) {
            LatLng a = polygon[i], b = polygon[(i + 1) % n];
            if (((a.latitude <= point.latitude && point.latitude < b.latitude) ||
                    (b.latitude <= point.latitude && point.latitude < a.latitude)) &&
                    (point.longitude < (b.longitude - a.longitude) *
                            (point.latitude - a.latitude) / (b.latitude - a.latitude) + a.longitude))
                intersections++;
        }
        return (intersections % 2) != 0;
    }

    private void shareLocationWithSecurity() {
        if (currentStudentLocation == null) {
            Toast.makeText(getContext(), "Location not available yet. Please wait for GPS signal.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Generate the clickable Google Maps URL using their exact coordinates
        String mapsLink = "https://www.google.com/maps/search/?api=1&query="
                + currentStudentLocation.latitude + ","
                + currentStudentLocation.longitude;

        // 2. Draft the text message that will accompany the link
        String shareMessage = "📍 I am sharing my current location via BeSafe. See exactly where I am right now:\n\n" + mapsLink;

        // 3. Trigger the native Android Share Menu
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "My BeSafe Location");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareMessage);

        // 4. Show the popup so they can choose WhatsApp, SMS, Email, etc.
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Location via..."));
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @RequiresPermission(Manifest.permission.ACTIVITY_RECOGNITION)
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopLocationUpdates();
        cancelWellnessCheck();
        stopGoogleActivityRecognition();
        if (proximityAlertManager != null) proximityAlertManager.stopHeartbeat();
        if (checkInCountdownTimer != null) checkInCountdownTimer.cancel();
        if (activityUpdateReceiver != null)
            LocalBroadcastManager.getInstance(requireContext())
                    .unregisterReceiver(activityUpdateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (heartbeatHandler != null && heartbeatRunnable != null)
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        if (uiCountdownTimer != null) uiCountdownTimer.cancel();
    }
}