package com.besafe.besafe.fragments;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.besafe.besafe.R;
import com.besafe.besafe.utils.NotificationHelper;
import com.besafe.besafe.utils.TokenManager;
import com.besafe.besafe.viewmodels.AlertViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class HomeFragment extends Fragment {

    private MaterialButton   btnSos;
    private View             btnReportIssue;
    private View             btnViewReports;
    private MaterialCardView btnThemeToggle;
    private TextView         tvThemeLabel;

    private FusedLocationProviderClient fusedLocationClient;
    private AlertViewModel alertViewModel;

    private static final String PHONE_CAMPUS_CONTROL = "0313732000";
    private static final String WHATSAPP_SECURITY    = "+27811234567";
    private static final String PHONE_SAPS           = "10111";
    private static final String PHONE_CLINIC         = "0313732223";

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean granted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (granted != null && granted) triggerPanicAlert();
                else Toast.makeText(getContext(),
                        "GPS permission is required for SOS to work!", Toast.LENGTH_LONG).show();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        btnSos         = view.findViewById(R.id.btnSos);
        btnReportIssue = view.findViewById(R.id.btnReportIssue);
        btnViewReports = view.findViewById(R.id.btnViewReports);
        btnThemeToggle = view.findViewById(R.id.btnThemeToggle);
        tvThemeLabel   = view.findViewById(R.id.tvThemeLabel);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        alertViewModel      = new ViewModelProvider(this).get(AlertViewModel.class);

        updateThemeLabel();

        if (btnThemeToggle != null) {
            btnThemeToggle.setOnClickListener(v -> {
                int current = AppCompatDelegate.getDefaultNightMode();
                int next = (current == AppCompatDelegate.MODE_NIGHT_YES)
                        ? AppCompatDelegate.MODE_NIGHT_NO
                        : AppCompatDelegate.MODE_NIGHT_YES;

                // Persist so it survives restarts (BeSafeApplication reads this on launch)
                TokenManager.saveNightMode(requireContext(), next);
                AppCompatDelegate.setDefaultNightMode(next);
            });
        }

        btnSos.setOnClickListener(v ->
                Toast.makeText(getContext(), "Press and HOLD to trigger SOS", Toast.LENGTH_SHORT).show());
        btnSos.setOnLongClickListener(v -> { checkPermissionsAndTriggerSOS(); return true; });

        btnReportIssue.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ReportFragment())
                        .addToBackStack(null).commit());

        btnViewReports.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ReportsCalendarFragment())
                        .addToBackStack(null).commit());

        View btnCallControl = view.findViewById(R.id.btn_call_control);
        View btnWhatsapp    = view.findViewById(R.id.btn_whatsapp_security);
        View btnCallSaps    = view.findViewById(R.id.btn_call_saps);
        View btnCallClinic  = view.findViewById(R.id.btn_call_clinic);

        if (btnCallControl != null) btnCallControl.setOnClickListener(v -> dialNumber(PHONE_CAMPUS_CONTROL));
        if (btnWhatsapp    != null) btnWhatsapp.setOnClickListener(v    -> openWhatsApp(WHATSAPP_SECURITY));
        if (btnCallSaps    != null) btnCallSaps.setOnClickListener(v    -> dialNumber(PHONE_SAPS));
        if (btnCallClinic  != null) btnCallClinic.setOnClickListener(v  -> dialNumber(PHONE_CLINIC));

        return view;
    }

    private void updateThemeLabel() {
        if (tvThemeLabel == null) return;
        int nightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        tvThemeLabel.setText(nightMode == Configuration.UI_MODE_NIGHT_YES ? "Light" : "Dark");
    }

    private void checkPermissionsAndTriggerSOS() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            triggerPanicAlert();
        } else {
            requestPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void triggerPanicAlert() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        btnSos.setEnabled(false);
        btnSos.setText("SENT");
        Toast.makeText(getContext(), "Locating you...", Toast.LENGTH_SHORT).show();

        fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
            if (location == null) {
                Toast.makeText(getContext(),
                        "Location unavailable. Open Google Maps briefly then try again.",
                        Toast.LENGTH_LONG).show();
                resetSosButton();
                return;
            }
            if (!isUserOnCampus(location)) {
                Toast.makeText(getContext(),
                        "You appear to be off-campus. Redirecting to National Police (10111).",
                        Toast.LENGTH_LONG).show();
                resetSosButton();
                return;
            }

            double latitude  = location.getLatitude();
            double longitude = location.getLongitude();

            alertViewModel.sendEmergencyAlert(latitude, longitude, requireContext())
                    .observe(getViewLifecycleOwner(), dbResult -> {
                        if (dbResult != null && dbResult.startsWith("SUCCESS")) {
                            Toast.makeText(getContext(),
                                    "Emergency alert sent to campus security!", Toast.LENGTH_LONG).show();

                            String alertId = (dbResult.contains(":") && dbResult.length() > 8)
                                    ? dbResult.substring(dbResult.indexOf(":") + 1) : "";

                            String locationDetails = "Lat: " + latitude + ", Lon: " + longitude;
                            String studentEmail = TokenManager.getEmail(requireContext());
                            String displayName = (studentEmail != null && studentEmail.contains("@"))
                                    ? "Student #" + studentEmail.substring(0, studentEmail.indexOf("@"))
                                    : "A Student";

                            NotificationHelper.sendSosToSecurity(
                                    displayName, locationDetails, alertId, latitude, longitude);

                            alertViewModel.saveBroadcastToDatabase(
                                    "ACTIVE SOS",
                                    displayName + " requested help at " + locationDetails,
                                    "Security Only");
                        } else {
                            Toast.makeText(getContext(), dbResult, Toast.LENGTH_LONG).show();
                            resetSosButton();
                        }
                    });
        });
    }

    private void resetSosButton() {
        if (btnSos == null) return;
        btnSos.setEnabled(true);
        btnSos.setText("SOS");
    }

    private boolean isUserOnCampus(Location userLocation) {
        Location durban = new Location(""); durban.setLatitude(-29.8540); durban.setLongitude(31.0044);
        Location pmb    = new Location(""); pmb.setLatitude(-30.6406);    pmb.setLongitude(30.3831);
        return userLocation.distanceTo(durban) <= 1500 || userLocation.distanceTo(pmb) <= 1500;
    }

    private void dialNumber(String phoneNumber) {
        try { startActivity(new Intent(Intent.ACTION_DIAL).setData(Uri.parse("tel:" + phoneNumber))); }
        catch (Exception e) { Toast.makeText(getContext(), "Unable to open phone dialer.", Toast.LENGTH_SHORT).show(); }
    }

    private void openWhatsApp(String phoneNumber) {
        try { startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse("https://api.whatsapp.com/send?phone=" + phoneNumber))); }
        catch (Exception e) { Toast.makeText(getContext(), "WhatsApp is not installed.", Toast.LENGTH_SHORT).show(); }
    }
}