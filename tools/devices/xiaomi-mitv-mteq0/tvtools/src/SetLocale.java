import android.content.res.Configuration;
import android.os.LocaleList;

/**
 * Sets the system locale list from the adb shell (shell holds CHANGE_CONFIGURATION).
 * Usage: CLASSPATH=/data/local/tmp/setlocale.dex app_process / SetLocale vi-VN,en-US
 */
public class SetLocale {
    public static void main(String[] args) throws Exception {
        Object am = Class.forName("android.app.ActivityManager").getMethod("getService").invoke(null);
        Configuration config = (Configuration) am.getClass().getMethod("getConfiguration").invoke(am);
        if (args.length > 0) {
            config.setLocales(LocaleList.forLanguageTags(args[0]));
            Configuration.class.getField("userSetLocale").setBoolean(config, true);
            am.getClass().getMethod("updatePersistentConfiguration", Configuration.class).invoke(am, config);
        }
        System.out.println("locales=" + ((Configuration) am.getClass().getMethod("getConfiguration").invoke(am)).getLocales().toLanguageTags());
    }
}
