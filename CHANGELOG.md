# Changelog

## Unreleased

- **Android app, stage 1 (`android/`, version 0.1.0-alpha).** Kotlin/Compose app with the script's feature
  set: freeze, release, remove for the user, bring back, AppOps lock, post-update check, reset. Access through
  Shizuku; without it a view-only mode with explanations, a jump to the system app-info page and the ADB
  command to copy. Custom list with import/export, German and English, description for all 103 packages.
  The package lists are generated from `oneplus_cleanup.bat` (`tools/gen_app_catalog.py`). Built-in wireless
  ADB is planned as stage 2. **Not tested on a device** – unit tests against a fake phone and JVM screenshots only.
- **Android app 0.2.0-alpha** (after first feedback from a device; Shizuku access works there):
  - "Protected" tab renamed to "Caution" and split: phone, SMS, contacts, calendar, Gmail and Gboard can now be
    frozen one at a time after a prompt. Freezing is refused while a package is the phone's default keyboard,
    dialer, SMS app or launcher. Only the system base and three system components stay locked.
  - Selection is kept across tabs, and "Everything at once" on the home screen freezes whole lists.
  - Home counters now show the real state of the phone (anything disabled or removed, whoever did it); new
    "Off" tab lists these packages for releasing or bringing back.
  - Help: how to start Shizuku from a PC over ADB, and why no one-time ADB permission grant exists.
- **Android app 0.3.0-alpha – "Other ways" for apps the vendor will not let go.** Newer OxygenOS versions
  ignore `pm disable-user` for some system apps *and* refuse `pm uninstall -k --user 0`
  ("only root can delete system app for a particular user"). The app now offers **Other ways …** (detail view,
  and in the result dialog after a failed freeze or removal). It tries, in order, and stops at the first step
  that is confirmed by reading the state back:
  1. remove for the user (as before),
  2. **hide as device owner** (`DevicePolicyManager.setApplicationHidden`) – only if the app was set up as
     device owner (Help → Device owner, or `adb shell dpm set-device-owner …`; needs no accounts and no second
     user at set-up time; "Give up" unhides everything first),
  3. **suspend** (`pm suspend` + force-stop + `RUN_ANY_IN_BACKGROUND`/`RUN_IN_BACKGROUND` ignored per package,
     never per uid + standby bucket `restricted`). Weaker than freezing; shown as "suspended".
  Each step's answer from the phone is shown. New states "hidden" and "suspended" appear in the lists and the
  "Off" tab; release, "Reset everything", the post-update check and "Apply again" cover them. Undoing a
  suspension made by someone else (e.g. Digital Wellbeing) only unsuspends and leaves their settings alone.
  Deliberately not used: `pm disable-until-used`, `pm hide` and disabling single components – AOSP refuses all
  three to the shell user, so they cannot work through Shizuku/adb. **Not tested on a device.**

- **Android app 0.3.3-alpha:** the picker's "Show system apps" switch is replaced by an origin filter – All apps /
  Only your apps / Only system apps – with counts; it combines with the state filter. Default is "All apps".
- **Android app 0.3.2-alpha:** the "Add apps" picker can filter by state – All, Active, Frozen, Suspended,
  Removed/hidden – with counts. The filters for switched-off apps always include system apps (nearly all of
  them are), and each row shows its state.

## v0.9.8-beta — pre-release

- **Menu 7: suspend when removing is refused too.** Newer OxygenOS answers `pm uninstall -k --user 0` for some
  system apps with `Failure [only root can delete system app for a particular user]` (confirmed on a device with
  `com.oplus.gleanerservice`). Menu 7 now offers `s` = suspend after a failed removal, or directly instead of
  removing: `pm suspend`, `am force-stop`, `RUN_ANY_IN_BACKGROUND`/`RUN_IN_BACKGROUND` = ignore (per package, never
  per uid), standby bucket `restricted`. Success is read back from `dumpsys package` (`User 0: … suspended=true`).
- Suspended packages are logged in `stillgelegt.log`. Menu 4 undoes them (`pm unsuspend`, app-ops `default`,
  bucket `active`; the log is kept if one fails), menu 7 shows "stillgelegt" with `f` = release or `e` = try
  freezing again, menu 1 shows `[stillgelegt]`, menu 8 reports `[WIEDER AKTIV]`.
- Not offered on purpose: `pm disable-until-used`, `pm hide`, disabling components – AOSP refuses them to the
  shell user. The Android app can additionally hide apps as device owner.
- Tested under Wine against a fake `adb` that behaves like OxygenOS 17 (freeze ignored, removal refused).
- **Android app 0.3.1-alpha:** the buttons above the custom list wrap on narrow screens or large fonts instead of
  squeezing "Export" into a vertical column.

## v0.9.7-beta — pre-release

- **Menu 7: remove for the user when freezing is ignored.** On a real device `com.heytap.browser` answered
  `pm disable-user` with `new state: default` — the phone accepts the command but keeps the app enabled. Menu 7
  now shows the phone's answer and offers `u` = remove for the user (`pm uninstall -k --user 0`, the method
  Canta uses). The system copy and the app data stay; `pm install-existing --user 0` brings it back.
  Only offered after a failed freeze and only after `u`; never-touch and system-base packages stay locked.
- Removed packages are logged in `entfernt.log`. Menu 4 restores them (the log is kept if a restore fails),
  menu 7 shows them as "entfernt" and brings them back with `f`, menu 1 shows `[entfernt]`, menu 8 reports
  `[WIEDER DA]` if an update reinstalled one.
- Releasing (`f`) in menu 7 now also removes the package from the log, so menu 8 no longer reports a
  deliberately released package as "wieder aktiv".
- Menus 2/3: a failed freeze now points to menu 7.
- Tested in Wine against a stub that reproduces the device answer (`new state: default`) and a refused
  uninstall (`DELETE_FAILED_INTERNAL_ERROR`); not yet on a device.

## v0.9.6-beta — pre-release

- **Version display.** The version is shown in the window title, in a startup line (`[i] OnePlus Clean-Up v…`)
  and in the menu header, so a user can tell at a glance which script they are running. It lives in one
  place (`APP_VERSION` at the top of `oneplus_cleanup.bat` and `tools/template_bat.txt`).
- **Renamed to "OnePlus OxygenOS & ColorOS Debloat".** The tool is also used on ColorOS; readmes, the
  instructions, the window title and the menu header now say so. The pre-release banner no longer claims that
  the script was never run on a device, and the known-limitations section reflects the real-device feedback.
- No change to package lists or behaviour.

## v0.9.5-beta — pre-release

- **Menu 5/8: frozen packages and refused AppOps.** Real-device feedback showed `No operations` /
  `Default mode: allow` for `statistics.rom`, `onetrace` and `postmanservice`: the phone accepts
  `appops set` but stores nothing, on package and uid level. ADB cannot override this.
  - Menu 5 skips packages that are already frozen (they cannot run, so the lock is unnecessary) and
    counts them separately; menu 8 shows them as `[ok - eingefroren]`.
  - Refusals are now labelled "vom Telefon abgelehnt", followed by an explanation and the advice to freeze
    the package via menu 7 instead of re-running menu 5.
- Tested in Wine against a stub that reproduces the exact device answer; not yet re-run on the device.

## v0.9.4-beta — pre-release

- **Fix menu 5/8: AppOps were reported as "OFFEN" although they were blocked.** The status check read only the
  first output line of `cmd appops get`; Android prints a separate `Uid mode:` line first, so the package-level
  `ignore` was never seen. All lines are evaluated now. Missing packages are reported as "not on the device"
  instead of "OFFEN".
- Menu 5 now reads every AppOp back after setting it, falls back to `appops set --uid` if the package mode does
  not stick, and reports `[X]` with the phone's raw answer instead of always printing `[op]`. Menu 6 resets both
  levels (package and uid).
- Fix: with no frozen package yet, the temporary list of disabled packages was never created, so menus 1, 2, 3
  and 8 printed "file not found" for every package. The file is now always created.
- Tested in Wine against an `adb` stub with `Uid mode: …allow` plus package-level `ignore` (the reported bug),
  missing packages and a package mode that does not stick; not yet on a device.

## v0.9.2-beta — pre-release

- **Menu 2 and 3: pick your own packages.** Before freezing, choose `a` (all, as before) or `w` (choose):
  every package that exists and is still active is asked one by one (`j`/`n`, `a` = rest of the group,
  `k` = skip the rest). Menu 3 asks for the safe and the optional list separately, each with all / choose / none,
  so e.g. only optional packages can be frozen. The result line now also counts deselected packages.
- **Menu 7: any package.** Packages from the safe and optional lists are no longer blocked, so 2/3 can be
  skipped entirely. New `?word` search over installed packages (frozen ones marked `[aus]`), status display
  before acting, and the prompt stays open for several packages. Never-touch and system-base packages can
  still not be frozen, but can now be released here.
- Tested in Wine against an `adb` stub, not yet on a device.

## v0.9.3-beta — pre-release

- **New menu 8: status check.** Read-only: shows for each AppOp of menu 5 whether it is still blocked or was
  reset (e.g. by a system update) and which packages from `eingefroren.log` are active again, so you no longer
  have to re-run everything blindly.
- Tested in Wine against an `adb` stub, not yet on a device.

## v0.9.1-beta — pre-release

- **New extended option: firewall blocklist.** `firewall/oppo-blocklist.json` (27 OPPO/ColorOS/OnePlus
  services, Wi-Fi and mobile data) for the open-source app *FireWall Blocks* (`com.shayan.firewall`),
  importable as JSON. Value semantics verified against the app's source: `true` = blocked.
- **New: ROM updates via OS Updater** (formerly Oxygen Updater). The blocklist disables the OTA services
  `com.oplus.ota`, `com.oplus.cota` and `com.oplus.romupdate`, so in-system update checks stop working.
- Both readmes updated (German and English), new `docs/firewall-blocklist.md` with a per-package table of
  what stops working, `ANLEITUNG-BAT.txt` extended, repo description updated.
- Package lists unchanged (71 safe / 23 optional / 9 never-touch).

Still a pre-release: neither the firewall import nor the OS Updater route has been run on a device.

## v0.9.0-beta — first public pre-release

Initial public version. Windows batch tool, German and English documentation, 71 safe / 23 optional /
9 never-touch packages, AppOps step for five telemetry services, full rollback.

Verified in an emulated Windows environment (Wine) against a self-built `adb` stub — not yet on a
physical device.

Fixed before publication:

- Freeze accounting reported "0 frozen, N missing" because the adb wrapper swallowed the caller context
- `-s <serial>` was missing in list/status/getprop calls — empty lists with two devices attached
- Package matching used substring search (`com.google.android.gm` also matched `gms`); now exact line matching
- Reset enabled packages that were active, disabled-at-factory or already re-enabled; now only actually
  disabled packages are touched and counted
- Reset looped over the freeze log and counted duplicates; the log is now cleared after a reset
- Uppercase `J` at the confirmation prompt aborted silently
- Menu 7 froze a package for any input other than `f`; now only `e` or `f`, with a name check and a
  block list for system-critical packages
- The `com.android.shell` workaround ran on every start, even for the read-only overview; now only after a
  failed freeze
- Empty input at the menu closed the window instead of redrawing it
- Missing detection for "unauthorized" devices and for multiple attached devices
- Package validation relied on `findstr /x` (unsupported by some environments) and on a pipe that broke
  delayed expansion

Moved from *safe* to *optional* after review: `work.clouddpc`, `uimGbaApp`, `healthconnect.controller`,
`locationproxy`.
