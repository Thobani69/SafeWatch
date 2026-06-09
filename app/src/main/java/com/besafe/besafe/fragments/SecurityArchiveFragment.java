package com.besafe.besafe.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.besafe.besafe.R;
import com.besafe.besafe.adapters.AlertAdapter;
import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.data.remote.SupabaseService;
import com.besafe.besafe.models.Alert;
import com.besafe.besafe.utils.TokenManager;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SecurityArchiveFragment extends Fragment {

    private RecyclerView recyclerView;
    private AlertAdapter alertAdapter;
    private SupabaseService apiService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_archive, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewArchive);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // We can reuse the exact same adapter we built for the live feed!
        alertAdapter = new AlertAdapter();
        recyclerView.setAdapter(alertAdapter);

        apiService = ApiClient.getClient().create(SupabaseService.class);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchResolvedAlerts(); // Refresh the archive every time they open this tab
    }

    private void fetchResolvedAlerts() {
        String token = "Bearer " + TokenManager.getToken(requireContext());

        // CRITICAL DIFFERENCE: We are calling getResolvedAlerts() here instead of getActiveAlerts()
        apiService.getResolvedAlerts(token).enqueue(new Callback<List<Alert>>() {
            @Override
            public void onResponse(Call<List<Alert>> call, Response<List<Alert>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    alertAdapter.setAlerts(response.body());
                } else {
                    Toast.makeText(getContext(), "Failed to load archive", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Alert>> call, Throwable t) {
                Toast.makeText(getContext(), "Network Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}