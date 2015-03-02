---
title: Building Thali Device Hub
layout: default
---

Clone and build:

<pre>
  mkdir thali
  cd thali
  git clone https://git01.codeplex.com/thali
  cd Production\ThaliDeviceHub\android\android
  gradlew assembleDebug
</pre>

Output:

```
  :android:compileDebugNdk
  :android:preBuild UP-TO-DATE
  ...
  :android:packageDebug
  :android:assembleDebug
  BUILD SUCCESSFUL
```

To run it in the Virtual Box emulator, find its IP address:

<pre>
  Alt-F1 in VirtualBox to bring up terminal
  # netcfg
  eth0  UP   192.168.1.116/24
</pre>

With ANDROID_SDK/platform-tools on the path, connect adb to the emulator:

<pre>
  # adb connect 192.168.1.116
  * daemon not running. starting it now on port 5037 *
  * daemon started successfully *
  connected to 192.168.1.116:5555
</pre>

[Tip: On Windows, anyway, a useful sanity check when things fail is to kill and restart the adb process.]

Now push the app to the emulator, using the full path to the built apk:

<pre>
  adb install C:\Users\Jon\AndroidDev\thali\Production\ThaliDeviceHub\android\android\build\apk\android-debug-unaligned.apk
   6019 KB/s (2428409 bytes in 0.393s)
     pkg: /data/local/tmp/android-debug-unaligned.apk
</pre>

Accept or decline:

[[File:VirtualBox_loading_app.png]]

[Tip: You may need to fiddle with Machine->Disable Mouse Integration in VirtualBox to get control of the mouse.]

[Tip: You may need to run VirtualBox on your primary monitor to control the mouse.]

[Tip: Once the mouse is captured, Right-CTRL to uncapture.]

The running device hub:

[[File:ThaliDeviceHubRunningInEmulator.png]]
