package com.gathin.tvhome;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Base64;

import com.tananaev.adblib.AdbBase64;
import com.tananaev.adblib.AdbConnection;
import com.tananaev.adblib.AdbCrypto;
import com.tananaev.adblib.AdbStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.GeneralSecurityException;

/**
 * Device patch (MiTV-MTEQ0). Runs shell commands through the TV's own adb daemon on 127.0.0.1:5555, with a key kept in app storage.
 * The first connection shows the TV's "Allow USB debugging?" prompt; tick "Always allow" there.
 * Callers pass fixed commands only: whoever runs a command here has the adb shell's permissions.
 */
final class LocalAdb {

    private static final int PORT = 5555;
    private static final AdbBase64 BASE64 = data -> Base64.encodeToString(data, Base64.NO_WRAP);
    private static AdbCrypto crypto;

    private LocalAdb() {
    }

    /**
     * Keeps adbd running for this process: Xiaomi China firmware's settings app switches it off a few seconds
     * after every boot, so turn it back on now and whenever it goes off. Needs WRITE_SECURE_SETTINGS.
     */
    static void keepEnabled(Context context) {
        if (context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS)
                != PackageManager.PERMISSION_GRANTED) return;
        ContentResolver resolver = context.getContentResolver();
        Runnable enable = () -> {
            if (Settings.Global.getInt(resolver, Settings.Global.ADB_ENABLED, 0) == 0) {
                Settings.Global.putInt(resolver, Settings.Global.ADB_ENABLED, 1);
            }
        };
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ADB_ENABLED), false,
                new ContentObserver(new Handler(Looper.getMainLooper())) {
                    @Override public void onChange(boolean selfChange) {
                        enable.run();
                    }
                });
        enable.run();
    }

    /** Blocks until the command ends, and while the TV waits for the user to allow this key: never call on the main thread. */
    static String run(Context context, String command) throws IOException, InterruptedException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", PORT), 3000);
            AdbConnection connection = AdbConnection.create(socket, crypto(context));
            connection.connect();
            AdbStream stream = connection.open("shell:" + command);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                while (true) out.write(stream.read());
            } catch (IOException closed) {
                // the command ended and adbd closed the stream
            }
            connection.close();
            return out.toString("UTF-8");
        }
    }

    private static synchronized AdbCrypto crypto(Context context) throws IOException {
        if (crypto != null) return crypto;
        File privateKey = new File(context.getFilesDir(), "adbkey");
        File publicKey = new File(context.getFilesDir(), "adbkey.pub");
        try {
            if (privateKey.isFile() && publicKey.isFile()) {
                crypto = AdbCrypto.loadAdbKeyPair(BASE64, privateKey, publicKey);
            } else {
                crypto = AdbCrypto.generateAdbKeyPair(BASE64);
                crypto.saveAdbKeyPair(privateKey, publicKey);
            }
        } catch (GeneralSecurityException e) {
            throw new IOException("adb key", e);
        }
        return crypto;
    }
}
