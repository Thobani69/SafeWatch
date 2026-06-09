package com.besafe.besafe.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besafe.besafe.R;
import com.besafe.besafe.activities.LoginActivity;
import com.besafe.besafe.adapters.AlertAdapter;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.Alert;
import com.besafe.besafe.utils.TokenManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SecurityHomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlertAdapter alertAdapter;
    private SupabaseService apiService;

    // Pagination Variables
    private List<Alert> allActiveAlerts = new ArrayList<>();
    private int currentPage = 1;
    private static final int ITEMS_PER_PAGE = 10;

    private Button btnPrevPage, btnNextPage;
    private TextView tvPageIndicator;

    // ✨ NEW: Logout Button
    private MaterialButton btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_home, container, false);

        // 1. Setup the RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewAlerts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alertAdapter = new AlertAdapter();
        recyclerView.setAdapter(alertAdapter);

        // 2. Setup Pagination UI
        btnPrevPage = view.findViewById(R.id.btn_prev_page);
        btnNextPage = view.findViewById(R.id.btn_next_page);
        tvPageIndicator = view.findViewById(R.id.tv_page_indicator);

        if(btnPrevPage != null && btnNextPage != null) {
            btnPrevPage.setOnClickListener(v -> {
                if (currentPage > 1) {
                    currentPage--;
                    renderCurrentPage();
                }
            });

            btnNextPage.setOnClickListener(v -> {
                int totalPages = calculateTotalPages();
                if (currentPage < totalPages) {
                    currentPage++;
                    renderCurrentPage();
                }
            });
        }

        // 3. ✨ NEW: Setup Logout Logic
        btnLogout = view.findViewById(R.id.btn_security_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                // Wipe the dead token!
                TokenManager.clearToken(requireContext());
                Toast.makeText(requireContext(), "Signed out securely", Toast.LENGTH_SHORT).show();

                // Send back to Login Screen
                Intent intent = new Intent(requireActivity(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                requireActivity().finish();
            });
        }

        // 4. Initialize the API
        apiService = ApiClient.getClient().create(SupabaseService.class);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchActiveAlerts();
    }

    private void fetchActiveAlerts() {
        String token = "Bearer " + TokenManager.getToken(requireContext());

        apiService.getActiveAlerts(token).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allActiveAlerts = response.body();
                    currentPage = 1;
                    renderCurrentPage();
                } else {
                    // 🚨 If the token is dead, it will hit this block!
                    Toast.makeText(getContext(), "Session expired. Please Sign Out and log back in.", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==========================================
    // PAGINATION LOGIC
    // ==========================================
    private int calculateTotalPages() {
        int total = (int) Math.ceil((double) allActiveAlerts.size() / ITEMS_PER_PAGE);
        return total == 0 ? 1 : total;
    }

    private void renderCurrentPage() {
        int totalPages = calculateTotalPages();

        if(tvPageIndicator != null) tvPageIndicator.setText("Page " + currentPage + " of " + totalPages);
        if(btnPrevPage != null) btnPrevPage.setEnabled(currentPage > 1);
        if(btnNextPage != null) btnNextPage.setEnabled(currentPage < totalPages);

        int startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, allActiveAlerts.size());

        if (startIndex <= allActiveAlerts.size()) {
            List<Alert> pageItems = allActiveAlerts.subList(startIndex, endIndex);
            alertAdapter.setAlerts(pageItems);
        } else {
            alertAdapter.setAlerts(new ArrayList<>());
        }
    }
}