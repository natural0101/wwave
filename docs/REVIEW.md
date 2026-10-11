# Compatibility review — 2026-10-11

## Result

WWave 1.9 adds two opt-in experimental adapters to the existing TCL adapters. These are implementations of published panel-brightness contracts, not a claim that every TV of these brands works. No Sony or Hisense hardware was available for certification.

| Platform | Implemented path | Evidence and remaining limit |
|---|---|---|
| TCL / iFFALCON | PQ SDK; legacy TVManager | PQ SDK tested on the owner's Android 12 TCL. Legacy uses synthetic fixtures; other firmware remains a candidate. |
| Sony BRAVIA | REST v1.0; capability discovery; read/write `brightness`; native min/max/step | Official BRAVIA Professional documentation explicitly defines this target as screen luminance. Requires both getter and setter in `getSupportedApiInfo`. Physical TV, consumer model coverage and on-device loopback availability untested. |
| Hisense Digital Signage | REST read/write `BackLightValue`, 0–100 | Official DS REST contract. Protocol tested; firmware availability and local port 8091 untested on Hisense. Does not imply consumer Hisense or VIDAA support. |
| Philips Android TV | No adapter yet | Panel backlight is distinct from Ambilight. Current JointSPACE setting IDs/authentication and panel-write semantics need verification on a real firmware. No guessed IDs implemented. |
| Xiaomi / other Android TV / boxes | No adapter yet | Android's generic brightness setting does not prove control of the TV panel. Firmware-specific API still needed. |
| Samsung Tizen / LG webOS | Cannot install this APK | Separate platform application or external controller would be required. |

The Sony and Hisense adapters deliberately use **127.0.0.1 only**. The APIs are documented for IP access, but local loopback access has not been proven on their hardware. Firmware that binds only to its LAN address will be rejected by this implementation. There is no network scan or automatic remote fallback. Until local access and real write/readback tests pass, these adapters are experimental.

## Review findings addressed

- A retained successful-write marker could survive a later refused/ignored write. Read or write failure now invalidates the marker and cached manager. Subsequent explicit attempts reconnect; failed writes never switch drivers.
- The old fixed 0–100 assumption cannot represent Sony's discovered native scale. `BacklightScale` maps UI percentages to raw native steps. Readback verifies the exact raw target. Example: 31% on 0–50 becomes raw 16 and is shown as 32%, not falsely reported as 31%.
- A changed reported scale is rejected before writing. Partial writes use the same driver to attempt restoring the exact original raw level, including zero. Failed restoration remains an explicit error.
- REST work must not block the Service main thread. Startup validation now runs on the worker executor; the foreground notification is established before those calls.
- Reflection no longer converts fatal JVM errors into ordinary compatibility failures.
- The boot/time receiver is no longer exported to ordinary apps. ControlReceiver remains protected by the system DUMP permission.
- Driver changes require a stopped schedule with no change in progress. The configuration screen scrolls, distinguishes experimental adapters and explains whether a PSK is needed. Saving performs only read-only discovery.
- PSK is encrypted using Android Keystore AES-GCM. Empty input removes it. Backup is disabled. Keys and raw response bodies are not logged or included in exception messages.
- HTTP permits only four fixed paths on the TV's own loopback address. No proxy or redirect following. Cleartext is allowed only for 127.0.0.1. Authentication failures, oversized responses and timeouts are errors, not successful writes.

## Important remaining findings

**Current input/profile matters.** The TCL native read returned 100 on TV input and later 30 after entering Android/screensaver. This observation does not establish its cause or prove a complete per-input profile model. A past successful write is firmware evidence, not proof of one global brightness across every input. The schedule applies the current native profile at its execution/wake events. It does not enumerate or modify every HDMI/HDR profile and does not enforce a level after every source switch. Full profile identity tracking and switch detection still need hardware tests; they were not improvised through private Binder IDs or menu automation.

**Not a Google Play readiness review.** This local build still targets SDK 32. Android 13+ runtime, current Play target requirements, reboot, sleep/wake and OEM lifecycle testing remain work before broad distribution.

## Sources and why they matter

- [Sony official REST reference](https://pro-bravia.sony.net/ja/remote-display-control/rest-api/reference/): discovery under `/sony/guide`, picture settings under `/sony/video`, luminance target `brightness`, numerical candidates and active-input availability. No other picture target is written.
- [Sony REST authentication](https://pro-bravia.sony.net/remote-display-control/rest-api/structure/): generic writes require authentication; PSK uses `X-Auth-PSK`.
- [Hisense official REST v1.0.1](https://www.hisense-b2b.com/en/Attachment/DownloadFile?downloadId=698): GET current backlight and POST new backlight; 0–100; optional configured `Auth-PSK`; code/isSuccess/data must all agree before verification.
- [Hisense DS SDK v1.1.5](https://www.hisense-b2b.com/en/Attachment/DownloadFile?downloadId=850): listed MTK9666 Android 11 DM66D/GM50D/WH80E models, asynchronous SDK readiness and a vendor jar. This separate SDK was not bundled or treated as consumer support.
- [Philips picture controls](https://www.usa.philips.com/c-f/XC000017988/how-do-i-adjust-the-picture-settings-on-my-philips-android-television): backlight must be distinguished from other picture adjustments.
- [Android TV Media Quality framework](https://developer.android.com/training/tv/mqf): global default profiles require system permissions; it is not a general fallback for an ordinary APK.

Tests and hardware evidence are recorded separately in `VERIFICATION-1.9.md` (prior 1.8 evidence remains in `VERIFICATION.md`).
