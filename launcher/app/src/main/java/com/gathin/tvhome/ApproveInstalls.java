package com.gathin.tvhome;

/**
 * Device patch (MiTV-MTEQ0). Runs as the adb shell, which holds INSTALL_PACKAGES, never inside the app: InstallApprover starts it with
 * `CLASSPATH=<this apk> app_process / com.gathin.tvhome.ApproveInstalls <session id>...`. It accepts the given
 * install sessions, standing in for the confirmation screen Xiaomi's package installer lacks.
 */
public final class ApproveInstalls {

    public static void main(String[] args) throws Exception {
        Object packageManager = Class.forName("android.app.ActivityThread").getMethod("getPackageManager").invoke(null);
        Object installer = packageManager.getClass().getMethod("getPackageInstaller").invoke(packageManager);
        for (String id : args) {
            installer.getClass().getMethod("setPermissionsResult", int.class, boolean.class)
                    .invoke(installer, Integer.parseInt(id), true);
            System.out.println("approved " + id);
        }
    }
}
