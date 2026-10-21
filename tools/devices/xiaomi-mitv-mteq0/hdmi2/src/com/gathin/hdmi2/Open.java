package com.gathin.hdmi2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** One tap to the HDMI 2 input on Xiaomi China firmware (MiTV-MTEQ0): HW6 is source 24. */
public class Open extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            startActivity(new Intent("com.xiaomi.mitv.tvplayer.EXTSRC_PLAY")
                    .setClassName("com.xiaomi.mitv.tvplayer", "com.xiaomi.mitv.tvplayer.ExternalSourceActivity")
                    .putExtra("input", 24)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, "HDMI 2 unavailable", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
}
