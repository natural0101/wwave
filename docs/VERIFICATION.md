# WWave 1.8 verification — 2026-10-11

Scope: broader TCL firmware handling, truthful compatibility detection, failure handling, schedule permission handling and TV layout. This is not certification of multiple physical TV models.

## PASS on real hardware

Device: TCL G10 / C6KS (Build.MODEL reports Smart TV Pro), Android 12. APK versionCode 9, target SDK 32.

- Upgrade retained enabled state, day/night 30/10 and 21:00–09:00.
- Detection chose TCL PQ SDK. Initial `connection` returned 30 and `WRITE_VERIFIED=false`; after real **30→31→30**, it returned `WRITE_VERIFIED=true`. YouTube task 958 remained resumed.
- Visible compatibility dialog read 30/100, identified TCL PQ SDK, Android 12, confirmed tested writing and exact-timer permission.
- Exact alarm app-op was temporarily changed from `default` to `deny`. Starting the schedule returned a clear permission error, persisted `enabled=false` and did not alter brightness. Compatibility dialog showed the missing permission and the permission-settings button.
- The first permission-revocation trial also exposed Android's ForegroundServiceStartNotAllowedException. Before final build, startup refusal handling was repaired in ScheduleService/MainActivity/BootReceiver/AlarmReceiver. Permission and enabled state were restored; the final trial passed.
- Visible night-test and disable controls changed 30→10→30. Disable left `enabled=false`, no pending WWave alarms and no active ScheduleService.
- Re-enabled with the visible switch. Accelerated alarms then performed **30→10→30**, with PC ADB stopped before each firing and briefly reconnected only for native readback. Final readback: 30; day result confirms preceding 10. No lastError, working=false, enabled=true.
- Daily alarms restored: 2026-10-11 21:00 and 2026-10-12 09:00. Original exact-alarm app-op `default` restored. YouTube resumed.

## PASS with explicit test conditions

- JVM suite: **34 assertions** in `tests/CompatibilityTests.java`. Modern and legacy contracts, absent classes, LinkageError, denied reads, invalid writer signatures, invalid levels, no-op detection, write refusal, ignored writes, partial writes and rollback, failed rollback, original zero restoration, midnight/day-window boundaries and invalid schedule inputs.
- Legacy TVManager uses a synthetic fixture. No actual legacy TV was tested.
- Isolated Android package deliberately omitted all OEM uses-library declarations. Read returned an unsupported error, UI disabled schedule/night/day controls, attempted start left enabled=false and no service. Package was uninstalled.
- Same isolated Activity used an app-only 640×360dp/480dpi configuration, without changing TV-wide resolution or density. Cards stacked vertically and DPAD navigation scrolled to the disable button and compatibility status. Ordinary 1920×1080 view was also inspected.
- This isolated APK was built before the final background-start repair; the tested absence-of-library and layout paths were unchanged by that repair. It is not a distributed artifact.
- Signed release APK passed apksigner verify. No OEM binaries, keys, ADB client, INTERNET permission, overlays or menu automation.

## UNKNOWN

Physical legacy TCL/iFFALCON; other TCL firmware; Sony/Hisense/Philips/Xiaomi hardware; Android 8–11/13+ runtime; full reboot, screen-off/wake, HDMI/SDR/HDR/profile switching, physical luminance measurements. Android 13+ receiver flag is compiled, not exercised on Android 13+.

See `COMPATIBILITY.md` for the candidate/support matrix. New model support requires real write/readback and restore tests, not brand-name matching.
