package com.gathin.tvhome;

import android.content.Context;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Approves installs that Xiaomi China firmware leaves hanging. Its package installer has no CONFIRM_INSTALL screen,
 * so an app store installing through a PackageInstaller session (Aptoide does for split APKs) waits forever for a
 * confirmation that never shows. Sessions from the stores in config.json auto_approve_installers are approved
 * through LocalAdb, which runs {@link ApproveInstalls} from this APK as the adb shell.
 */
final class InstallApprover extends PackageInstaller.SessionCallback {

    private static final String TAG = "InstallApprover";
    private static InstallApprover started;

    private final Context context;
    private final PackageInstaller installer;
    private final List<String> stores;
    private final Set<Integer> handled = new HashSet<>();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    private InstallApprover(Context context, List<String> stores) {
        this.context = context;
        this.installer = context.getPackageManager().getPackageInstaller();
        this.stores = stores;
    }

    /**
     * Watches install sessions for the rest of this process; does nothing unless config lists a store. Safe to call
     * often: at boot config.json may not be readable yet (external storage mounts late), so callers retry.
     */
    static synchronized void start(Context context) {
        if (started != null) return;
        List<String> stores = Config.autoApproveInstallers(context);
        if (stores.isEmpty()) return;
        LocalAdb.keepEnabled(context.getApplicationContext());
        started = new InstallApprover(context.getApplicationContext(), stores);
        started.installer.registerSessionCallback(started, new Handler(Looper.getMainLooper()));
        // Connect once now, so the TV asks to allow this key while someone is setting it up, not mid-install.
        started.worker.execute(() -> started.shell("true"));
        for (PackageInstaller.SessionInfo session : started.installer.getAllSessions()) {
            started.check(session.getSessionId());
        }
    }

    private void check(int sessionId) {
        PackageInstaller.SessionInfo session = installer.getSessionInfo(sessionId);
        if (session == null || !stores.contains(session.getInstallerPackageName())) return;
        // A session reaches 0.8 once all its APKs are written; it needs the confirmation only after commit.
        boolean committed = Build.VERSION.SDK_INT >= 29 ? session.isCommitted() : session.getProgress() >= 0.8f;
        if (!committed || !handled.add(sessionId)) return;
        Log.i(TAG, "approving session " + sessionId + " from " + session.getInstallerPackageName());
        worker.execute(() -> shell("CLASSPATH=" + context.getApplicationInfo().sourceDir
                + " app_process / " + ApproveInstalls.class.getName() + " " + sessionId));
    }

    private void shell(String command) {
        try {
            String out = LocalAdb.run(context, command);
            if (!out.isEmpty()) Log.i(TAG, out.trim());
        } catch (Exception e) {
            Log.w(TAG, "adb: " + e);
        }
    }

    @Override public void onCreated(int sessionId) {
        check(sessionId);
    }

    @Override public void onBadgingChanged(int sessionId) {
    }

    @Override public void onActiveChanged(int sessionId, boolean active) {
        check(sessionId);
    }

    @Override public void onProgressChanged(int sessionId, float progress) {
        if (progress >= 0.8f) check(sessionId);
    }

    @Override public void onFinished(int sessionId, boolean success) {
    }
}
