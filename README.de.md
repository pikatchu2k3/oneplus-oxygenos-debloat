# OnePlus OxygenOS Debloat (ADB)

> ## ⚠️ VORABVERSION — v0.9.0-beta
>
> Das hier ist eine **Vorabversion**. Es kann haken, und die Paketlisten passen nicht zwangsläufig zu
> jedem Modell und jeder Region.
>
> **Auf einem echten OnePlus wurde das Skript noch nicht ausgeführt.** Getestet wurde nur in einer
> nachgebauten Windows-Umgebung (Wine) gegen einen selbst gebauten `adb`-Stub. Alles, was das Skript tut,
> ist umkehrbar — die Nutzung erfolgt trotzdem auf eigene Gefahr.
>
> Rückmeldungen, Paketnamen anderer OnePlus-Modelle und Korrekturen sind ausdrücklich willkommen (Issue aufmachen).

Eine einzelne Windows-Batchdatei, die Bloatware, Telemetrie und ungenutzte System-Apps auf OnePlus-Geräten
**einfriert**, statt sie zu deinstallieren. Kein Root, kein Shizuku, kein Canta, kein App Manager — nur
USB-Kabel und `adb` — plus eine optionale Firewall-Blockliste gegen Tracker und ein klarer Update-Weg
über **OS Updater**, weil die Firewall die OTA-Dienste abschaltet.

English version: [README.md](README.md)

---

## Was es macht

- Friert Apps mit `pm disable-user --user 0` ein — App verschwindet, verbraucht keinen Akku, **nichts wird gelöscht**
- Drei kuratierte Listen: **71 sicher**, **23 optional**, **9 nie anfassen**
- Prüft jede Aktion über `pm list packages -d`, statt der adb-Ausgabe zu vertrauen
- Überspringt Pakete, die es auf dem Gerät nicht gibt (Listen sind modell- und regionsabhängig)
- Optionaler AppOps-Schritt: sperrt Hintergrundausführung und Wakelocks von fünf Telemetrie-Diensten
- Jederzeit vollständig rückgängig (Menüpunkt 4, oder über das Log `eingefroren.log`)
- Protokolliert jedes Einfrieren, damit auch einzeln eingefrorene Pakete zurückkommen

## Was es nicht macht

- Kein Deinstallieren, kein Datenverlust, kein Löschen von App-Daten
- Kein Root, kein Custom-ROM, kein Flashen
- Kein DNS-Blocking — dafür gibt es die optionale Firewall-Blockliste (siehe unten) oder ein NextDNS-Profil
  (Kapitel 6 in `docs/oneplus_cleanup.pdf`)
- Keine UI-Einstellungen (Autostart, Benachrichtigungen, Akku-Optimierung) — das sind Schalter in OxygenOS

## Voraussetzungen

- Windows 10/11
- USB-Kabel
- [Android Platform-Tools](https://developer.android.com/tools/releases/platform-tools) — **entpacken**, der Ordner
  `platform-tools` liegt neben der `.bat`
- USB-Debugging am Telefon an (Einstellungen → Über das Telefon → 7× auf *Build-Nummer* → Entwickleroptionen)
- Bildschirm während des Laufs entsperrt

## Schnellstart

1. Repo herunterladen (grüner *Code*-Button → *Download ZIP*) und **entpacken**. Nicht direkt aus der ZIP starten.
2. Platform-Tools herunterladen, entpacken und den Ordner `platform-tools` neben `oneplus_cleanup.bat` legen.
3. Telefon anstecken, Popup *USB-Debugging zulassen* mit **Immer zulassen** bestätigen.
4. Doppelklick auf `oneplus_cleanup.bat`.
5. Zuerst **1** (Übersicht) laufen lassen — ändert nichts und zeigt, was das Gerät wirklich hat.
6. Dann **2** (Sicher). Einmal neu starten.
7. Danach **USB-Debugging wieder ausschalten**.

## Menü

- **1 — Übersicht**: pro Paket, ob vorhanden und ob aktiv oder gesperrt. Ändert nichts.
- **2 — Sicher**: friert die 71 sicheren Pakete ein (Telemetrie, Werbung, Logs, Konto-Gerüst, Testtools).
- **3 — Optional**: sichere Liste **plus** 23 feature-abhängige Pakete — Chrome, Fotos, Android Auto,
  „Hey Google“, eID, **Privater Safe** (Inhalt danach nicht mehr erreichbar), Schrittzähler,
  Arbeitsprofil/Intune, WLAN-Telefonie-Baustein.
- **Bei 2 und 3 selbst auswählen**: vor dem Start fragt das Skript `a` = alle oder `w` = selbst auswählen.
  Bei `w` wird jedes Paket, das auf dem Gerät ist und noch läuft, einzeln abgefragt: `j` einfrieren, `n` auslassen,
  `a` = Rest der Gruppe komplett, `k` = Rest der Gruppe auslassen. Bei 3 lassen sich die sichere und die
  optionale Liste getrennt wählen (alle / selbst auswählen / `k` = keins) — z. B. nur einzelne optionale Pakete.
- **4 — Reset**: aktiviert alles wieder, was dieses Skript eingefroren hat. Jederzeit möglich.
- **5 — App-Ops**: sperrt `RUN_ANY_IN_BACKGROUND`, `RUN_IN_BACKGROUND` und `WAKE_LOCK` für
  `statistics.rom`, `logkit`, `onetrace`, `postmanservice`, `ocloud`. Überlebt Neustarts.
- **6 — App-Ops Reset**: setzt diese drei Ops auf `default` zurück.
- **8 — Status**: Kontrolle nach einem System-Update, ändert nichts. Zeigt je App-Op, ob es noch gesperrt (`[ok]`)
  oder wieder erlaubt (`[OFFEN]`) ist, und welche Pakete aus `eingefroren.log` wieder aktiv sind. Bei Abweichungen
  einfach 5 bzw. 2/3/7 erneut ausführen.
- **7 — Einzeln**: **beliebige** Apps und Dienste einfrieren (`e`) oder freigeben (`f`), auch Pakete aus der
  sicheren/optionalen Liste — damit lassen sich 2 und 3 komplett umgehen. Mit `?wort` sucht man installierte
  Pakete (z. B. `?heytap`, nur `?` listet alle, `[aus]` = eingefroren). Das Menü bleibt offen, bis man Enter
  ohne Eingabe drückt. Nur die System-Basis (Launcher, SystemUI, Telefon, Einstellungen) und die
  „Nie anfassen“-Liste lassen sich hier nicht einfrieren, wohl aber freigeben.

## Wie „einfrieren“ funktioniert

```bat
adb shell pm disable-user --user 0 <paket>
```

Das Paket bleibt installiert, ist für den aktuellen Benutzer aber deaktiviert. Rückgängig:

```bat
adb shell pm enable --user 0 <paket>      :: macht Menüpunkt 4
adb shell pm install-existing <paket>     :: holt zurück, was mit Canta/ADB entfernt wurde
```

Ein Werksreset stellt ebenfalls alles wieder her.

## Erweiterte Option: Netzwerk-Blockade (Tracker unterbinden)

Einfrieren hilft nur bei Apps, die ganz weg sollen. Dienste, die aktiv bleiben **müssen** — Launcher,
System-UI, Kamera, Sicherheitscenter — telefonieren trotzdem nach Hause. Dafür gibt es die zweite Ebene:
eine Firewall mit Regeln pro App.

- App: **FireWall Blocks** (`com.shayan.firewall`) von IzzyOnDroid — Open Source, kein Root, zwei Modi
  (Shizuku oder lokales VPN): https://apt.izzysoft.de/fdroid/index/apk/com.shayan.firewall
- Fertige Liste: `firewall/oppo-blocklist.json` — 27 OPPO-/ColorOS-/OnePlus-Dienste, Wi-Fi **und** mobile Daten gesperrt
- Lesbare Fassung mit „was geht dabei kaputt": `docs/firewall-blocklist.md`

Vorgehen:

1. FireWall Blocks installieren, Modus **Shizuku** wählen (sparsamer als VPN, kein Dauer-VPN-Symbol).
2. In der App importieren: `firewall/oppo-blocklist.json`. Achtung: der Import **ersetzt** alle Regeln der App.
3. „Enable" einschalten und Shizuku starten.
4. **Nach jedem Neustart**: Shizuku starten und die App einmal öffnen — sonst sind die Regeln nicht aktiv.
5. Gegenprobe: Browser, Messenger und Play Store müssen weiter funktionieren, das Wetter-Widget bleibt leer.

Die Liste ist der Stand eines funktionierenden Geräts. Fehlt ein Paket auf deinem Modell, ignoriert die App
es stillschweigend. Und: was nicht in der Liste steht, bleibt erlaubt — das ist die Voreinstellung der App.

## ROM-Updates: nur noch über OS Updater

Die Firewall sperrt `com.oplus.ota`, `com.oplus.cota` und `com.oplus.romupdate` — genau die Dienste, über
die OxygenOS seine Updates zieht. Das ist gewollt (keine ungefragten Downloads, keine Telemetrie), hat aber
eine Folge: **die Update-Funktion in den Einstellungen findet nichts mehr.**

Updates laufen deshalb über **OS Updater** (früher „Oxygen Updater"):
https://play.google.com/store/apps/details?id=com.arjanvlek.oxygenupdater

1. App installieren. Das Projekt ist Open Source, die Werbung lässt sich per In-App-Kauf abschalten.
2. Gerät/Region prüfen, Kanal *Stable* wählen.
3. Update herunterladen — die App zieht die offizielle Datei direkt vom Hersteller und prüft die MD5-Summe.
4. Der Installationsanleitung in der App folgen („Local upgrade"). Das System installiert selbst,
   die App kann kein ROM flashen.
5. **Wenn die Installation abbricht** („Installation fehlgeschlagen"): in FireWall Blocks `com.oplus.ota`
   und `com.oplus.romupdate` kurz erlauben, Installation wiederholen, danach wieder sperren.

Sicherheitspatches kommen im selben Paket und damit auf demselben Weg.

## Paketlisten

- **Sicher (71)**: weg, ohne dass eine Funktion fehlt
- **Optional (23)**: Feature- oder Geschmacksfrage — Beschreibung vorher lesen
- **Nie anfassen (9)**: Telefon, Kontakte, Nachrichten, Gboard, Gmail, Kalender, Akku-Anzeige, Update-Dienst,
  Health-Connect-Overlay

Die Gruppe „nie anfassen“ existiert, weil öffentliche Debloat-Listen mehrere davon als „empfohlen“ markieren —
auf OnePlus kostet das Gesten, Gesichtserkennung, Medienspeicher oder Updates.

Nach Prüfung bewusst nach *Optional* verschoben: `work.clouddpc` (Schul-/MDM-Einrichtung),
`uimGbaApp` (WLAN-Telefonie des Providers), `healthconnect.controller`, `locationproxy`.

## Sicherheit

- **Vorher Backup machen.** Einfrieren löscht keine App-Daten, aber ein Backup kostet fünf Minuten.
- Privater Safe (Menü 3) versteckt deine Dateien, solange er eingefroren ist — vorher entsperren und herauskopieren.
- `com.oplus.sauhelper` und `com.oplus.powermonitor` nicht einfrieren: keine Systemupdates, keine Akkuwerte.
- USB-Debugging ist eine echte Sicherheitslücke. Danach wieder ausschalten.
- Immer nur ein Gerät anschließen. Bei zwei Geräten verweigert das Skript den Start.
- Scheitert ein Einfrieren, deaktiviert das Skript kurz `com.android.shell` und aktiviert es sofort wieder —
  der bekannte OnePlus-Workaround gegen „not allowed to disable“. Das passiert nur, wenn es wirklich nötig ist.

## Bekannte Grenzen

- Bisher nur unter Wine mit Stub-`adb` getestet, noch nicht auf echter Hardware
- Paketnamen unterscheiden sich zwischen Modellen, Regionen und OxygenOS-Versionen — fehlende werden
  übersprungen und als „nicht auf dem Gerät“ geführt
- Der `com.android.shell`-Workaround ist auf den neuesten OxygenOS-Builds nicht belegt
- Chinesische ROMs (ColorOS-Basis) enthalten deutlich mehr Pakete als diese Listen
- AppOps-Namen können sich zwischen Android-Versionen ändern; Menü 5 zeigt nur, was wirklich geklappt hat
- Die Firewall-Liste ist nicht auf einem Gerät getestet: Format und Bedeutung der Werte wurden gegen den
  Quellcode von FireWall Blocks geprüft, der Import selbst nicht
- Der Update-Weg über OS Updater inklusive „Local upgrade" ist ebenfalls nicht getestet

## Fehlersuche

- **„adb nicht gefunden“** — Platform-Tools nicht entpackt, oder der Ordner liegt nicht neben der `.bat`
- **„Kein Telefon im Debug-Modus“** — Kabel, USB-Debugging, oder das Telefon lädt nur (Benachrichtigung
  herunterziehen und *Dateiübertragung* wählen)
- **„Telefon angeschlossen, aber nicht freigegeben“** — am Telefon *Zulassen* tippen, dann Skript neu starten
- **„Telefon antwortet nicht / Ordner schreibgeschützt“** — aus der ZIP gestartet, oder Bildschirm gesperrt
- **Ein Paket lässt sich nicht einfrieren** — vielleicht gibt es das Paket auf deinem Modell nicht; Menü 1 prüfen

## Inhalt des Repos

- `oneplus_cleanup.bat` — das Werkzeug (Windows, CRLF)
- `ANLEITUNG-BAT.txt` — deutsche Kurzanleitung
- `README.md` — englische Fassung
- `docs/oneplus_cleanup.pdf` — die lange Anleitung: 12 Bereiche aus dem android-hilfe.de-Thread, auf OxygenOS übertragen
- `docs/canta_kontrolle.pdf` — Prüfung eines Canta-Presets (welche Einträge Kernfunktionen zerstören)
- `docs/expert-review.md` — unabhängiges Code-Review (deutsch), inklusive der für v0.9.0 behobenen Fehler
- `docs/firewall-blocklist.md` — die Firewall-Liste erklärt (deutsch/englisch), inklusive Nebenwirkungen
- `firewall/oppo-blocklist.json` — Import-Datei für FireWall Blocks
- `firewall/oppo-blocklist.txt` — dieselben Pakete als reine Liste zum Abtippen
- `tools/template_bat.txt` — Vorlage mit `@@SAFE@@` / `@@OPT@@` / `@@NEVER@@` zum Neubauen der `.bat`

## Haftung

Keine Gewähr. Du veränderst dein eigenes Gerät auf eigenes Risiko. Das Projekt gehört weder zu OnePlus,
Oppo oder Google noch zu Canta / App Manager / Universal Android Debloater. Halte dein Telefon
wiederherstellbar: Recovery erreichbar, Zugangsdaten zur Hand.

## Danke an

Den android-hilfe.de-Thread „China-Version Clean Up & Optimize für DAUs“, an
[Universal Android Debloater](https://github.com/Universal-Debloater-Alliance/universal-android-debloater-next-generation)
(nur Listen-Recherche) und an [Canta](https://github.com/samolego/Canta) / App Manager als die
Shizuku-basierten Alternativen, die dieses Skript ersetzt. Firewall-Regeln auf Basis von
[FireWall Blocks](https://github.com/shynoiddev/FireWall-Blocks), Update-Weg über
[OS Updater](https://oxygenupdater.com/).
