package com.besafe.besafe.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class ProximityAlertManager {

    // Distance thresholds
    public static final double TIER_1_METERS = 300.0;
    public static final double TIER_2_METERS = 150.0;
    public static final double TIER_3_METERS =  75.0;

    // Notification channel IDs
    private static final String CHANNEL_CAUTION = "bs_caution";
    private static final String CHANNEL_WARNING = "bs_warning";
    private static final String CHANNEL_DANGER  = "bs_danger";

    // Notification IDs — each tier overwrites its own previous notification
    private static final int NOTIF_ID_CAUTION = 2001;
    private static final int NOTIF_ID_WARNING = 2002;
    private static final int NOTIF_ID_DANGER  = 2003;

    private final Context context;
    private final Vibrator vibrator;
    private final NotificationManager notificationManager;

    private int currentTier = 0;

    private final Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private boolean isHeartbeatRunning = false;
    private long heartbeatInterval = 2000;

    public ProximityAlertManager(Context context) {
        this.context = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            this.vibrator = (vm != null) ? vm.getDefaultVibrator() : null;
        } else {
            this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }

        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        // MUST be called before any notification is shown
        createChannels();
    }

    // ── Create channels once on first run ─────────────────────────────────────
    // Without this, notifications are silently dropped on Android 8+
    private void createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        if (notificationManager == null) return;

        NotificationChannel caution = new NotificationChannel(
                CHANNEL_CAUTION, "Safety Caution", NotificationManager.IMPORTANCE_DEFAULT);
        caution.setDescription("Risk zone 300m ahead warning");
        caution.enableVibration(false);

        NotificationChannel warning = new NotificationChannel(
                CHANNEL_WARNING, "Safety Warning", NotificationManager.IMPORTANCE_HIGH);
        warning.setDescription("Entering 150m risk buffer");
        warning.enableVibration(false);

        NotificationChannel danger = new NotificationChannel(
                CHANNEL_DANGER, "Danger Zone Alert", NotificationManager.IMPORTANCE_HIGH);
        danger.setDescription("Inside active danger zone");
        danger.enableVibration(true);
        danger.setVibrationPattern(new long[]{0, 300, 200, 300});

        notificationManager.createNotificationChannel(caution);
        notificationManager.createNotificationChannel(warning);
        notificationManager.createNotificationChannel(danger);
    }

    // ── Main radar method — call every location update ────────────────────────
    public void evaluate(double distanceMeters, String zoneName) {
        int newTier = 0;

        if      (distanceMeters <= TIER_3_METERS) newTier = 3;
        else if (distanceMeters <= TIER_2_METERS) newTier = 2;
        else if (distanceMeters <= TIER_1_METERS) newTier = 1;

        // Only escalate — never re-fire the same tier
        if (newTier > currentTier) {
            triggerAlertForTier(newTier, zoneName);
        } else if (newTier == 0 && currentTier > 0) {
            // Student walked clear of all zones
            stopHeartbeat();
            if (notificationManager != null) {
                notificationManager.cancel(NOTIF_ID_WARNING);
                notificationManager.cancel(NOTIF_ID_DANGER);
            }
            Toast.makeText(context, "You have safely exited the risk area.", Toast.LENGTH_SHORT).show();
        }

        currentTier = newTier;

        if (currentTier >= 2) adjustHeartbeatSpeed(distanceMeters);
    }

    private void triggerAlertForTier(int tier, String zoneName) {
        switch (tier) {
            case 1:
                singleVibrate(400);
                sendNotification(NOTIF_ID_CAUTION, CHANNEL_CAUTION,
                        NotificationCompat.PRIORITY_DEFAULT,
                        "⚠️ Risk zone ahead",
                        "You are approaching " + zoneName + " (300m ahead). Consider an alternative path.");
                break;

            case 2:
                sendNotification(NOTIF_ID_WARNING, CHANNEL_WARNING,
                        NotificationCompat.PRIORITY_HIGH,
                        "🚨 Warning: Entering risk area",
                        "You are within 150m of " + zoneName + ". Stay alert. SafeWalk check-in required.");
                startHeartbeat();
                break;

            case 3:
                singleVibrate(1500);
                sendNotification(NOTIF_ID_DANGER, CHANNEL_DANGER,
                        NotificationCompat.PRIORITY_MAX,
                        "🛑 DANGER: Inside active risk zone",
                        "You are inside " + zoneName + ". Press SOS if you feel unsafe.");
                break;
        }
    }

    // ── Notification sender ───────────────────────────────────────────────────
    private void sendNotification(int notifId, String channelId, int priority,
                                  String title, String body) {
        if (notificationManager == null) return;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_SOUND);

        notificationManager.notify(notifId, builder.build());
    }

    // ── Vibration ─────────────────────────────────────────────────────────────
    private void singleVibrate(long ms) {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(ms);
                }
            }
        } catch (Exception e) { /* never crash over vibration */ }
    }

    // ── Heartbeat (rhythmic pocket buzz while inside Tier 2) ─────────────────
    private void startHeartbeat() {
        if (isHeartbeatRunning) return;
        isHeartbeatRunning = true;
        heartbeatRunnable.run();
    }

    public void stopHeartbeat() {
        isHeartbeatRunning = false;
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
        if (vibrator != null) vibrator.cancel();
    }

    private void adjustHeartbeatSpeed(double distanceMeters) {
        if      (distanceMeters <= 50)  heartbeatInterval = 500;
        else if (distanceMeters <= 100) heartbeatInterval = 1000;
        else                            heartbeatInterval = 2000;
    }

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isHeartbeatRunning) return;
            try {
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        long[] timings    = {0, 150, 100, 150};
                        int[]  amplitudes = {0, 100,   0, 100};
                        vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1));
                    } else {
                        vibrator.vibrate(new long[]{0, 150, 100, 150}, -1);
                    }
                }
            } catch (Exception e) { /* ignore */ }
            heartbeatHandler.postDelayed(this, heartbeatInterval);
        }
    };

    public void reset() {
        currentTier = 0;
        stopHeartbeat();
    }
}