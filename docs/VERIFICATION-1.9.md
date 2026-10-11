# WWave 1.9 verification — 2026-10-11

Local signed build, versionCode 10, target SDK 32. This is not a multi-model hardware certification or a Google Play release.

## PASS — physical TCL Android 12

- Installed over 1.8 with matching signature; day/night 30/10, 21:00–09:00 retained. Default driver remains TCL; Sony/Hisense are opt-in.
- Native backlight read and small real change **100→99→100**, after startup had settled. Existing TVActivity task 979 stayed resumed during those commands.
- Compatibility dialog reads native scale 0–100/step 1 and current level. Driver-selection dialog visibly distinguishes TCL and experimental Sony/Hisense; all three dropdown choices are present. TCL's PSK field is disabled.
- Accelerated alarms repeated with ADB server shut down before each firing. Native read after night firing returned **10**, before=30; after day firing returned **30**, before=10. No lastError; working=false; enabled=true.
- Final daily alarms: **2026-10-11 21:00** / **2026-10-12 09:00**. Exact-alarm app-op remains `default`.
- Returned to original `com.tcl.tv/.TVActivity`, task 979. Restored the original input's level 100 after timer testing; native readback confirms 100. Scheduled levels remain day30/night10.
- Installer SHA-256 equals the installed base.apk: `16819f420813d33b9c49ea822fefa0b962f89104c1157c488d1994691c4665e3`.

## PASS — protocol fixtures / Android runtime tests

- JVM: **34 existing compatibility/rollback/time-window assertions** plus **227 REST/scale/transport assertions** in this run. Some assertions count each polling request; this is not a count of independent supported devices.
- Tests check exact documented paths, auth header, version and panel-only write; Sony capability pair; candidate range/step; quantization; unavailable target; changed scale; incorrect response ID; native false/error responses; HTTP redirects/auth refusal; successful HTTP with ignored hardware write; original-value restoration.
- A separate `local.wwreviewprobe` APK omitted TCL uses-library declarations. On the actual Android 12 TV, synthetic REST tests ran against Android's JSON implementation and an in-process loopback HTTP fixture. The fixture only exercised mock backlight values.
- Same isolated APK confirmed Android Keystore encrypt/decrypt, no plaintext PSK in preferences, key clearing, failed-write marker revocation, and unavailable Sony rejection. No TV panel writer was used by this probe. Probe uninstalled after PASS.
- The isolated probe used the same RestBacklight/LocalTvHttp/BacklightScale/TvApiKey and apply-failure logic as the final build. Later UI scrolling/help text, nativeDetails failure invalidation and boot-receiver export changes were compiled in the final build; no claim that the probe exercised those later changes.
- Final signed APK passed apksigner verification; no OEM jars, native binaries, ADB client, remote hosts or overlays bundled. The test-only JSON jar is downloaded with a pinned SHA-256 and is not included in the APK.

## INFERRED from code review

- Driver saving requires enabled=false and working=false. The active-schedule save rejection was reviewed in code; it was not manually exercised on the final UI.

## UNKNOWN / limits

- No physical Sony/Hisense was tested. Their adapters implement documented contracts but require firmware that exposes them to an app on the same TV via loopback. Consumer coverage, server configuration and write permissions remain unproven.
- Other TCL/iFFALCON firmware; Philips/Xiaomi support; Android 8–11/13+ runtime; reboot; sleep/wake; complete HDMI/SDR/HDR/profile behavior; physical luminance measurements.
- A native level of 100 was observed on the original TV input; later reads in Android/screensaver returned 30. This alone does not establish the cause or certify separate profile behavior. WWave does not enumerate every input/profile or continuously override source switches.
- Prior 1.8 hardware/layout/permission checks remain in `VERIFICATION.md`; they are historical evidence, not automatically rerun 1.9 checks.

See `REVIEW.md` for primary-source links and the changes made after review.
