# WWave 1.6 verification — 2026-10-07

Device: TCL G10 / C6KS, Android 12, Russian settings UI.
APK: `WWave-1.6.apk`, package `local.tclbrightness`, versionCode 7, target SDK 32.

Verified on the device:

- Clean-install authorization: an isolated test package generated its own RSA key, displayed the system ADB authorization prompt, and connected as `uid=2000(shell)` after approval. A subsequent connection succeeded without another prompt. The isolated test package was then uninstalled.
- Upgrade from the installed app retained its existing key, day/night levels 30/10, schedule 21:00–09:00 and enabled state.
- Real brightness cycle through accelerated alarms: **30 → 10 → 30**, read back from the native TCL menu. The PC ADB server was stopped before each alarm fired. Intermediate readback used a brief reconnect between the two alarms.
- The app screen returned after the menu operation. The header contains a single pastel logo with transparency and no duplicate WW text.
- The visible disable button returned day brightness, disabled the schedule and left **0 active alarms / 0 ScheduleService records**. The schedule was re-enabled afterwards.
- The signed APK passed `apksigner verify`; its ZIP entries contain no ADB keys or keystores.

Representative diagnostic commands (with your already-authorized ADB connection):

```text
adb shell am broadcast -n local.tclbrightness/.ControlReceiver --es op connection
adb shell am broadcast -n local.tclbrightness/.ControlReceiver --es op status
adb shell am broadcast -n local.tclbrightness/.ControlReceiver --es op testTimers
adb shell dumpsys alarm
adb shell dumpsys activity services local.tclbrightness
```

Do not run a separate UI automation dump while the app is changing brightness: it can interfere with the native-menu readback.

Not verified: a complete reboot, separate HDMI/HDR profiles, other TCL firmware/menu languages, other Android versions, or automatic placement in every third-party launcher. This is a preview release, not a claim of universal TCL compatibility.
