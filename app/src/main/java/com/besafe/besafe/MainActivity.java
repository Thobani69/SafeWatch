package com.besafe.besafe;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.besafe.besafe.fragments.AdminDashboardFragment;
import com.besafe.besafe.fragments.AlertsFragment;
import com.besafe.besafe.fragments.HomeFragment;
import com.besafe.besafe.fragments.MapFragment;
import com.besafe.besafe.fragments.ProfileFragment;
import com.besafe.besafe.fragments.ReportsCalendarFragment;
import com.besafe.besafe.fragments.SecurityArchiveFragment;
import com.besafe.besafe.fragments.SecurityHomeFragment;
import com.besafe.besafe.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.onesignal.OneSignal;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // ✅ Route by ROLE — not by email address
        String userRole = TokenManager.getRole(this);

        // OneSignal notification deep-link listener
        OneSignal.getNotifications().addClickListener(event -> {
            org.json.JSONObject data = event.getNotification().getAdditionalData();
            String role = TokenManager.getRole(MainActivity.this);

            if (data != null && data.has("alert_id")) {
                String pushAlertId = data.optString("alert_id");
                double pushLat     = data.optDouble("latitude", 0.0);
                double pushLon     = data.optDouble("longitude", 0.0);

                Intent intent = new Intent(MainActivity.this, AlertDetailActivity.class);
                intent.putExtra("ALERT_ID",     pushAlertId);
                intent.putExtra("ALERT_LAT",    pushLat);
                intent.putExtra("ALERT_LON",    pushLon);
                intent.putExtra("ALERT_STATUS", "ACTIVE");
                startActivity(intent);

            } else if ("security".equals(role)) {
                fetchLatestAlertAndOpen();
            }
        });

        // ── ADMIN ─────────────────────────────────────────────────────────────
        if ("admin".equals(userRole)) {
            OneSignal.getUser().addTag("role", "admin");
            bottomNavigationView.setVisibility(View.GONE);

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new AdminDashboardFragment())
                        .commit();
            }
            return; // Admin done — no nav bar needed
        }

        // ── SECURITY ──────────────────────────────────────────────────────────
        if ("security".equals(userRole)) {
            OneSignal.getUser().addTag("role", "security");
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.guard_bottom_nav);

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SecurityHomeFragment())
                        .commit();
                bottomNavigationView.setSelectedItemId(R.id.nav_live_feed);
            }

            // ── STUDENT (default) ─────────────────────────────────────────────────
        } else {
            OneSignal.getUser().addTag("role", "student");
            bottomNavigationView.getOrCreateBadge(R.id.nav_alerts).setVisible(true);

            if (savedInstanceState == null) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
                bottomNavigationView.setSelectedItemId(R.id.nav_home);
            }
        }

        // ── Bottom nav item selection ─────────────────────────────────────────
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if      (itemId == R.id.nav_home)             selectedFragment = new HomeFragment();
                else if (itemId == R.id.nav_map)              selectedFragment = new MapFragment();
                else if (itemId == R.id.nav_alerts) {
                    bottomNavigationView.removeBadge(R.id.nav_alerts);
                    selectedFragment = new AlertsFragment();
                }
                else if (itemId == R.id.nav_profile)          selectedFragment = new ProfileFragment();
                else if (itemId == R.id.nav_live_feed)        selectedFragment = new SecurityHomeFragment();
                else if (itemId == R.id.nav_reports_calendar) selectedFragment = new ReportsCalendarFragment();
                else if (itemId == R.id.nav_archive)          selectedFragment = new SecurityArchiveFragment();

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    return true;
                }
                return false;
            }
        });
    }

    private void fetchLatestAlertAndOpen() {
        String token = "Bearer " + TokenManager.getToken(this);

        com.besafe.besafe.data.remote.SupabaseService apiService =
                com.besafe.besafe.data.remote.ApiClient.getClient()
                        .create(com.besafe.besafe.data.remote.SupabaseService.class);

        apiService.getActiveAlerts(token).enqueue(
                new retrofit2.Callback<java.util.List<com.besafe.besafe.models.Alert>>() {
                    @Override
                    public void onResponse(
                            retrofit2.Call<java.util.List<com.besafe.besafe.models.Alert>> call,
                            retrofit2.Response<java.util.List<com.besafe.besafe.models.Alert>> response) {

                        if (response.isSuccessful()
                                && response.body() != null
                                && !response.body().isEmpty()) {

                            com.besafe.besafe.models.Alert latest = response.body().get(0);

                            Intent intent = new Intent(MainActivity.this, AlertDetailActivity.class);
                            intent.putExtra("ALERT_ID",       latest.getId());
                            intent.putExtra("ALERT_LAT",      latest.getLatitude());
                            intent.putExtra("ALERT_LON",      latest.getLongitude());
                            intent.putExtra("ALERT_STATUS",   latest.getStatus());
                            intent.putExtra("REPORTER_EMAIL", latest.getReporterEmail());
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onFailure(
                            retrofit2.Call<java.util.List<com.besafe.besafe.models.Alert>> call,
                            Throwable t) {
                        getSupportFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, new SecurityHomeFragment())
                                .commit();
                    }
                });
    }
}