package com.gathin.tvhome;

import android.service.notification.NotificationListenerService;

/**
 * Keeps HomeKeyService enabled. Xiaomi China firmware force-stops every third-party app when the TV goes to
 * sleep, and Android then removes a force-stopped app's accessibility service from the enabled list for good.
 * Notification listeners are rebound after the process dies, so this one comes back and restores it.
 */
public class HomeGuardService extends NotificationListenerService {

    @Override
    public void onListenerConnected() {
        HomeKeyService.ensureEnabled(this);
        InstallApprover.start(this);
    }
}
