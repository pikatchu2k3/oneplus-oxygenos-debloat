# Changelog

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
