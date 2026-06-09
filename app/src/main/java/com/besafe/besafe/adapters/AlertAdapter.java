package com.besafe.besafe.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besafe.besafe.AlertDetailActivity;
import com.besafe.besafe.R;
import com.besafe.besafe.models.Alert;

import java.util.ArrayList;
import java.util.List;

public class AlertAdapter extends RecyclerView.Adapter<AlertAdapter.AlertViewHolder> {

    private List<Alert> alertList = new ArrayList<>();

    public void setAlerts(List<Alert> alerts) {
        this.alertList = alerts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AlertViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new AlertViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlertViewHolder holder, int position) {
        Alert alert = alertList.get(position);

        // 1. Ticket number from first 6 chars of UUID
        String ticketNum = alert.getId() != null
                ? "#" + alert.getId().substring(0, 6).toUpperCase()
                : "#UNKNOWN";
        holder.tvAlertId.setText(ticketNum);

        // 2. ✅ FIXED: Show reporter email. If null or empty, fall back to "Anonymous"
        //    The "Unknown User" was showing because reporter_email was not being saved
        //    when the SOS was sent. Now we show the email and strip the domain part
        //    to show just the student number for a cleaner display.
        String reporterEmail = alert.getReporterEmail();
        String displayName;

        if (reporterEmail != null && !reporterEmail.isEmpty()) {
            // Extract student number from email (e.g. "21900000@dut4life.ac.za" → "Student #21900000")
            if (reporterEmail.contains("@")) {
                String studentNumber = reporterEmail.substring(0, reporterEmail.indexOf("@"));
                displayName = "Student #" + studentNumber;
            } else {
                displayName = reporterEmail;
            }
        } else {
            displayName = "Anonymous Student";
        }
        holder.tvReporterEmail.setText("From: " + displayName);

        // 3. Location
        holder.tvAlertLocation.setText(
                "Location: " + alert.getLatitude() + ", " + alert.getLongitude());

        // 4. Time — formatted cleanly
        String time = alert.getCreatedAt();
        if (time != null && time.length() > 16) {
            time = time.substring(0, 10) + " at " + time.substring(11, 16);
        }
        holder.tvAlertTime.setText("Time: " + time);

        // 5. Click → open AlertDetailActivity
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), AlertDetailActivity.class);
            intent.putExtra("ALERT_ID",     alert.getId());
            intent.putExtra("ALERT_LAT",    alert.getLatitude());
            intent.putExtra("ALERT_LON",    alert.getLongitude());
            intent.putExtra("ALERT_STATUS", alert.getStatus());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return alertList.size();
    }

    static class AlertViewHolder extends RecyclerView.ViewHolder {
        TextView tvAlertLocation, tvAlertTime, tvAlertId, tvReporterEmail;

        AlertViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAlertLocation  = itemView.findViewById(R.id.tvAlertLocation);
            tvAlertTime      = itemView.findViewById(R.id.tvAlertTime);
            tvAlertId        = itemView.findViewById(R.id.tvAlertId);
            tvReporterEmail  = itemView.findViewById(R.id.tvReporterEmail);
        }
    }
}