package com.besafe.besafe.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.besafe.besafe.R;
import com.google.android.material.card.MaterialCardView;

public class SecurityDirectoryFragment extends Fragment {

    // Dummy numbers for testing - you can update these to the real DUT numbers
    private static final String PHONE_CAMPUS_CONTROL = "0313732000";
    private static final String WHATSAPP_SECURITY = "+27811234567"; // Include country code!
    private static final String PHONE_SAPS = "10111";
    private static final String PHONE_CLINIC = "0313732223";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_security_directory, container, false);

        MaterialCardView cardCallControl = view.findViewById(R.id.card_call_control);
        MaterialCardView cardWhatsapp = view.findViewById(R.id.card_whatsapp_security);
        MaterialCardView cardCallSaps = view.findViewById(R.id.card_call_saps);
        MaterialCardView cardCallClinic = view.findViewById(R.id.card_call_clinic);

        // 1. Dial Campus Control
        cardCallControl.setOnClickListener(v -> dialNumber(PHONE_CAMPUS_CONTROL));

        // 2. Open WhatsApp Chat
        cardWhatsapp.setOnClickListener(v -> openWhatsApp(WHATSAPP_SECURITY));

        // 3. Dial SAPS
        cardCallSaps.setOnClickListener(v -> dialNumber(PHONE_SAPS));

        // 4. Dial Clinic
        cardCallClinic.setOnClickListener(v -> dialNumber(PHONE_CLINIC));

        return view;
    }

    private void dialNumber(String phoneNumber) {
        try {
            // ACTION_DIAL safely opens the phone dialer with the number pre-filled.
            // It does NOT require CALL_PHONE permissions in your Manifest!
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phoneNumber));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Unable to open phone dialer.", Toast.LENGTH_SHORT).show();
        }
    }

    private void openWhatsApp(String phoneNumber) {
        try {
            // This URL format opens WhatsApp directly to a chat with this number
            String url = "https://api.whatsapp.com/send?phone=" + phoneNumber;
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "WhatsApp is not installed on this device.", Toast.LENGTH_SHORT).show();
        }
    }
}