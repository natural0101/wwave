# WWave 1.7 verification — 2026-10-07

Device: TCL G10 / C6KS, Android 12. Package `local.tclbrightness`, versionCode 8, target SDK 32.

Verified on the device:

- Native TCL shared library: an isolated app declared `com.tcl.tv.display`, read back **10/100**, then changed **10 → 15 → 10** using `TvBacklightManager.setBacklight(EN_APPLY_CURRENT, level, EN_TCL_ACT_EXEC_SAVE)`. No shell Binder calls or elevated app permissions were used. The diagnostic app was uninstalled.
- Upgrade to 1.7 retained enabled state, day/night **30/10**, schedule **21:00–09:00**. Package manager reports versionCode 8 and resolves `/system_ext/framework/com.tcl.tv.display.jar`.
- WWave service changed **10 → 30 → 10**; native readback confirmed each level. `com.fgl27.twitch/.PlayerActivity`, task 932, remained resumed.
- Accelerated alarms performed **30 → 10 → 30**. PC ADB server was stopped before each firing, briefly reconnected only for readback. Native service logs: night at **21:20:21**, day at **21:21:01**. Twitch remained the foreground activity.
- Visible **«Проверить доступ»** button showed **10/100**. Visible **«Отключить и вернуть дневную яркость»** button returned **30/100**, set `enabled=false`, left no pending WWave alarms and no ScheduleService records.
- Re-enabled through the visible switch: `enabled=true`, `working=false`, `lastError=`; native value **10/100**. Two daily alarms observed for **2026-10-08 09:00** and **2026-10-08 21:00**. Back returned to the original Twitch activity/task.
- Invalid diagnostic level **101** was rejected and native brightness remained **10/100**.
- Actual settings screenshot inspected for text, logo, controls and status. No duplicate logo text or square background.
- Signed APK passed `apksigner verify`. Source/APK have no ADB client, menu automation, TaskRestore or INTERNET permission. The TCL binary library is not redistributed.

Commands (ADB is needed only for these diagnostics, not app operation):

```text
adb shell am broadcast -n local.tclbrightness/.ControlReceiver --es op connection
adb shell am broadcast -n local.tclbrightness/.ControlReceiver --es op status
adb shell am broadcast -n local.tclbrightness/.ControlReceiver --es op testTimers
adb shell dumpsys alarm
adb shell dumpsys activity services local.tclbrightness
adb shell dumpsys activity activities
```

The generic Android command `cmd display set-brightness` was tested briefly and restored to the original 0.39763778. Its physical effect was not confirmed and it is not used in WWave. TCL native API readback is the evidence for the implemented backend.

Not verified: full reboot, screen-off/wake cycle, HDMI/HDR/profile switching, physical luminance measurement, other TCL firmware or Android versions. The setter targets the current picture profile. No universal compatibility claim.
