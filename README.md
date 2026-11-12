# OnePlus OxygenOS Debloat (ADB)

> ## ⚠️ PRE-RELEASE — v0.9.0-beta
>
> This is a **pre-release**. Things can break, and the package lists may not fit every model or region.
>
> **It has not been run on a physical OnePlus device yet.** It was verified only in an emulated
> Windows environment (Wine) against a self-built `adb` stub. Everything it does is reversible,
> but you are using it at your own risk.
>
> Feedback, package names from other OnePlus models and corrections are very welcome — please open an issue.

A single Windows batch file that **freezes** bloatware, telemetry and unused system apps on OnePlus
phones instead of uninstalling them. No root, no Shizuku, no Canta, no App Manager — just a USB cable
and `adb` — plus an optional firewall blocklist against trackers and a clear update path via
**OS Updater**, because the firewall switches the OTA services off.

German version: [README.de.md](README.de.md)

---

## What it does

- Freezes apps with `pm disable-user --user 0` — the app disappears, consumes no battery, and **nothing is deleted**
- Three curated lists: **71 safe**, **23 optional** and **9 never-touch** packages
- Verifies every action through `pm list packages -d` instead of trusting adb output
- Skips packages that do not exist on your device (lists are model and region dependent)
- Optional AppOps step: blocks background execution and wake locks of five telemetry services
- Full rollback at any time (menu 4, or the log file `eingefroren.log`)
- Logs every freeze so a rollback also covers packages you froze individually

## What it does not do

- No uninstalling, no data wiping, no deleting of app data
- No root, no custom ROM, no firmware flashing
- No DNS blocking — use the optional firewall blocklist below, or a NextDNS profile (chapter 6 in
  `docs/oneplus_cleanup.pdf`)
- No UI settings (autostart, notifications, battery optimisation) — those are switches inside OxygenOS

## Requirements

- Windows 10/11
- USB cable
- [Android Platform-Tools](https://developer.android.com/tools/releases/platform-tools) — **unzip** them, keep the
  `platform-tools` folder next to the `.bat`
- USB debugging enabled on the phone (Settings → About phone → tap *Build number* 7× → Developer options)
- Screen unlocked during the run

## Quick start

1. Download this repo (green *Code* button → *Download ZIP*) and **extract** it. Do not run the batch
   file from inside the ZIP.
2. Download Platform-Tools, extract them, and put the `platform-tools` folder next to `oneplus_cleanup.bat`.
3. Connect the phone, allow the *USB debugging* prompt with **Always allow**.
4. Double-click `oneplus_cleanup.bat`.
5. Run **1** (overview) first — it changes nothing and shows what your device actually has.
6. Then run **2** (safe). Reboot once.
7. When you are done: turn **USB debugging off** again.

## Menu

- **1 — Overview**: per package, whether it exists on the device and whether it is active or frozen. Read-only.
- **2 — Safe**: freezes the 71 safe packages (telemetry, ads, logs, account scaffolding, test tools).
- **3 — Optional**: safe list **plus** 23 feature-dependent packages — Chrome, Photos, Android Auto,
  “Hey Google”, eID, **Private Safe** (contents become unreachable), step counter, work profile/Intune,
  carrier Wi-Fi calling module.
- **Pick your own in 2 and 3**: before starting, the script asks `a` = all or `w` = choose. With `w`, every
  package that exists on the device and is still active is asked one by one: `j` freeze, `n` skip,
  `a` = rest of the group, `k` = skip the rest of the group. In menu 3 the safe and the optional list are chosen
  separately (all / choose / `k` = none) — e.g. only a few optional packages.
- **4 — Reset**: re-enables everything this script froze. Works at any time.
- **5 — AppOps**: blocks `RUN_ANY_IN_BACKGROUND`, `RUN_IN_BACKGROUND` and `WAKE_LOCK` for
  `statistics.rom`, `logkit`, `onetrace`, `postmanservice`, `ocloud`. Survives reboots.
- **6 — AppOps reset**: sets those three ops back to `default`.
- **8 — Status**: post-update check, changes nothing. Shows per AppOp whether it is still blocked (`[ok]`) or
  allowed again (`[OFFEN]`), and which packages from `eingefroren.log` are active again. If something differs,
  just run 5 or 2/3/7 again.
- **7 — Single**: freeze (`e`) or release (`f`) **any** app or service, including packages from the safe/optional
  lists — this lets you skip 2 and 3 entirely. `?word` searches installed packages (e.g. `?heytap`; a bare `?`
  lists all, `[aus]` = frozen). The prompt stays open until you press Enter on an empty line. Only the system
  base (launcher, SystemUI, phone, settings) and the never-touch list cannot be frozen here, but they can be released.

## How “freezing” works

```bat
adb shell pm disable-user --user 0 <package>
```

The package stays installed but is disabled for the current user. Undo:

```bat
adb shell pm enable --user 0 <package>      :: what menu 4 does
adb shell pm install-existing <package>     :: brings back something removed with Canta/ADB
```

A factory reset also restores everything.

## Extended option: network blocking (kill trackers)

Freezing only helps with apps you want gone entirely. Services that have to stay **active** — launcher,
system UI, camera, security centre — still phone home. That is what the second layer is for: a firewall with
per-app rules.

- App: **FireWall Blocks** (`com.shayan.firewall`) from IzzyOnDroid — open source, no root, two modes
  (Shizuku or local VPN): https://apt.izzysoft.de/fdroid/index/apk/com.shayan.firewall
- Ready-made list: `firewall/oppo-blocklist.json` — 27 OPPO/ColorOS/OnePlus services, Wi-Fi **and** mobile data blocked
- Readable version incl. “what breaks”: `docs/firewall-blocklist.md`

How to apply:

1. Install FireWall Blocks and pick **Shizuku** mode (lighter than VPN, no permanent VPN key icon).
2. Import `firewall/oppo-blocklist.json`. Note: importing **replaces** every existing rule in the app.
3. Flip “Enable” and start Shizuku.
4. **After every reboot**: start Shizuku and open the app once — otherwise the rules are not active.
5. Sanity check: browser, messenger and Play Store must keep working; the weather widget stays empty.

The list is a snapshot from a working device. If your model lacks a package, the app silently ignores it.
Anything not in the list stays allowed — that is the app's default.

## ROM updates: only via OS Updater

The firewall blocks `com.oplus.ota`, `com.oplus.cota` and `com.oplus.romupdate` — exactly the services
OxygenOS uses to fetch updates. That is intended (no unrequested downloads, no telemetry), but it has a
consequence: **the update function in settings no longer finds anything.**

Updates therefore go through **OS Updater** (formerly “Oxygen Updater”):
https://play.google.com/store/apps/details?id=com.arjanvlek.oxygenupdater

1. Install the app. It is open source and the ads can be removed with an in-app purchase.
2. Check device/region, select the *Stable* channel.
3. Download the update — the app pulls the official file straight from the vendor and verifies its MD5 sum.
4. Follow the in-app install guide (“Local upgrade”). The system performs the install; the app cannot flash a ROM.
5. **If the install fails** (“installation failed”): temporarily allow `com.oplus.ota` and
   `com.oplus.romupdate` in FireWall Blocks, retry, then block them again.

Security patches ship in the same package and take the same route.

## Package lists

| Group | Count | Meaning |
|---|---|---|
| Safe | 71 | Remove without losing a feature |
| Optional | 23 | Feature or taste — read the description first |
| Never touch | 9 | Dialer, Contacts, Messages, Gboard, Gmail, Calendar, battery monitor, update service, Health-Connect overlay |

The never-touch group exists because public debloat lists mark several of these as “recommended” —
on OnePlus that costs you gestures, face unlock, media storage or updates.

Some packages were deliberately moved to *Optional* after review: `work.clouddpc` (school/MDM enrolment),
`uimGbaApp` (carrier Wi-Fi calling), `healthconnect.controller`, `locationproxy`.

## Safety notes

- **Back up first.** Freezing does not delete app data, but a backup costs five minutes.
- Private Safe (menu 3) hides your files while frozen — unlock and copy them out first.
- Do not freeze `com.oplus.sauhelper` or `com.oplus.powermonitor`: no system updates, no battery stats.
- USB debugging is a real security hole. Turn it off when you are done.
- One device at a time. The script refuses to run with two devices attached.
- If a freeze fails, the script briefly disables and re-enables `com.android.shell` — a known OnePlus
  workaround for “not allowed to disable”. It only happens when a freeze actually fails.

## Known limitations

- Tested on Wine + stub `adb` only, not on real hardware yet
- Package names differ between models, regions and OxygenOS versions — missing ones are skipped silently
  (listed as “not on device”)
- The `com.android.shell` workaround is unproven on the newest OxygenOS builds
- Chinese ROMs (ColorOS-based) contain many more packages than the lists here
- AppOps names can change between Android versions; menu 5 prints only what actually succeeded
- The firewall list has not been tested on a device: its format and value semantics were verified against the
  FireWall Blocks source code, the import itself was not
- The OS Updater route including “Local upgrade” is untested as well

## Troubleshooting

- **“adb not found”** — Platform-Tools not extracted, or the folder is not next to the `.bat`
- **“No phone in debug mode”** — cable, USB debugging, or the phone is in charging-only mode (pull down the
  notification and select *File transfer*)
- **“Phone connected but not authorised”** — tap *Allow* on the phone, then restart the script
- **“Phone does not answer / folder read-only”** — script started from inside the ZIP, or screen locked
- **Freeze fails for a single package** — the package name may not exist on your model; check menu 1

## Repo contents

- `oneplus_cleanup.bat` — the tool (Windows, CRLF)
- `ANLEITUNG-BAT.txt` — German quick guide
- `README.de.md` — German readme
- `docs/oneplus_cleanup.pdf` — the long German guide: 12 areas from the android-hilfe.de thread, mapped to OxygenOS
- `docs/canta_kontrolle.pdf` — review of a Canta preset list (which entries would break core features)
- `docs/expert-review.md` — independent code review (German), including the bugs fixed for v0.9.0
- `docs/firewall-blocklist.md` — the firewall list explained (German/English), including side effects
- `firewall/oppo-blocklist.json` — import file for FireWall Blocks
- `firewall/oppo-blocklist.txt` — the same packages as a plain list to type in by hand
- `tools/template_bat.txt` — template with `@@SAFE@@` / `@@OPT@@` / `@@NEVER@@` placeholders to rebuild the `.bat`

## Disclaimer

No warranty. You modify your own device at your own risk. This project is not affiliated with OnePlus,
Oppo, Google or with Canta / App Manager / Universal Android Debloater. Keep your phone recoverable:
know how to reach recovery and have your account credentials at hand.

## Credits

Inspired by the android-hilfe.de thread “China-Version Clean Up & Optimize für DAUs”, by
[Universal Android Debloater](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
(list research only) and by [Canta](https://github.com/samolego/Canta) / App Manager as the Shizuku-based
alternatives this script replaces. Firewall rules based on
[FireWall Blocks](https://github.com/shynoiddev/FireWall-Blocks), update path via
[OS Updater](https://oxygenupdater.com/).
