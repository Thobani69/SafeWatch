package com.besafe.besafe.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besafe.besafe.R;
import com.besafe.besafe.adapters.ReportAdapter;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.Alert;
import com.besafe.besafe.models.Report;
import com.besafe.besafe.utils.TokenManager;
import com.google.android.material.button.MaterialButtonToggleGroup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportsCalendarFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView recyclerViewReports;
    private TextView tvSelectedDate;
    private TextView tvEmptyState;
    private MaterialButtonToggleGroup toggleGroupFilters;

    private ReportAdapter adapter;
    private SupabaseService apiService;
    private List<Report> masterDataList = new ArrayList<>();

    private String currentSelectedDate = "";
    private String currentFilterType   = "ALL";

    // ✅ SAST formatter for calendar date display
    private final SimpleDateFormat dateFormatter;

    public ReportsCalendarFragment() {
        dateFormatter = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        dateFormatter.setTimeZone(TimeZone.getTimeZone("Africa/Johannesburg"));
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports_calendar, container, false);

        calendarView        = view.findViewById(R.id.calendarView);
        recyclerViewReports = view.findViewById(R.id.recyclerViewReports);
        tvSelectedDate      = view.findViewById(R.id.tvSelectedDate);
        tvEmptyState        = view.findViewById(R.id.tvEmptyState);
        toggleGroupFilters  = view.findViewById(R.id.toggleGroupFilters);

        recyclerViewReports.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReportAdapter(new ArrayList<>());
        recyclerViewReports.setAdapter(adapter);

        apiService = ApiClient.getClient().create(SupabaseService.class);

        // Default to today in SAST
        currentSelectedDate = dateFormatter.format(Calendar.getInstance(
                TimeZone.getTimeZone("Africa/Johannesburg")).getTime());
        tvSelectedDate.setText("DATA FOR " + currentSelectedDate);

        setupListeners();
        return view;
    }

    private void setupListeners() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            currentSelectedDate = String.format(
                    Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            tvSelectedDate.setText("DATA FOR " + currentSelectedDate);
            applyFilters();
        });

        toggleGroupFilters.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if      (checkedId == R.id.btnFilterAll)     currentFilterType = "ALL";
                else if (checkedId == R.id.btnFilterReports) currentFilterType = "REPORTS";
                else if (checkedId == R.id.btnFilterSos)     currentFilterType = "SOS";
                applyFilters();
            }
        });
    }

    private void fetchAllData() {
        String token = "Bearer " + TokenManager.getToken(requireContext());
        masterDataList.clear();

        apiService.getReports(token).enqueue(new Callback<List<Report>>() {
            @Override
            public void onResponse(Call<List<Report>> call, Response<List<Report>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    masterDataList.addAll(response.body());
                }
                fetchSosAlerts(token);
            }
            @Override
            public void onFailure(Call<List<Report>> call, Throwable t) {
                fetchSosAlerts(token);
            }
        });
    }

    private void fetchSosAlerts(String token) {
        apiService.getAlerts(token).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for (Alert alert : response.body()) {
                        String description = "Emergency at Lat: "
                                + alert.getLatitude() + ", Lon: " + alert.getLongitude();

                        // ✅ FIXED: Use the 5-arg constructor so lat/lon are stored
                        // This prevents the crash when tapping an SOS card
                        Report converted = new Report(
                                alert.getId(),
                                "  EMERGENCY SOS",
                                description,
                                alert.getStatus(),
                                alert.getCreatedAt(),
                                alert.getLatitude(),
                                alert.getLongitude()
                        );
                        masterDataList.add(converted);
                    }
                }
                applyFilters();
            }
            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                applyFilters();
            }
        });
    }

    private void applyFilters() {
        List<Report> filtered = new ArrayList<>();

        for (Report item : masterDataList) {
            if (item.getCreatedAt() == null) continue;

            // ✅ Convert the UTC created_at to SAST date for comparison
            String itemDate = utcToSastDate(item.getCreatedAt());
            boolean matchesDate = itemDate.equals(currentSelectedDate);

            boolean isSos = "  EMERGENCY SOS".equals(item.getCategory());
            boolean matchesType = "ALL".equals(currentFilterType)
                    || ("REPORTS".equals(currentFilterType) && !isSos)
                    || ("SOS".equals(currentFilterType) && isSos);

            if (matchesDate && matchesType) filtered.add(item);
        }

        if (filtered.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            recyclerViewReports.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewReports.setVisibility(View.VISIBLE);
        }

        adapter.updateData(filtered);
    }

    /**
     * Converts a Supabase UTC ISO timestamp to a SAST date string "yyyy-MM-dd".
     * Used so calendar filtering matches correctly for SA users.
     */
    private String utcToSastDate(String utcTimestamp) {
        try {
            SimpleDateFormat inputFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            inputFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            String clean = utcTimestamp.length() > 19
                    ? utcTimestamp.substring(0, 19) : utcTimestamp;
            Date date = inputFmt.parse(clean);
            return dateFormatter.format(date); // dateFormatter is already in SAST
        } catch (Exception e) {
            return utcTimestamp.length() >= 10 ? utcTimestamp.substring(0, 10) : utcTimestamp;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchAllData();
    }
}