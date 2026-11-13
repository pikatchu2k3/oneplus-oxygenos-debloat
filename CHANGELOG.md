# Changelog

## Unreleased

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
