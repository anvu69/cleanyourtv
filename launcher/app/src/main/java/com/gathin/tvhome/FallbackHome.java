package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;

/**
 * Xiaomi China firmware with its stock launcher disabled: Home resolves to TV Settings' empty FallbackHome, and
 * HomeKeyService puts this launcher in its place. Any TV Settings screen started normally joins FallbackHome's task,
 * and closing it uncovers FallbackHome without a window event, leaving a black screen. Those screens get a task
 * of their own instead.
 */
final class FallbackHome {

    private FallbackHome() {
    }

    static boolean isFallbackHome(CharSequence className) {
        return className != null && className.toString().endsWith(".FallbackHome");
    }

    /** The package of FallbackHome when it is the system's Home, else null. */
    static String homePackage(Context context) {
        ResolveInfo home = context.getPackageManager().resolveActivity(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        return home != null && isFallbackHome(home.activityInfo.name) ? home.activityInfo.packageName : null;
    }

    /** Adds NEW_TASK, plus MULTIPLE_TASK when the target lives in FallbackHome's package. Returns the intent. */
    static Intent ownTask(Context context, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        String fallback = homePackage(context);
        if (fallback == null) return intent;
        ComponentName target = intent.resolveActivity(context.getPackageManager());
        if (target != null && target.getPackageName().equals(fallback)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }
        return intent;
    }
}
