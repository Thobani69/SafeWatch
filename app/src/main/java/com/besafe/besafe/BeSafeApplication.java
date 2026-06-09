package com.besafe.besafe;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

import com.besafe.besafe.data.remote.ApiClient;
import com.besafe.besafe.utils.TokenManager;
import com.onesignal.OneSignal;
import com.onesignal.debug.LogLevel;
import com.onesignal.Continue;

public class BeSafeApplication extends Application {

    private static final String ONESIGNAL_APP_ID = "656207f7-97cf-4cee-99fa-b8b27e041fc8";

    @Override
    public void onCreate() {
        super.onCreate();

        // Give ApiClient the app context so its token-refresh interceptor can
        // read/save the JWT without needing an Activity reference
        ApiClient.init(this);

        // Restore the user's dark/light mode choice before any Activity launches
        // This prevents the white flash on dark-mode users every restart
        int savedMode = TokenManager.getNightMode(this);
        AppCompatDelegate.setDefaultNightMode(savedMode);

        // OneSignal
        OneSignal.getDebug().setLogLevel(LogLevel.VERBOSE);
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID);
        OneSignal.getNotifications().requestPermission(true, Continue.with(r -> {}));
    }
}