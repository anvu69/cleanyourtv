import java.lang.reflect.Method;
import java.util.List;

/**
 * Approves install sessions stuck waiting for confirmation. Xiaomi's package installer on this firmware has no
 * CONFIRM_INSTALL screen, so app stores that install through PackageInstaller sessions (Aptoide) hang forever.
 * Run from the adb shell, which holds INSTALL_PACKAGES:
 *   CLASSPATH=/data/local/tmp/tvtools.dex app_process / ApproveInstalls          (list sessions)
 *   CLASSPATH=/data/local/tmp/tvtools.dex app_process / ApproveInstalls approve  (approve committed ones)
 */
public class ApproveInstalls {
    public static void main(String[] args) throws Exception {
        Object pm = Class.forName("android.app.ActivityThread").getMethod("getPackageManager").invoke(null);
        Object installer = pm.getClass().getMethod("getPackageInstaller").invoke(pm);
        Object slice = installer.getClass().getMethod("getAllSessions", int.class).invoke(installer, 0);
        List<?> sessions = (List<?>) slice.getClass().getMethod("getList").invoke(slice);
        Method approve = installer.getClass().getMethod("setPermissionsResult", int.class, boolean.class);
        for (Object s : sessions) {
            int id = (Integer) s.getClass().getMethod("getSessionId").invoke(s);
            String by = (String) s.getClass().getMethod("getInstallerPackageName").invoke(s);
            float progress = (Float) s.getClass().getMethod("getProgress").invoke(s);
            System.out.println("session " + id + " by " + by + " progress " + progress);
            // Progress reaches 0.8 once the installer app has committed the session.
            if (args.length > 0 && args[0].equals("approve") && progress >= 0.8f) {
                approve.invoke(installer, id, true);
                System.out.println("  approved " + id);
            }
        }
    }
}
