package com.besafe.besafe.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Central store for all user session data.
 * Handles: JWT token + refresh token, role, email, user_id,
 *          dark-mode preference, and SafeWalk PIN.
 */
public class TokenManager {

    private static final String PREF_NAME       = "BeSafePrefs";
    private static final String KEY_TOKEN        = "jwt_token";
    private static final String KEY_REFRESH      = "refresh_token";
    private static final String KEY_ROLE         = "user_role";
    private static final String KEY_EMAIL        = "user_email";
    private static final String KEY_USER_ID      = "USER_ID";
    private static final String KEY_NIGHT_MODE   = "night_mode";
    private static final String KEY_SAFEWALK_PIN = "safewalk_pin";

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ── JWT access token ──────────────────────────────────────────────────────
    public static void saveToken(Context ctx, String token) {
        prefs(ctx).edit().putString(KEY_TOKEN, token).apply();
    }

    public static String getToken(Context ctx) {
        return prefs(ctx).getString(KEY_TOKEN, null);
    }

    // ── Refresh token (keeps session alive beyond 1-hour JWT expiry) ──────────
    public static void saveRefreshToken(Context ctx, String refreshToken) {
        prefs(ctx).edit().putString(KEY_REFRESH, refreshToken).apply();
    }

    public static String getRefreshToken(Context ctx) {
        return prefs(ctx).getString(KEY_REFRESH, null);
    }

    // ── Role ──────────────────────────────────────────────────────────────────
    public static void saveRole(Context ctx, String role) {
        prefs(ctx).edit().putString(KEY_ROLE, role).apply();
    }

    public static String getRole(Context ctx) {
        return prefs(ctx).getString(KEY_ROLE, "student");
    }

    // ── Email ─────────────────────────────────────────────────────────────────
    public static void saveEmail(Context ctx, String email) {
        prefs(ctx).edit().putString(KEY_EMAIL, email).apply();
    }

    public static String getEmail(Context ctx) {
        return prefs(ctx).getString(KEY_EMAIL, null);
    }

    // ── User ID ───────────────────────────────────────────────────────────────
    public static void saveUserId(Context ctx, String userId) {
        prefs(ctx).edit().putString(KEY_USER_ID, userId).apply();
    }

    public static String getUserId(Context ctx) {
        return prefs(ctx).getString(KEY_USER_ID, "");
    }

    // ── Dark mode preference (persists across restarts) ───────────────────────
    public static void saveNightMode(Context ctx, int mode) {
        prefs(ctx).edit().putInt(KEY_NIGHT_MODE, mode).apply();
    }

    public static int getNightMode(Context ctx) {
        return prefs(ctx).getInt(KEY_NIGHT_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // ── SafeWalk PIN ──────────────────────────────────────────────────────────
    public static void saveSafeWalkPin(Context ctx, String pin) {
        prefs(ctx).edit().putString(KEY_SAFEWALK_PIN, pin).apply();
    }

    public static String getSafeWalkPin(Context ctx) {
        // Falls back to "1234" if the user has never set a PIN
        return prefs(ctx).getString(KEY_SAFEWALK_PIN, "1234");
    }

    // ── Clear everything on logout ────────────────────────────────────────────
    public static void clearToken(Context ctx) {
        prefs(ctx).edit()
                .remove(KEY_TOKEN)
                .remove(KEY_REFRESH)
                .remove(KEY_ROLE)
                .remove(KEY_USER_ID)
                .apply();
        // Intentionally keep email, night_mode, and safewalk_pin
        // so UX preferences survive a logout/re-login
    }
}