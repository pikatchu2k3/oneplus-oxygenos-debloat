# Firewall-Blockliste für OPPO/OnePlus-Dienste / Firewall blocklist for OPPO/OnePlus services

> **Vorabversion / pre-release.** Die Liste stammt aus einem funktionierenden Regelwerk (Export der App
> „FireWall Blocks"), ist aber nicht auf jedem Modell identisch — fehlende Pakete ignoriert die App einfach.

## Deutsch

**Getestet mit:** FireWall Blocks (`com.shayan.firewall`), Modus *Shizuku*, OnePlus mit OxygenOS.
Die App kann ihre Regeln als JSON exportieren und importieren; `firewall/oppo-blocklist.json` ist genau so ein Export.

**Bedeutung der Werte:** `true` = gesperrt, `false` = erlaubt. Gesperrt werden immer beide Wege
(`_wifi` und `_data`), weil die App im Shizuku-Modus Wi-Fi und mobile Daten aneinanderkoppelt.

**Import:** Einstellungen der App → Import → `oppo-blocklist.json` auswählen. Der Import **ersetzt** alle
bisherigen Regeln der App. Danach Schalter „Enable" setzen und (Modus Shizuku) Shizuku starten.
Nach jedem Neustart: Shizuku starten, App einmal öffnen, damit die Regeln neu gesetzt werden.

**Bewusst nicht enthalten:**

- Drittanbieter-Launcher (z. B. Nova) – blockiert man nur, wenn man keine Online-Widgets braucht
- Messenger, Browser, Banking – nie blockieren, die brauchen Netz
- Alles, was in dieser Liste fehlt, bleibt erlaubt (Standard der App ist „erlaubt")

**Was dabei kaputtgehen kann** – steht in der Tabelle. Kritisch sind vor allem drei Punkte:

1. `com.oplus.ota`, `com.oplus.cota`, `com.oplus.romupdate` – ohne diese Dienste findet das System keine
   Updates mehr. Deshalb: **ROM-Updates über OS Updater** (siehe Haupt-README).
2. `com.coloros.weather.service` – Wetter in App und Widget bleibt leer.
3. `com.google.android.as` – Smart Reply, Live Captions und Now Playing verlieren ihre Cloud-Anteile.

| Paket | Was es ist | Was ohne Netz nicht mehr geht |
|---|---|---|
| `com.android.dynsystem` | Android „Dynamic System Updates" (DSU) – testet GSI-Systemabbilder | Nichts im Alltag; nur für Bastler relevant. |
| `com.android.launcher` | System-Launcher | Widgets und Suchleiste, die online nachladen (Wetter, Vorschläge). |
| `com.android.systemui` | System-UI | Online-Anteile von Sperrbildschirm und Statusleiste (Wetter, Medieninfo). |
| `com.coloros.activation` | Geräteaktivierung (ColorOS) | Nichts; die Aktivierung läuft einmalig beim Einrichten. |
| `com.coloros.assistantscreen` | Assistant-/Left-Screen (ColorOS) | News und Empfehlungen auf dieser Seite. |
| `com.coloros.lockassistant` | Sperrbildschirm-Assistent | Empfehlungen und News-Einblendungen auf dem Sperrbildschirm. |
| `com.coloros.weather.service` | Wetterdienst (ColorOS) | Wetterdaten in App und Widget. |
| `com.google.android.as` | Android System Intelligence | Smart Reply, Live Captions, Now Playing (die Cloud-Anteile). |
| `com.google.android.configupdater` | Konfigurations-Updater | Nichts im Alltag; Sicherheits-Konfiguration kommt etwas später. |
| `com.google.android.googlequicksearchbox` | Google-App / Suche / Assistant | Suche, Discover und Assistant-Antworten. |
| `com.google.android.tts` | Google Sprachausgabe | Online-Stimmen; Offline-Stimmen funktionieren weiter. |
| `com.heytap.htms` | HeyTap Mobile Services (Cloud/Daten) | HeyTap-Cloud, Theme-Store, Teile des Kontos. |
| `com.nearme.instant.platform` | OPPO/Realme Instant Apps | Nichts in Europa. |
| `com.oneplus.gallery` | OnePlus Galerie | Cloud-Sync und AI-Funktionen der Galerie; lokale Fotos bleiben. |
| `com.oneplus.oshare` | O+ Connect (Dateiaustausch) | Austausch mit PC und anderen Geräten. |
| `com.oplus.aiunit` | AI Unit (Cloud-KI) | Cloudgestützte AI-Funktionen, z. B. Zusammenfassungen. |
| `com.oplus.camera` | Kamera | Cloudgestützte Bildverbesserung/AI-Retusche; lokale Aufnahmen bleiben. |
| `com.oplus.cota` | COTA-Update-Dienst | Automatische Update-Prüfung – dafür OS Updater benutzen. |
| `com.oplus.framework.network.drs` | Device Recognition Service | Nichts; dient nur der Geräteerkennung. |
| `com.oplus.gleanerservice` | Telemetrie-Sammler | Nichts. |
| `com.oplus.olc` | OLC-Clouddienst (OPPO/OnePlus-Cloud) | Cloud-Backup und Sync. |
| `com.oplus.ota` | OTA-Client | Systemupdate über Einstellungen – dafür OS Updater benutzen. |
| `com.oplus.pantanal.ums` | User Metrics System | Nutzungsstatistiken. |
| `com.oplus.phonemanager` | Phone Manager | Online-Teile wie Datenverbrauch-Cloud und App-Markt. |
| `com.oplus.romupdate` | ROM-Update-Dienst | ROM-Updates und Update-Prüfung – dafür OS Updater benutzen. |
| `com.oplus.safecenter` | Security Center | Cloud-Virenprüfung; lokale Scans bleiben. |
| `com.oplus.stdid` | Werbe-/Standard-ID | Personalisierte Werbung. |

## English

**Tested with:** FireWall Blocks (`com.shayan.firewall`), *Shizuku* mode, OnePlus running OxygenOS.
The app imports and exports its rules as JSON; `firewall/oppo-blocklist.json` is exactly such an export.

**Value semantics:** `true` = blocked, `false` = allowed. Both transports are blocked together
(`_wifi` and `_data`), because Shizuku mode couples Wi-Fi and mobile data.

**Import:** app settings → import → pick `oppo-blocklist.json`. The import **replaces** every existing rule.
Then flip the “Enable” switch and start Shizuku (Shizuku mode). After every reboot: start Shizuku, open the
app once so the rules are reapplied.

**Deliberately not included:** third-party launchers (Nova etc. — only block them if you do not need online
widgets), messengers, browsers, banking apps. Anything missing from the list stays allowed, which is the
app's default.

| Package | What it is | What stops working without network |
|---|---|---|
| `com.android.dynsystem` | Android Dynamic System Updates (DSU) – testing GSI system images | Nothing in daily use; only relevant for tinkerers. |
| `com.android.launcher` | Stock launcher | Widgets and the search bar that fetch data online (weather, suggestions). |
| `com.android.systemui` | System UI | Online parts of lock screen and status bar (weather, media info). |
| `com.coloros.activation` | Device activation (ColorOS) | Nothing; activation happens once during setup. |
| `com.coloros.assistantscreen` | Assistant / left screen (ColorOS) | News and recommendations on that screen. |
| `com.coloros.lockassistant` | Lock screen assistant | Recommendation and news cards on the lock screen. |
| `com.coloros.weather.service` | Weather service (ColorOS) | Weather data in app and widget. |
| `com.google.android.as` | Android System Intelligence | Smart Reply, Live Captions, Now Playing (cloud parts). |
| `com.google.android.configupdater` | Configuration updater | Nothing in daily use; security configuration arrives a bit later. |
| `com.google.android.googlequicksearchbox` | Google app / search / Assistant | Search, Discover and Assistant answers. |
| `com.google.android.tts` | Google text-to-speech | Online voices; offline voices keep working. |
| `com.heytap.htms` | HeyTap Mobile Services (cloud/data) | HeyTap cloud, theme store, parts of the account. |
| `com.nearme.instant.platform` | OPPO/Realme instant apps | Nothing in Europe. |
| `com.oneplus.gallery` | OnePlus Gallery | Cloud sync and AI features of the gallery; local photos stay. |
| `com.oneplus.oshare` | O+ Connect (file exchange) | Exchange with PC and other devices. |
| `com.oplus.aiunit` | AI Unit (cloud AI) | Cloud-backed AI features, e.g. summaries. |
| `com.oplus.camera` | Camera | Cloud-backed image enhancement/AI retouching; local shots stay. |
| `com.oplus.cota` | COTA update service | Automatic update checks – use OS Updater instead. |
| `com.oplus.framework.network.drs` | Device Recognition Service | Nothing; only used for device recognition. |
| `com.oplus.gleanerservice` | Telemetry collector | Nothing. |
| `com.oplus.olc` | OLC cloud service (OPPO/OnePlus cloud) | Cloud backup and sync. |
| `com.oplus.ota` | OTA client | System update via settings – use OS Updater instead. |
| `com.oplus.pantanal.ums` | User Metrics System | Usage statistics. |
| `com.oplus.phonemanager` | Phone Manager | Online parts such as data usage cloud and app market. |
| `com.oplus.romupdate` | ROM update service | ROM updates and update checks – use OS Updater instead. |
| `com.oplus.safecenter` | Security Center | Cloud virus scan; local scans stay. |
| `com.oplus.stdid` | Advertising / standard ID | Personalised advertising. |

## Warum diese Liste überhaupt nötig ist / Why this list exists

Die Batch-Datei in diesem Repo friert Apps ein. Das hilft nur bei Apps, die man komplett loswerden will.
Diese Liste ist die zweite Ebene: Dienste, die **aktiv bleiben müssen** (Launcher, System-UI, Kamera,
Sicherheitscenter), aber nichts ins Internet zu senden haben.
The batch file in this repo freezes apps you want gone. This list is the second layer: services that must
stay **active** (launcher, system UI, camera, security center) but have no business talking to the internet.
