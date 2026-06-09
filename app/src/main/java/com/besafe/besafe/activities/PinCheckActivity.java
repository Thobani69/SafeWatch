package com.besafe.besafe.activities;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.besafe.besafe.R;
import com.besafe.besafe.utils.TokenManager;
import com.besafe.besafe.viewmodels.AlertViewModel;

public class PinCheckActivity extends AppCompatActivity {

    private TextView tvWarning;
    private EditText etPin;
    private Button btnConfirmPin;
    private CountDownTimer emergencyCountdown;
    private AlertViewModel alertViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Force screen on over the lock screen
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(createUI());

        alertViewModel = new ViewModelProvider(this).get(AlertViewModel.class);

        // Fetch the student's PIN — falls back to "1234" if never set
        String correctPin = TokenManager.getSafeWalkPin(this);

        emergencyCountdown = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long ms) {
                tvWarning.setText("ARE YOU SAFE?\nEnter PIN in "
                        + (ms / 1000) + " seconds or Security will be dispatched!");
            }
            @Override
            public void onFinish() {
                fireAutoSOS();
            }
        }.start();

        btnConfirmPin.setOnClickListener(v -> {
            String entered = etPin.getText().toString().trim();
            if (correctPin.equals(entered)) {
                emergencyCountdown.cancel();
                Toast.makeText(this, "Timer cancelled. Stay safe!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Incorrect PIN!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fireAutoSOS() {
        tvWarning.setText("FIRING AUTO-SOS...");
        btnConfirmPin.setEnabled(false);
        alertViewModel.sendEmergencyAlert(-29.8541, 31.0043, this)
                .observe(this, result -> {
                    if (result != null && result.startsWith("SUCCESS")) {
                        Toast.makeText(this, "SECURITY DISPATCHED", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "SOS Failed. Call security directly.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                });
    }

    @Override
    public void onBackPressed() {
        Toast.makeText(this, "You must enter your PIN!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (emergencyCountdown != null) emergencyCountdown.cancel();
    }

    private android.widget.LinearLayout createUI() {
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(64, 128, 64, 64);
        layout.setBackgroundColor(android.graphics.Color.parseColor("#DC3545"));

        tvWarning = new TextView(this);
        tvWarning.setTextColor(android.graphics.Color.WHITE);
        tvWarning.setTextSize(24f);
        tvWarning.setTextAlignment(android.view.View.TEXT_ALIGNMENT_CENTER);
        tvWarning.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(tvWarning);

        etPin = new EditText(this);
        etPin.setHint("Enter your SafeWalk PIN");
        etPin.setTextColor(android.graphics.Color.BLACK);
        etPin.setBackgroundColor(android.graphics.Color.WHITE);
        etPin.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        etPin.setPadding(32, 32, 32, 32);
        android.widget.LinearLayout.LayoutParams p =
                new android.widget.LinearLayout.LayoutParams(-1, -2);
        p.setMargins(0, 64, 0, 64);
        layout.addView(etPin, p);

        btnConfirmPin = new Button(this);
        btnConfirmPin.setText("I AM SAFE — CANCEL TIMER");
        btnConfirmPin.setBackgroundColor(android.graphics.Color.BLACK);
        btnConfirmPin.setTextColor(android.graphics.Color.WHITE);
        layout.addView(btnConfirmPin);

        return layout;
    }
}