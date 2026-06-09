package com.besafe.besafe.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.besafe.besafe.R;
import com.besafe.besafe.activities.PinCheckActivity;
import com.besafe.besafe.MainActivity;

public class SafetyTimerService extends Service {

    private static final String CHANNEL_ID = "SafetyTimerChannel";
    private static final int NOTIFICATION_ID = 1;
    private CountDownTimer countDownTimer;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // If the user wants to cancel the timer from the notification
        if (intent != null && "ACTION_CANCEL".equals(intent.getAction())) {
            stopTimerAndService();
            return START_NOT_STICKY;
        }

        // Get the minutes passed from MapFragment
        int minutes = (intent != null) ? intent.getIntExtra("MINUTES", 5) : 5;
        long timeLeftInMillis = minutes * 60 * 1000L;

        // Build the notification
        Notification notification = buildNotification(minutes + ":00");

        // Start the Foreground Service with the correct type for Android 14+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        startTimer(timeLeftInMillis);

        return START_NOT_STICKY; // If killed by system, don't auto-restart
    }

    private void startTimer(long durationInMillis) {
        countDownTimer = new CountDownTimer(durationInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int minutes = (int) (millisUntilFinished / 1000) / 60;
                int seconds = (int) (millisUntilFinished / 1000) % 60;
                String timeLeftFormatted = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);

                // Update the notification every second
                updateNotification(timeLeftFormatted);
            }

            @Override
            public void onFinish() {
                // TIME IS UP! Wake up the screen and launch the PIN Check Activity
                Intent alarmIntent = new Intent(SafetyTimerService.this, PinCheckActivity.class);
                alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(alarmIntent);

                stopSelf(); // Stop the service
            }
        }.start();
    }

    private Notification buildNotification(String timeLeft) {
        // Intent to open the app if they tap the notification
        Intent openAppIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingOpenAppIntent = PendingIntent.getActivity(
                this, 0, openAppIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        // Intent to cancel the timer
        Intent cancelIntent = new Intent(this, SafetyTimerService.class);
        cancelIntent.setAction("ACTION_CANCEL");
        PendingIntent pendingCancelIntent = PendingIntent.getService(
                this, 1, cancelIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BeSafe Safety Timer")
                .setContentText("Time remaining: " + timeLeft)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingOpenAppIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel Timer", pendingCancelIntent)
                .setOngoing(true) // Cannot be swiped away
                .setOnlyAlertOnce(true) // Don't vibrate every second!
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(String timeLeft) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(NOTIFICATION_ID, buildNotification(timeLeft));
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Safety Timer Channel",
                    NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't beep every second
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private void stopTimerAndService() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        stopForeground(true);
        stopSelf();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}