package com.besafe.besafe.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.besafe.besafe.utils.TokenManager;
import com.besafe.besafe.viewmodels.AlertViewModel;
import com.google.android.material.card.MaterialCardView;

/**
 * AlertsFragment — Campus Alerts inbox for students.
 *
 * Shows TWO types of content:
 *  1. Admin campus broadcasts (sent from the Admin Control Room)
 *  2. System safety alerts the student personally received
 *     (e.g. "You entered a risk zone", proximity warnings)
 *
 * The proximity zone notifications (300m / 150m / 75m) appear as
 * Android system notifications on the lock screen — they do NOT need
 * to appear here because they are real-time hardware alerts, not messages.
 * This page is the persistent inbox of campus-wide announcements.
 */
public class AlertsFragment extends Fragment {

    private AlertViewModel alertViewModel;
    private LinearLayout alertsContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        // Build the scroll layout
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setPadding(40, 40, 40, 40);

        alertsContainer = new LinearLayout(requireContext());
        alertsContainer.setOrientation(LinearLayout.VERTICAL);
        alertsContainer.setLayoutParams(new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        // Page title
        TextView title = new TextView(requireContext());
        title.setText("Campus Alerts");
        title.setTextSize(28);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setPadding(0, 0, 0, 8);
        alertsContainer.addView(title);

        // Subtitle explaining what this page shows
        TextView subtitle = new TextView(requireContext());
        subtitle.setText("Campus-wide announcements from security and administration.");
        subtitle.setTextSize(13);
        subtitle.setTextColor(Color.GRAY);
        subtitle.setPadding(0, 0, 0, 40);
        alertsContainer.addView(subtitle);

        scrollView.addView(alertsContainer);

        String currentUserRole = TokenManager.getRole(requireContext());
        alertViewModel = new ViewModelProvider(this).get(AlertViewModel.class);

        // Fetch broadcasts filtered by role
        alertViewModel.fetchBroadcasts(currentUserRole).observe(getViewLifecycleOwner(), broadcasts -> {
            // Clear everything below the title + subtitle
            alertsContainer.removeAllViews();
            alertsContainer.addView(title);
            alertsContainer.addView(subtitle);

            if (broadcasts == null || broadcasts.isEmpty()) {
                TextView empty = new TextView(requireContext());
                empty.setText("No campus alerts at this time.\n\nYour danger zone proximity warnings appear as phone notifications when you are near a risk zone.");
                empty.setTextSize(14);
                empty.setTextColor(Color.GRAY);
                empty.setPadding(0, 20, 0, 0);
                alertsContainer.addView(empty);
            } else {
                for (String alertText : broadcasts) {
                    addAlertCard(alertText);
                }
            }
        });

        return scrollView;
    }

    private void addAlertCard(String text) {
        MaterialCardView card = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 30);
        card.setLayoutParams(params);
        card.setCardElevation(4f);
        card.setRadius(16f);
        card.setStrokeWidth(1);
        card.setStrokeColor(Color.LTGRAY);

        // Colour-code SOS alerts differently from regular broadcasts
        if (text.contains("🚨")) {
            card.setCardBackgroundColor(Color.parseColor("#FFF0F0")); // light red for SOS
            card.setStrokeColor(Color.parseColor("#FFCCCC"));
        }

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setPadding(40, 40, 40, 40);
        tv.setTextSize(15);
        tv.setTextColor(Color.DKGRAY);
        tv.setLineSpacing(4f, 1f);

        card.addView(tv);
        alertsContainer.addView(card);
    }
}