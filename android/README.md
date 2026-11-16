# OnePlus Clean-Up – Android-App (Vorabversion)

Die App macht auf dem Telefon dasselbe wie `oneplus_cleanup.bat` am PC: vorinstallierte Apps
einfrieren, bei Bedarf für den Nutzer entfernen, Telemetrie-Dienste per App-Ops sperren, alles
wieder zurücksetzen. Dazu kommen eine eigene Liste, Erklärungen zu jeder App und ein Status-Check
nach System-Updates.

**Stand: Etappe 1, auf keinem echten Gerät getestet.** Gebaut und mit Unit-Tests sowie
Screenshot-Tests auf der JVM geprüft.

## Zugriffswege

| Weg | Stand | Umfang |
|---|---|---|
| Shizuku | enthalten | voll (gleiche Rechte wie `adb shell`) |
| Ohne alles (Anzeige-Modus) | enthalten | Zustand, Erklärungen, Sprung zur App-Info, ADB-Befehl zum Kopieren |
| Eingebautes Wireless ADB | geplant (Etappe 2) | voll |

Android vergibt die Berechtigung zum Deaktivieren fremder Apps nur an System-Apps; sie lässt sich
nicht per `pm grant` setzen. Deshalb kann der Anzeige-Modus nichts ändern.

## Bauen

```
cd android
./gradlew assembleDebug          # APK: app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # Logik-Tests gegen ein Fake-Telefon
./gradlew recordRoborazziDebug   # Screenshots nach app/build/outputs/roborazzi
```

Benötigt JDK 17+ und das Android-SDK (Plattform 35). GitHub Actions baut die APK bei jedem Push
(`.github/workflows/android.yml`) und legt sie als Artefakt ab.

## Aufbau

- `core/` – `DebloatEngine`: dieselben Schritte wie das Skript, jede Änderung wird zurückgelesen.
  Reines Kotlin, ohne Android-Abhängigkeit testbar.
- `shizuku/` – Verbindung zu Shizuku; ein kleiner User-Service führt die Shell-Befehle aus.
- `data/` – Paketzustände über den PackageManager, Journal und eigene Liste als JSON.
- `ui/` – Compose/Material 3. Texte stehen in `Txt.kt` (Deutsch und Englisch).
- `app/src/main/assets/catalog.json` – Paketlisten mit Erklärungen. **Nicht von Hand ändern:**
  `python3 tools/gen_app_catalog.py` erzeugt die Datei aus den Listen in `oneplus_cleanup.bat`;
  ein Test stellt sicher, dass beide übereinstimmen.

## Signatur

`testkey.jks` ist ein **öffentlicher Testschlüssel** (Passwort `testkey`), absichtlich im Repo,
damit jede Testversion über die vorige installiert werden kann. Für eine echte Veröffentlichung
gehört ein eigener, geheimer Schlüssel her; danach ist einmalig eine Neuinstallation nötig.

## Bewusst anders als das Skript

- Kein `com.android.shell`-Trick: Er ist unbelegt, und die App läuft über Shizuku selbst als Shell.
- „Zurücksetzen“ betrifft nur, was die App selbst eingefroren oder entfernt hat (Journal).
