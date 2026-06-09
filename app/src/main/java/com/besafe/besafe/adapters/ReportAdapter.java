package com.besafe.besafe.adapters;

import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.besafe.besafe.AlertDetailActivity;
import com.besafe.besafe.R;
import com.besafe.besafe.activities.ReportDetailActivity;
import com.besafe.besafe.models.Report;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {

    private List<Report> reportList;

    public ReportAdapter(List<Report> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_report_card, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);

        holder.tvCategory.setText(report.getCategory());
        holder.tvDescription.setText(report.getDescription());

        String status = report.getStatus() != null
                ? report.getStatus().toUpperCase() : "PENDING";
        holder.tvStatus.setText(status);

        // Colour-code the status badge
        if ("RESOLVED".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#D4EDDA"));
            holder.tvStatus.setTextColor(Color.parseColor("#155724"));
        } else if ("UNDER REVIEW".equals(status) || "DISPATCHED".equals(status)) {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#CCE5FF"));
            holder.tvStatus.setTextColor(Color.parseColor("#004085"));
        } else {
            holder.cardStatus.setCardBackgroundColor(Color.parseColor("#FFF3CD"));
            holder.tvStatus.setTextColor(Color.parseColor("#856404"));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent;
            boolean isSos = report.getCategory() != null
                    && report.getCategory().contains("SOS");

            if (isSos) {
                intent = new Intent(v.getContext(), AlertDetailActivity.class);
                intent.putExtra("ALERT_ID",     report.getId());
                intent.putExtra("ALERT_STATUS", status);

                // ✅ FIXED: Pass lat/lon so AlertDetailActivity doesn't crash.
                // Report model stores coordinates from the SOS alert conversion.
                // If both are 0 the map will just show a default pin — no crash.
                intent.putExtra("ALERT_LAT", report.getLatitude());
                intent.putExtra("ALERT_LON", report.getLongitude());

            } else {
                intent = new Intent(v.getContext(), ReportDetailActivity.class);
                intent.putExtra("REPORT_ID",   report.getId());
                intent.putExtra("IMAGE_URL",   report.getImageUrl());
                intent.putExtra("DESCRIPTION", report.getDescription());
                intent.putExtra("STATUS",      status);
            }

            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return reportList != null ? reportList.size() : 0;
    }

    public void updateData(List<Report> newReportList) {
        this.reportList = newReportList;
        notifyDataSetChanged();
    }

    public static class ReportViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvDescription, tvStatus;
        ImageView ivThumbnail;
        MaterialCardView cardStatus;

        public ReportViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory    = itemView.findViewById(R.id.tvReportCategory);
            tvDescription = itemView.findViewById(R.id.tvReportDescription);
            tvStatus      = itemView.findViewById(R.id.tvReportStatus);
            ivThumbnail   = itemView.findViewById(R.id.ivReportThumbnail);
            cardStatus    = itemView.findViewById(R.id.cardStatus);
        }
    }
}