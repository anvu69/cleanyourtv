# Shrink and optimise, but keep class and method names: the size win comes from dropping unused
# AndroidX code, not from renaming, and readable stack traces matter more in a sideloaded app
# where crash reports arrive as screenshots.
-dontobfuscate

# Started by name through app_process as the adb shell (InstallApprover), never called from app code.
-keep class com.gathin.tvhome.ApproveInstalls { public static void main(java.lang.String[]); }
