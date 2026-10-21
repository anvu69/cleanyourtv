package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/**
 * Sends the remote's Home button to this launcher on remotes that emit KEYCODE_ALL_APPS for Home
 * (Xiaomi sticks and boxes). Android TV 9 routes that key only to the system-image launcher, so once
 * the stock launcher is disabled the button does nothing. Every other key passes through untouched.
 *
 * Enable over adb:
 *   settings put secure enabled_accessibility_services com.gathin.tvhome/.HomeKeyService
 *   settings put secure accessibility_enabled 1
 */
public class HomeKeyService extends AccessibilityService {

    /** Remote buttons that should open Settings (config.xml, settings_key_scancodes and settings_key_keycodes). */
    private int[] settingsScancodes = new int[0];
    private int[] settingsKeycodes = new int[0];
    private boolean settingsPanel;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        settingsScancodes = getResources().getIntArray(R.array.settings_key_scancodes);
        // Keycodes and the panel choice can also come from a theme; theme.sh restarts this service.
        Config config = Config.load(this);
        settingsKeycodes = config.settingsKeycodes;
        settingsPanel = config.settingsKeyPanel;
        // FallbackHome is already up when this service binds at boot.
        if (FallbackHome.homePackage(this) != null) openLauncher();
    }

    private boolean isSettingsKey(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_SETTINGS) return true;
        for (int code : settingsScancodes) if (event.getScanCode() == code) return true;
        for (int code : settingsKeycodes) if (event.getKeyCode() == code) return true;
        return false;
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // The stock launcher handled the remote's Settings button; with it disabled the button does nothing.
        if (isSettingsKey(event)) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                Intent open = settingsPanel ? new Intent(this, QuickSettingsActivity.class)
                        : new Intent(Settings.ACTION_SETTINGS);
                startActivity(FallbackHome.ownTask(this, open));
            }
            return true;
        }
        if (event.getKeyCode() != KeyEvent.KEYCODE_ALL_APPS) return false;
        if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) openLauncher();
        return true;
    }

    private void openLauncher() {
        startActivity(new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setPackage(getPackageName())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED));
    }

    /** Adds this service back to the enabled accessibility services; needs WRITE_SECURE_SETTINGS (pm grant). */
    static void ensureEnabled(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) return;
        ContentResolver resolver = context.getContentResolver();
        ComponentName me = new ComponentName(context, HomeKeyService.class);
        String enabled = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) enabled = "";
        for (String entry : enabled.split(":")) {
            if (me.equals(ComponentName.unflattenFromString(entry))) return;
        }
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                enabled.isEmpty() ? me.flattenToShortString() : enabled + ":" + me.flattenToShortString());
        Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1);
    }

    private boolean isActivity(CharSequence pkg, CharSequence cls) {
        if (pkg == null || cls == null) return false;
        try {
            getPackageManager().getActivityInfo(new ComponentName(pkg.toString(), cls.toString()), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CharSequence topWindow;
    private final Runnable replaceFallbackHome = () -> {
        if (FallbackHome.isFallbackHome(topWindow)) openLauncher();
    };

    /**
     * Xiaomi China firmware only lets system-signed apps be HOME, so with the stock launcher disabled Home
     * lands on the empty FallbackHome (see {@link FallbackHome}). Step in for it on every Home press.
     */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        // Only activities count: Xiaomi's settings overlay reports a state change of its own right after
        // Home, and taking it for the screen on top cancelled the redirect.
        if (!isActivity(event.getPackageName(), event.getClassName())) return;
        topWindow = event.getClassName();
        // TV Settings opens in FallbackHome's task, which surfaces FallbackHome for a moment first: only
        // step in when it is still on top a beat later.
        handler.removeCallbacks(replaceFallbackHome);
        if (FallbackHome.isFallbackHome(topWindow)) handler.postDelayed(replaceFallbackHome, 300);
    }

    @Override
    public void onInterrupt() {
    }
}
