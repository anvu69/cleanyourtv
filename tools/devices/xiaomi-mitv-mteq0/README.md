# Xiaomi MiTV-MTEQ0 device branch

This branch is upstream CleanYourTV plus patches that only make sense on a Xiaomi China-firmware
MiTV-MTEQ0 (baymax). What the firmware does and why is in
[`docs/devices/xiaomi-mitv-mteq0-baymax.md`](../../../docs/devices/xiaomi-mitv-mteq0-baymax.md); the
debloat list is [`examples/xiaomi-mitv-mteq0/debloat.txt`](../../../examples/xiaomi-mitv-mteq0/debloat.txt).

## What is on top of upstream

Proposed upstream (inert on other TVs), Gathin23/cleanyourtv#611:

- `Config`: the theme overlay is read from a copy in app storage while external storage is not
  mounted yet (Home starts before it at every boot on this set). PR #614.
- The debloat list and device notes. PR #612.

Device patches, kept out of upstream on purpose:

- **Vietnamese font** (`res/font-vi`): Lexend, because Outfit has no Vietnamese glyphs. Withdrawn
  from upstream (#613): even subset it adds about 40 KB and upstream keeps the APK under 420 KB.

- **HDMI/AV switching** (`MainActivity.xiaomiSourceIntent`): Inputs rows start
  `com.xiaomi.mitv.tvplayer/.ExternalSourceActivity` with `EXTSRC_PLAY` and the `input` extra
  (HDMI `HW<n>` is n + 18, AV `HW<n>` is n, CEC devices go through their parent port).
- **Install approver** (`LocalAdb`, `InstallApprover`, `ApproveInstalls`, `com.tananaev:adblib`,
  `INTERNET`): Xiaomi's package installer has no `CONFIRM_INSTALL` screen, so split-APK installs
  from Aptoide hang. With `auto_approve_installers` in `config.json` the launcher keeps adb on
  (the firmware switches it off after every boot), and accepts committed sessions from the listed
  stores by running `ApproveInstalls` from its own APK as the adb shell over `127.0.0.1:5555`.
  Trade-off: adb stays open on the LAN (a new key still needs the on-screen approval) and the
  launcher holds an adb key. Remove the key from `config.json` to turn it all off.
- `hdmi2/`: a 20 KB shortcut app ("HDMI 2") that opens one input; useful as a dock tile.
- `tvtools/`: `SetLocale` (set the system locale list; the firmware has no picker for it) and
  `ApproveInstalls` (accept stuck install sessions by hand), both run with `app_process`.

## Build and install

The launcher must use the allowlisted package name `com.ts.launcher` (see the AETP0 notes), or
Xiaomi's framework will not let it be Home. Check `pm path com.ts.launcher` prints nothing first.

```bash
export TV_HOST=<tv ip>
PY=tools/venv/bin/python        # tools/venv/Scripts/python.exe on Windows
(cd launcher && ./gradlew assembleRelease -PlauncherApplicationId=com.ts.launcher)
"$PY" -I tools/b64push.py launcher/app/build/outputs/apk/release/app-release.apk /data/local/tmp/ts.apk
"$PY" -I tools/tv.py 'pm install -r /data/local/tmp/ts.apk'
"$PY" -I tools/tv.py 'pm grant com.ts.launcher android.permission.ACCESS_FINE_LOCATION'
"$PY" -I tools/tv.py 'pm grant com.ts.launcher android.permission.WRITE_SECURE_SETTINGS'
"$PY" -I tools/tv.py 'cmd package set-home-activity --user 0 com.ts.launcher/com.gathin.tvhome.MainActivity'
```

Example `config.json`, pushed to `/sdcard/Android/data/com.ts.launcher/files/config.json`:

```json
{
  "dock_apps": ["com.gathin.hdmi2", "tungbui.mstore", "cm.aptoidetv.pt"],
  "hidden_apps": ["com.mitv.tvhome", "org.liskovsoft.androidtv.rukeyboard"],
  "live_tv_package": "com.xiaomi.mitv.tvplayer",
  "auto_approve_installers": ["cm.aptoidetv.pt", "tungbui.mstore"]
}
```

When Home starts with `auto_approve_installers` set, the TV asks "Allow USB debugging?" for the
launcher's key once: tick "Always allow" and press OK. Then also allow the stores to install apps:
`appops set cm.aptoidetv.pt REQUEST_INSTALL_PACKAGES allow`.

`hdmi2/build.sh` and `tvtools/build.sh` use only the Android SDK (no Gradle). As written they run
in Git Bash on Windows with JDK 17 from mise; adjust the tool paths elsewhere.

```bash
"$PY" -I tools/tv.py --push tools/devices/xiaomi-mitv-mteq0/tvtools/tvtools.dex /data/local/tmp/tvtools.dex
"$PY" -I tools/tv.py 'CLASSPATH=/data/local/tmp/tvtools.dex app_process / SetLocale vi-VN,en-US'
"$PY" -I tools/tv.py 'CLASSPATH=/data/local/tmp/tvtools.dex app_process / ApproveInstalls approve'
```

## Verified on the set

Home from HDMI, after a force-stop like the one standby does, and after a reboot; adb back on by
itself 1 s after the firmware turned it off at boot; dock order kept across a reboot. A split-APK
install from Aptoide was approved in 0.7 s by the same approver code in an earlier build installed
as `com.gathin.tvhome`.

## Rollback

```bash
"$PY" -I tools/tv.py 'pm enable --user 0 com.mitv.tvhome'
"$PY" -I tools/tv.py 'cmd package set-home-activity --user 0 com.mitv.tvhome/.MainActivityUserMode'
"$PY" -I tools/tv.py 'pm uninstall com.ts.launcher'
```
