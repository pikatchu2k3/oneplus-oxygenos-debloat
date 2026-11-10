Ich habe keine Dateien geändert. Für einen Gegentest unter wine hätte ich zwei Testdateien in /tmp schreiben müssen; die Freigabe dafür kam nicht, deshalb ist der Test nicht gelaufen. Befund 1 stützt sich auf das dokumentierte Verhalten von cmd.exe und passt genau zu euren Zahlen. Die Listen habe ich nachgezählt: 75 / 19 / 9 stimmt.

## P0: Ursache des gemeldeten Fehlers

**1. Eine `.bat` wird ohne `call` gestartet** (`oneplus_cleanup.bat:172-173, 196, 207, 215, 223-224`, außerdem Z.18, die `adb.bat` ausdrücklich zulässt)

- **Ursache:** Startet eine Batch eine andere `.bat` ohne `call`, ersetzt diese den laufenden Kontext und kehrt nie zurück. In `:freeze` endet `adb.bat` mit `exit /b 0`. Damit ist auch der `call :freeze`-Kontext beendet. `if errorlevel`, `echo` und `set /a OK+=1` laufen nie, die Schleife macht beim nächsten Paket weiter.
- **Die Zahlen bestätigen das:** Der Stub kennt nur 4 Pakete der SAFE-Liste (heytap.browser, statistics.rom, logkit, aicall). Es gilt 75 − 4 = **71 übersprungen**. Die 4 gefundenen verschwinden ohne Zähler, also stimmen 0/71/0.
- **Dasselbe in `:dump`:** Z.173 läuft nie, `opdb_pd.txt` wird nie erzeugt. Der Kommentar zu „zwei Umleitungen verschlucken die Ausgabe“ ist eine Fehldiagnose genau dieses Effekts. „gesperrt“ in der Übersicht kann unter dem Stub nur aus einer alten Datei kommen, denn die Übersicht löscht ihre Dateien nie.
- **Mit echtem `adb.exe`** tritt der Fehler nicht auf. Z.18 bleibt trotzdem eine Falle.

```bat
REM vorher
"adb" %DEVARG% shell pm disable-user --user 0 %~1 >nul 2>nul
REM nachher (einmal oben definieren, überall verwenden)
set "ADB=call adb %DEVARG%"
%ADB% shell pm disable-user --user 0 %~1 >nul 2>nul
```

## P1: Korrektheit

**2. `DEVARG` fehlt bei dump, prep und getprop** (Z.30-31, 172-173, 220-224)

- Mit zwei Geräten (oder Telefon plus Emulator) schlagen diese Aufrufe mit „more than one device“ fehl. Die Dump-Dateien sind dann leer, alles gilt als „nicht vorhanden“.
- Ein zweites Gerät wird außerdem stillschweigend ignoriert.
- Fix: überall `%ADB%` (siehe 1). Bei mehr als einem Gerät abbrechen:

```bat
set N=0 & for %%d in (!DEVS!) do set /a N+=1
if !N! GTR 1 (echo Bitte nur EIN Telefon anschliessen. & pause & exit /b 1)
```

**3. Leerer Dump wird nicht erkannt** (`:dump`)

- Bei Kabelwackler, Sperrbildschirm-Autorisierung oder schreibgeschütztem Ordner (z. B. .bat direkt aus der ZIP gestartet) ist `opdb_pk.txt` leer oder fehlt. `findstr` liefert dann errorlevel 2, und das Ergebnis lautet „0 eingefroren, 75 nicht vorhanden“, ohne Warnung.

```bat
:dump
set "PK=%TEMP%\opdb_pk.txt"
%ADB% shell pm list packages > "%PK%"
findstr /x /c:"package:android" "%PK%" >nul || (echo [!] Telefon antwortet nicht. & pause & exit /b 1)
```

Den Aufrufer dann mit `call :dump || goto :menu` absichern.

**4. `findstr /c:` sucht Teilstrings** (Z.177, 182, 191)

- `package:com.google.android.gm` trifft auch `…gms`, `com.oplus.location` trifft `…locationproxy`.
- Fix: `findstr /i /x /c:"package:%~1"`.

**5. `J` (groß) bricht ohne Meldung ab** (Z.87, 103, 119, 131, 144)

- `:confirm` vergleicht mit `/i`, der Aufrufer nicht. Wer „J“ tippt, landet ohne Meldung wieder im Menü.
- Fix: `if /i not "%GO%"=="j" goto :menu`.

**6. Erfolg wird nur am Exitcode erkannt** (Z.196-203)

- Der Exitcode von `adb shell` ist je nach Version und Fehlerart unzuverlässig. Besser auf die Ausgabe von pm prüfen:

```bat
%ADB% shell pm disable-user --user 0 %~1 | findstr /c:"new state: disabled-user" >nul
```

**7. Reset ist zu breit und zählt falsch** (`:thaw`, Z.206-212)

- `pm enable` schaltet auch Pakete ein, die **ab Werk** deaktiviert sind (z. B. engineermode, traceur).
- Es zählt jedes schon aktive Paket als „wieder aktiviert“.
- Fix: nur behandeln, was in `pm list packages -d` steht. Dazu `pm default-state --user 0 %~1` oder wenigstens `pm enable --user 0 %~1`.

**8. Sonderzeichen in Pfad und Eingaben**

- Ein `!` im Pfad geht wegen `EnableDelayedExpansion` verloren (Z.16).
- `%CHOICE%`, `%PKG%` und `%MODE%` mit `&`, `"` oder `;` führen Befehle auf dem PC bzw. in der Telefon-Shell aus (adb verkettet die Argumente zu einem Shell-String). Das schadet nur dem Nutzer selbst, aber es kann passieren.
- Fix: `!CHOICE!` usw. und den Paketnamen prüfen:

```bat
echo !PKG!| findstr /r /x "[a-zA-Z0-9_.]*" >nul || goto :single
```

**9. `chcp 65001` ist überflüssig** (Z.2)

- Das Skript ist reines ASCII. Unter Windows 7/8 bringt Codepage 65001 den Batch-Parser durcheinander. Zeile streichen.

## P1: Sicherheit für das Telefon

**10. `com.android.shell`-Trick läuft ungefragt beim Start** (Z.36, `:prep`)

- Er läuft vor dem Menü, also auch, wenn nur „Übersicht (ändert nichts)“ gewünscht ist.
- Bricht die Verbindung zwischen Z.223 und 224 ab, bleibt Shell deaktiviert (Bugreport, Teile der Entwickleroptionen). Weiteres adb funktioniert meist noch, ein Wiederherstellen ist mit `pm enable` möglich.
- Der Nutzen ist unbelegt. Bei ColorOS/OxygenOS ist der eigentliche Schalter meist **Entwickleroptionen → „Berechtigungsüberwachung deaktivieren“**. Das ist unbestätigt, bitte am Gerät prüfen.
- Fix: `call :prep` entfernen. Den Trick höchstens als Notfallhinweis anbieten, wenn `FAIL > 0`.

**11. Menüpunkt 7 hat keinen Schutz und ist im Zweifel destruktiv** (Z.160-164)

- Jede Eingabe außer exakt `f` friert ein. „F“ oder „freigeben“ **friert also ein**.
- Es gibt keine Sperre für NEVER_LIST, Launcher, SystemUI, Settings oder `com.android.phone`. Wird der Launcher eingefroren, bleibt der Bildschirm schwarz, und das Freigeben geht nur noch per adb.

```bat
if /i "!MODE!"=="f" ( %ADB% shell pm enable --user 0 !PKG! & goto :single_end )
if /i not "!MODE!"=="e" ( echo Abgebrochen. & goto :single_end )
for %%n in (%NEVER_LIST% com.android.systemui com.android.settings com.android.launcher com.android.phone com.android.shell) do if /i "!PKG!"=="%%n" (echo GESPERRT. & goto :single_end)
```

**12. Grenzfälle in der SAFE-Liste**

- **`com.google.android.apps.work.clouddpc`:** Lehrkräfte haben oft Schul-MDM oder Intune; ohne dieses Paket fällt das Arbeitsprofil aus. Gehört in OPT.
- **`com.qualcomm.qti.uimGbaApp`:** GBA ist die Authentifizierung für IMS/WLAN-Calling, bei manchen Anbietern bricht VoLTE/VoWiFi weg. Gehört in OPT.
- **`com.google.android.healthconnect.controller`:** Mainline-Modul. Ihr schützt das healthfitness-Overlay (NEVER), deaktiviert aber den Controller. Das ist widersprüchlich.
- **`com.oplus.location`/`locationproxy` (SAFE) und `com.qualcomm.location` (OPT):** Das ist schlechtere Ortung, nicht „sicher“. Ein Warnhinweis fehlt.
- **OPT `com.oplus.encryption` (Privater Safe):** Die Inhalte sind danach nicht mehr erreichbar. Vor dem Bestätigen ausdrücklich warnen.

**13. Abbruch mittendrin** (Strg+C, Kabel)

- Unkritisch, weil idempotent und rücksetzbar. Es fehlt nur ein Protokoll, was eingefroren wurde.
- Fix: `echo %~1>>"%~dp0eingefroren.log"` in `:freeze`. Reset arbeitet dann genau diese Liste ab.

## P2: Idiotensicherheit

- **Leeres Enter im Menü schließt das Fenster** (Z.62): `exit /b 0` durch `goto :menu` ersetzen.
- **Kein Hinweis auf Backup** und dass der Bildschirm entsperrt bleiben soll.
- **USB-Debugging bleibt an:** Am Ende fehlt „USB-Debugging wieder ausschalten“. Das ist ein Sicherheitsrisiko, wenn das Telefon an fremden PCs geladen wird.
- **Nur „Kein Telefon“ bei „unauthorized“:** Gerät im Zustand `unauthorized` gesondert melden („Popup am Telefon bestätigen“).
- **Start direkt aus der ZIP:** Hinweis „Erst entpacken!“ in die Anleitung, weil `platform-tools` sonst nicht gefunden wird.

## Sachfehler in ANLEITUNG-BAT.txt

| Zeile | Anleitung | Tatsächlich |
|---|---|---|
| 16 | Übersicht „Ändert nichts“ | Beim Start lief schon `:prep` (disable + enable shell) |
| 31-32 | Nur `disable-user com.android.shell` | Danach sofort `pm enable`; läuft automatisch und ungefragt |
| 18 | „plus die 5 Ergänzungen“ | Die Ergänzungen sind nirgends benannt, für Laien unverständlich |
| 21 | Reset aktiviert „alle Pakete beider Listen“ | Aktiviert auch ab Werk deaktivierte Pakete |
| bat Z.117 | „Deinstallierte Pakete nur per Werksreset“ | Falsch: `pm install-existing <paket>` holt sie zurück. Wichtig, weil vorher Canta im Einsatz war; solche Pakete zeigt das Skript als „nicht auf dem Gerät“ |
| 35-37 | NEVER-Pakete „nicht drin“ | Stimmt für die Listen, aber Menüpunkt 7 friert sie ohne Rückfrage ein |
| 12-13 | Satz zu „Als Administrator“ | Verwirrend formuliert; einfach „Nicht als Administrator nötig“ |
| 24 | `e`/`f` | Nicht erwähnt: alles außer `f` friert ein |
| fehlt | | Erst entpacken, Backup, USB-Debugging danach ausschalten, Warnung zum Privaten Safe |

**Reihenfolge der Fixes:** zuerst 1 (`call`, und `DEVARG` gleich mit), dann 3, 10 und 11. Der Rest ist Feinschliff.
