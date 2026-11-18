package io.github.pikatchu2k3.oneplusdebloat.ui

import io.github.pikatchu2k3.oneplusdebloat.core.Attempt
import io.github.pikatchu2k3.oneplusdebloat.core.OpOutcome
import io.github.pikatchu2k3.oneplusdebloat.core.Outcome
import io.github.pikatchu2k3.oneplusdebloat.core.OwnerSetup
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import io.github.pikatchu2k3.oneplusdebloat.core.Way
import io.github.pikatchu2k3.oneplusdebloat.data.DefaultRole
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuState

/**
 * All user-visible text. Kept in code instead of strings.xml so that the compiler guarantees
 * both languages are complete; German is used when the phone language is German.
 */
interface Txt {
    val german: Boolean

    // navigation and common words
    val tabHome: String
    val tabApps: String
    val tabTelemetry: String
    val tabHelp: String
    val ok: String
    val cancel: String
    val notNow: String
    val add: String
    val refresh: String
    val howTo: String
    val searchHint: String
    val systemApp: String
    fun andMore(n: Int): String
    fun phoneSays(text: String): String
    fun message(m: Message): String

    // states
    val stateActive: String
    val stateFrozen: String
    val stateRemoved: String
    val stateNotPresent: String
    val stateHidden: String
    val stateSuspended: String
    fun stateExplanation(state: PkgState): String

    // lists
    val listSafe: String
    val listOptional: String
    val listCustom: String
    val listCaution: String
    val listOff: String
    val locked: String
    fun listIntro(tab: Int): String
    fun listSummary(present: Int, all: Int): String
    fun showMissing(n: Int): String
    fun selectAllActive(n: Int): String
    val selectNone: String
    fun selectedCount(n: Int): String
    val listEmpty: String
    val customEmpty: String
    val addApps: String
    val importList: String
    val exportList: String
    val pickerTitle: String
    val pickerIntro: String
    val showSystemApps: String
    fun addCount(n: Int): String
    val manualAddTitle: String

    // actions
    val freeze: String
    val thaw: String
    val remove: String
    val restore: String
    val systemSettings: String
    val removeFromList: String
    val freezeVsRemove: String
    val protectedHint: String
    val cautionHint: String
    val cautionTitle: String
    val cautionBody: String
    val cautionConfirm: String
    fun defaultRole(role: DefaultRole): String
    fun defaultHint(role: DefaultRole): String
    fun reportDefaultApp(n: Int): String
    val criticalDescription: String
    val noDescription: String
    val manualTitle: String
    val manualBody: String
    val copyCommand: String
    val needsShellShort: String

    // batch dialogs
    fun progressTitle(kind: BatchKind): String
    val progressHint: String
    val reportTitle: String
    fun reportLines(report: Report): List<String>
    fun reportIgnoredTitle(n: Int): String
    val reportIgnoredBody: String
    fun reportFailedTitle(n: Int): String
    fun reportProtected(n: Int): String
    fun tryOtherWays(n: Int): String
    fun removeTitle(n: Int): String
    val removeBody: String
    val removeConfirm: String
    fun outcome(o: Outcome): String

    // other ways (vendor blocks freezing and removing)
    val otherWays: String
    fun otherWaysTitle(n: Int): String
    val otherWaysBody: String
    fun otherWaysSteps(owner: Boolean): List<String>
    val otherWaysUndo: String
    val otherWaysConfirm: String
    fun wayName(way: Way): String
    fun attemptLine(a: Attempt): String
    val reportChainsTitle: String
    val reportOwnerTip: String
    val reportNeutralizedNote: String

    // device owner
    val ownerTitle: String
    fun ownerStatus(isOwner: Boolean): String
    fun ownerBody(isOwner: Boolean): List<String>
    fun ownerResult(r: OwnerSetup.Result): String
    val ownerSetup: String
    val ownerRelease: String
    fun ownerReleaseBody(hidden: Int): String
    val ownerReleaseFailed: String
    val ownerPcHint: String

    // home
    val appTitle: String
    val appSubtitle: String
    fun connectionTitle(state: ShizukuState): String
    fun connectionBody(state: ShizukuState): String
    val getShizuku: String
    val openShizuku: String
    val allowAccess: String
    val viewOnlyHint: String
    val statFrozen: String
    val statRemoved: String
    val statSafeActive: String
    val statNote: String
    val bulkButton: String
    val bulkTitle: String
    val bulkBody: String
    fun bulkList(name: String, n: Int): String
    val bulkOptionalWarning: String
    val bulkNote: String
    fun bulkConfirm(n: Int): String
    fun driftTitle(n: Int): String
    val driftBody: String
    val driftApply: String
    val allGoodTitle: String
    val allGoodBody: String
    val startTitle: String
    val startBody: String
    val resetTitle: String
    val resetBody: String
    val resetButton: String
    fun resetConfirmBody(frozen: Int, removed: Int): String
    val logTitle: String
    val showAll: String
    val showLess: String
    val clearLog: String
    fun versionLine(version: String): String

    // telemetry
    val telemetryTitle: String
    val telemetryWhatTitle: String
    val telemetryWhatBody: String
    val telemetryNeedsShellTitle: String
    val telemetryNeedsShellBody: String
    val opsBlock: String
    val opsReset: String
    val opsLoading: String
    val opsUnknown: String
    val telemetryLegend: String
    fun opName(op: String): String
    val opBlocked: String
    val opOpen: String
    val opNotRunning: String
    fun opsReportLines(report: OpsReport): List<String>
    val opsRefusedTitle: String
    val opsRefusedBody: String
    fun freezeInstead(n: Int): String

    // help
    val helpTitle: String
    val helpIntro: String
    val helpTopics: List<HelpTopic>
    val shizukuGuide: String
    val openRepo: String
    val reportProblem: String
    val disclaimer: String
}

private fun opLabel(op: String, background: String, backgroundAny: String, wake: String) = when (op) {
    "RUN_IN_BACKGROUND" -> background
    "RUN_ANY_IN_BACKGROUND" -> backgroundAny
    "WAKE_LOCK" -> wake
    else -> op
}

object De : Txt {
    override val german = true

    override val tabHome = "Start"
    override val tabApps = "Apps"
    override val tabTelemetry = "Telemetrie"
    override val tabHelp = "Hilfe"
    override val ok = "OK"
    override val cancel = "Abbrechen"
    override val notNow = "Nicht jetzt"
    override val add = "Hinzufügen"
    override val refresh = "Aktualisieren"
    override val howTo = "So geht's"
    override val searchHint = "Name oder Paket suchen"
    override val systemApp = "System-App"
    override fun andMore(n: Int) = "… und $n weitere"
    override fun phoneSays(text: String) = "Telefon meldet: $text"
    override fun message(m: Message) = when (m) {
        Message.NEEDS_SHELL -> "Dafür muss Shizuku verbunden sein (siehe Start)."
        Message.CONNECTION_LOST -> "Verbindung zu Shizuku verloren. Shizuku neu starten und erneut versuchen."
        Message.IMPORTED -> "Liste importiert."
        Message.IMPORT_FAILED -> "Die Datei enthält keine gültige Liste."
        Message.NOTHING_SELECTED -> "Nichts zu tun."
        Message.COPIED -> "Befehl kopiert."
    }

    override val stateActive = "aktiv"
    override val stateFrozen = "eingefroren"
    override val stateRemoved = "entfernt"
    override val stateNotPresent = "nicht vorhanden"
    override val stateHidden = "versteckt"
    override val stateSuspended = "stillgelegt"
    override fun stateExplanation(state: PkgState) = when (state) {
        PkgState.ACTIVE -> "Zustand: aktiv – die App ist installiert und kann laufen."
        PkgState.FROZEN -> "Zustand: eingefroren – die App ist deaktiviert. Sie startet nicht, verbraucht keinen Akku und zeigt kein Icon. Nichts wurde gelöscht."
        PkgState.REMOVED -> "Zustand: entfernt – für dich deinstalliert, die Kopie im System ist noch da. „Zurückholen“ stellt sie wieder her, ein Werksreset ebenfalls."
        PkgState.NOT_PRESENT -> "Diese App gibt es auf deinem Telefon nicht. Die Listen gelten für viele Modelle; fehlende Einträge sind normal."
        PkgState.HIDDEN -> "Zustand: versteckt – über den Geräteinhaber für dich ausgeblendet. Die App läuft nicht und zeigt kein Icon; die Kopie im System und die Daten bleiben. „Zurückholen“ macht sie wieder sichtbar."
        PkgState.SUSPENDED -> "Zustand: stillgelegt – die App ist pausiert und lässt sich nicht öffnen. Hat diese App sie stillgelegt, darf sie auch nicht im Hintergrund laufen. Schwächer als Einfrieren: Das System kann sie trotzdem kurz starten. Auch „App pausieren“ aus Digital Wellbeing führt zu diesem Zustand."
    }

    override val listSafe = "Sicher"
    override val listOptional = "Optional"
    override val listCustom = "Eigene"
    override val listCaution = "Vorsicht"
    override val listOff = "Aus"
    override val locked = "gesperrt"
    override fun listIntro(tab: Int) = when (tab) {
        0 -> "Telemetrie, Werbung, Protokolle und Testwerkzeuge. Diese Apps kannst du einfrieren, ohne dass im Alltag etwas fehlt. Tippe auf einen Eintrag für Details."
        1 -> "Geschmackssache: Hier hängen Funktionen dran, die du vielleicht nutzt (Chrome, Fotos, Android Auto, Privater Safe …). Lies vor dem Einfrieren die Beschreibung."
        2 -> "Deine eigene Liste. Füge beliebige installierte Apps hinzu und friere sie genauso ein. Die Liste lässt sich als Datei sichern und auf ein anderes Telefon übertragen."
        3 -> "Kern-Apps wie Telefon, SMS, Kontakte und Tastatur. Du kannst sie einfrieren, wenn du Ersatz hast – einzeln und nach einer Rückfrage. Solange eine davon die Standard-App des Telefons ist, verweigert die App das. Mit Schloss markiert ist die System-Basis: Sie bleibt gesperrt, weil das Telefon ohne sie nicht mehr startet oder bedienbar ist."
        else -> "Alles, was auf diesem Telefon gerade deaktiviert oder für dich entfernt ist – egal ob durch diese App, das PC-Skript, Canta, dich selbst oder ab Werk. Tippe auf einen Eintrag, um ihn freizugeben oder zurückzuholen."
    }
    override fun listSummary(present: Int, all: Int) = "$present von $all auf diesem Telefon vorhanden"
    override fun showMissing(n: Int) = "Nicht vorhandene zeigen ($n)"
    override fun selectAllActive(n: Int) = "Alle aktiven wählen ($n)"
    override val selectNone = "Auswahl aufheben"
    override fun selectedCount(n: Int) = "$n ausgewählt"
    override val listEmpty = "Keine Einträge. Auf diesem Telefon ist von dieser Liste nichts (mehr) vorhanden."
    override val customEmpty = "Noch leer. Tippe auf „Apps hinzufügen“, um installierte Apps auszuwählen."
    override val addApps = "Apps hinzufügen"
    override val importList = "Importieren"
    override val exportList = "Exportieren"
    override val pickerTitle = "Apps hinzufügen"
    override val pickerIntro = "Wähle Apps für deine eigene Liste. Hinzufügen ändert noch nichts – eingefroren wird erst, wenn du es in der Liste auslöst."
    override val showSystemApps = "System-Apps anzeigen"
    override fun addCount(n: Int) = if (n == 0) "Hinzufügen" else "$n hinzufügen"
    override val manualAddTitle = "Paketname von Hand eingeben"

    override val freeze = "Einfrieren"
    override val thaw = "Freigeben"
    override val remove = "Entfernen"
    override val restore = "Zurückholen"
    override val systemSettings = "App-Info öffnen"
    override val removeFromList = "Aus meiner Liste nehmen"
    override val freezeVsRemove = "Einfrieren ist der sanfte Weg. Manche Telefone ignorieren das bei einzelnen System-Apps – dann hilft „Entfernen“ (für dich deinstallieren, jederzeit zurückholbar). Lehnt das Telefon auch das ab, probiert „Andere Wege …“ alles, was ohne Root geht."
    override val protectedHint = "Gesperrt: Teil der System-Basis. Die App friert sie nicht ein und entfernt sie nicht, weil das Telefon ohne sie nicht mehr startet oder bedienbar ist."
    override val cautionHint = "Vorsicht: Kern-App. Nur einfrieren, wenn eine andere App diese Aufgabe übernimmt."
    override val cautionTitle = "Kern-App wirklich abschalten?"
    override val cautionBody = "Stelle vorher sicher, dass eine andere App diese Aufgabe übernimmt und in den Einstellungen als Standard gesetzt ist. Rückgängig geht es jederzeit im Tab „Aus“."
    override val cautionConfirm = "Ich habe Ersatz"
    override fun defaultRole(role: DefaultRole) = when (role) {
        DefaultRole.KEYBOARD -> "aktive Tastatur"
        DefaultRole.DIALER -> "Standard-Telefon-App"
        DefaultRole.SMS -> "Standard-SMS-App"
        DefaultRole.LAUNCHER -> "Standard-Startbildschirm"
    }
    override fun defaultHint(role: DefaultRole) = when (role) {
        DefaultRole.KEYBOARD -> "Das ist gerade deine aktive Tastatur. Wähle zuerst eine andere Tastatur aus (Einstellungen → System → Tastatur), sonst kannst du nichts mehr eingeben – auch keine PIN."
        DefaultRole.DIALER -> "Das ist gerade deine Standard-Telefon-App. Lege zuerst eine andere fest (Einstellungen → Apps → Standard-Apps), sonst sind keine Anrufe mehr möglich."
        DefaultRole.SMS -> "Das ist gerade deine Standard-SMS-App. Lege zuerst eine andere fest (Einstellungen → Apps → Standard-Apps), sonst kommen keine SMS mehr an."
        DefaultRole.LAUNCHER -> "Das ist gerade dein Startbildschirm. Lege zuerst einen anderen fest (Einstellungen → Apps → Standard-Apps), sonst hat das Telefon keine Oberfläche mehr."
    }
    override fun reportDefaultApp(n: Int) = "$n übersprungen, weil sie gerade Standard-App des Telefons sind (Tastatur, Telefon, SMS oder Startbildschirm). Erst Ersatz als Standard festlegen."
    override val criticalDescription = "Teil der System-Basis (Oberfläche, Einstellungen, Telefonie oder Installer). Ohne sie ist das Telefon nicht mehr bedienbar."
    override val noDescription = "Zu dieser App gibt es keine Beschreibung – sie steht in keiner der vorbereiteten Listen. Friere nur ein, was du kennst; im Zweifel hilft eine Suche nach dem Paketnamen."
    override val manualTitle = "Ohne Shizuku"
    override val manualBody = "Die App darf ohne Shizuku nichts ändern. Du kannst es selbst tun: über „App-Info öffnen“ → „Deaktivieren“ (falls das Telefon den Knopf anbietet) oder am PC mit diesem ADB-Befehl:"
    override val copyCommand = "Befehl kopieren"
    override val needsShellShort = "Dafür muss Shizuku verbunden sein (Start → Verbindung)."

    override fun progressTitle(kind: BatchKind) = when (kind) {
        BatchKind.FREEZE -> "Friere ein …"
        BatchKind.REMOVE -> "Entferne …"
        BatchKind.RESET -> "Stelle wieder her …"
        BatchKind.REAPPLY -> "Wende erneut an …"
        BatchKind.HARD -> "Probiere andere Wege …"
    }
    override val progressHint = "Bitte die App geöffnet lassen."
    override val reportTitle = "Ergebnis"
    override fun reportLines(report: Report): List<String> {
        val out = mutableListOf<String>()
        fun line(n: Int, text: String) { if (n > 0) out += "$n $text" }
        line(report.count(Outcome.FROZEN), "eingefroren")
        line(report.count(Outcome.ALREADY_FROZEN), "waren schon eingefroren")
        line(report.count(Outcome.REMOVED), "entfernt")
        line(report.count(Outcome.HIDDEN), "versteckt")
        line(report.count(Outcome.NEUTRALIZED), "stillgelegt")
        line(report.count(Outcome.THAWED), "freigegeben")
        line(report.count(Outcome.RESTORED), "zurückgeholt")
        line(report.count(Outcome.ALREADY_ACTIVE), "waren schon aktiv")
        line(report.count(Outcome.NOT_PRESENT), "nicht vorhanden")
        if (out.isEmpty() && report.results.none { it.isFailure }) out += "Nichts geändert."
        return out
    }
    override fun reportIgnoredTitle(n: Int) = "$n lassen sich nicht einfrieren"
    override val reportIgnoredBody = "Das Telefon nimmt den Befehl an, lässt die App aber aktiv. Das ist eine Sperre des Herstellers. „Andere Wege“ probiert dann Entfernen, Verstecken und Stilllegen – alles lässt sich rückgängig machen."
    override fun reportFailedTitle(n: Int) = "$n fehlgeschlagen"
    override fun reportProtected(n: Int) = "$n gesperrte System-Apps wurden übersprungen."
    override fun tryOtherWays(n: Int) = if (n == 1) "Andere Wege versuchen" else "Andere Wege für diese $n"
    override fun removeTitle(n: Int) = if (n == 1) "App entfernen?" else "$n Apps entfernen?"
    override val removeBody = "Die App wird nur für dich deinstalliert (wie bei Canta). Die Kopie im System und die App-Daten bleiben erhalten. Rückgängig: „Zurückholen“ in der App-Ansicht oder „Alles zurücksetzen“ auf der Startseite – ein Werksreset bringt sie ebenfalls zurück."
    override val removeConfirm = "Entfernen"
    override fun outcome(o: Outcome) = when (o) {
        Outcome.FROZEN -> "eingefroren"
        Outcome.ALREADY_FROZEN -> "war schon eingefroren"
        Outcome.FREEZE_IGNORED -> "Einfrieren vom Telefon ignoriert"
        Outcome.THAWED -> "freigegeben"
        Outcome.ALREADY_ACTIVE -> "war schon aktiv"
        Outcome.THAW_FAILED -> "Freigeben hat nicht geklappt"
        Outcome.REMOVED -> "entfernt"
        Outcome.REMOVE_FAILED -> "Entfernen hat nicht geklappt"
        Outcome.HIDDEN -> "versteckt"
        Outcome.NEUTRALIZED -> "stillgelegt"
        Outcome.NOTHING_WORKED -> "kein Weg hat gewirkt"
        Outcome.RESTORED -> "zurückgeholt"
        Outcome.RESTORE_FAILED -> "Zurückholen hat nicht geklappt"
        Outcome.NOT_PRESENT -> "nicht vorhanden"
        Outcome.PROTECTED -> "gesperrt (System-Basis)"
        Outcome.DEFAULT_APP -> "ist gerade Standard-App"
        Outcome.INVALID_NAME -> "ungültiger Paketname"
    }

    override val otherWays = "Andere Wege …"
    override fun otherWaysTitle(n: Int) = if (n == 1) "App auf anderem Weg abschalten?" else "$n Apps auf anderem Weg abschalten?"
    override val otherWaysBody = "Das Telefon lässt diese App nicht normal abschalten. Die App probiert der Reihe nach diese Wege und hört beim ersten auf, der wirklich wirkt – jeder wird am Telefon nachgeprüft:"
    override fun otherWaysSteps(owner: Boolean) = listOf(
        "1. Für dich entfernen – wie Canta. Die Kopie im System und die Daten bleiben.",
        if (owner) "2. Als Geräteinhaber verstecken – ein anderer Weg, den Hersteller oft nicht sperren."
        else "2. Als Geräteinhaber verstecken – wird übersprungen, weil nicht eingerichtet (Hilfe → Geräteinhaber).",
        "3. Stilllegen – pausieren, beenden, Hintergrund verbieten. Die App lässt sich nicht mehr öffnen; schwächer als Einfrieren.",
    )
    override val otherWaysUndo = "Rückgängig: „Zurückholen“ bzw. „Freigeben“ in der App-Ansicht oder „Alles zurücksetzen“ auf der Startseite."
    override val otherWaysConfirm = "Versuchen"
    override fun wayName(way: Way) = when (way) {
        Way.REMOVE -> "Entfernen"
        Way.HIDE -> "Verstecken (Geräteinhaber)"
        Way.NEUTRALIZE -> "Stilllegen"
    }
    override fun attemptLine(a: Attempt) = when {
        a.ok -> "✓ ${wayName(a.way)}"
        a.skipped -> "– ${wayName(a.way)}: übersprungen (nicht eingerichtet)"
        a.phoneSays.isNotEmpty() -> "✗ ${wayName(a.way)} – Telefon meldet: ${a.phoneSays}"
        else -> "✗ ${wayName(a.way)} – vom Telefon ignoriert"
    }
    override val reportChainsTitle = "Andere Wege"
    override val reportOwnerTip = "Tipp: Mit eingerichtetem Geräteinhaber (Hilfe → Geräteinhaber) kommt ein stärkerer Weg dazu. Danach in der App-Ansicht erneut „Andere Wege …“ wählen."
    override val reportNeutralizedNote = "Stillgelegt heißt: Die App lässt sich nicht öffnen und darf nicht im Hintergrund laufen. Das System kann sie aber weiter kurz starten – ganz aus ist sie nur eingefroren, entfernt oder versteckt."

    override val ownerTitle = "Geräteinhaber (für hartnäckige Apps)"
    override fun ownerStatus(isOwner: Boolean) =
        if (isOwner) "Eingerichtet – „Andere Wege“ kann Apps verstecken." else "Nicht eingerichtet (optional)."
    override fun ownerBody(isOwner: Boolean) = if (isOwner) listOf(
        "„Abgeben“ holt zuerst alle hier versteckten Apps zurück und gibt dann den Geräteinhaber ab. Solange die App Geräteinhaber ist, lässt sie sich nicht deinstallieren.",
    ) else listOf(
        "Manche Hersteller sperren bei System-Apps sowohl Einfrieren als auch Entfernen. Als Geräteinhaber darf diese App Apps über die Geräteverwaltung verstecken – ein anderer Weg, den die Sperre oft nicht abdeckt. Ob er auf deinem Telefon wirkt, zeigt erst der Versuch.",
        "Android erlaubt das Einrichten nur, wenn kein Konto angemeldet ist (Google, OnePlus …) und es keinen zweiten Nutzer, kein Arbeitsprofil und keine geklonten Apps gibt. Die App entfernt dafür nichts, sie zeigt die Antwort des Telefons. Konten kannst du danach wieder hinzufügen.",
        "Danach lässt sich diese App erst nach „Abgeben“ deinstallieren. Es gibt nur einen Geräteinhaber pro Telefon – ist schon eine andere App eingerichtet (z. B. App Ops Shizuku), nutze deren Verstecken.",
    )
    override fun ownerResult(r: OwnerSetup.Result) = when (r) {
        OwnerSetup.Result.OK -> "Eingerichtet."
        OwnerSetup.Result.ACCOUNTS -> "Abgelehnt: Auf dem Telefon sind Konten angemeldet. Vorübergehend entfernen (Einstellungen → Konten), einrichten, danach wieder hinzufügen."
        OwnerSetup.Result.USERS -> "Abgelehnt: Es gibt einen zweiten Nutzer, ein Arbeitsprofil oder geklonte Apps. Diese vorher entfernen."
        OwnerSetup.Result.OTHER_OWNER -> "Abgelehnt: Eine andere App ist schon Geräteinhaber – davon gibt es nur einen."
        OwnerSetup.Result.OTHER -> "Nicht eingerichtet."
    }
    override val ownerSetup = "Einrichten"
    override val ownerRelease = "Abgeben"
    override fun ownerReleaseBody(hidden: Int) =
        (if (hidden > 0) "$hidden versteckte Apps werden zuerst zurückgeholt. " else "") +
            "Danach ist diese App kein Geräteinhaber mehr und lässt sich wieder normal deinstallieren."
    override val ownerReleaseFailed = "Abgeben hat nicht geklappt: Eine App ist noch versteckt, oder Android hat abgelehnt. Details im Protokoll."
    override val ownerPcHint = "Ohne Shizuku geht es auch am PC – gleiche Rechte, gleiche Bedingungen:"

    override val appTitle = "OnePlus Clean-Up"
    override val appSubtitle = "Bloatware und Telemetrie auf OxygenOS und ColorOS einfrieren – ohne Root, jederzeit rückgängig."
    override fun connectionTitle(state: ShizukuState) = when (state) {
        ShizukuState.NOT_INSTALLED -> "Shizuku fehlt"
        ShizukuState.NOT_RUNNING -> "Shizuku läuft nicht"
        ShizukuState.NEEDS_PERMISSION -> "Zugriff erlauben"
        ShizukuState.CONNECTING -> "Verbinde mit Shizuku …"
        ShizukuState.READY -> "Verbunden über Shizuku"
    }
    override fun connectionBody(state: ShizukuState) = when (state) {
        ShizukuState.NOT_INSTALLED -> "Zum Einfrieren braucht die App dieselben Rechte wie ADB. Die kostenlose App Shizuku stellt sie bereit – ganz ohne PC und ohne Root."
        ShizukuState.NOT_RUNNING -> "Shizuku ist installiert, aber nicht gestartet. Nach jedem Neustart des Telefons muss Shizuku einmal neu gestartet werden."
        ShizukuState.NEEDS_PERMISSION -> "Shizuku läuft. Erlaube dieser App jetzt den Zugriff – es erscheint eine Abfrage von Shizuku."
        ShizukuState.CONNECTING -> "Einen Moment, der Hilfsdienst wird gestartet."
        ShizukuState.READY -> "Alles bereit. Du kannst Apps einfrieren, entfernen, zurückholen und die Telemetrie sperren."
    }
    override val getShizuku = "Shizuku installieren"
    override val openShizuku = "Shizuku öffnen"
    override val allowAccess = "Zugriff erlauben"
    override val viewOnlyHint = "Ohne Shizuku arbeitet die App im Anzeige-Modus: Sie zeigt den Zustand aller Apps und erklärt, wie du sie von Hand deaktivierst."
    override val statFrozen = "eingefroren"
    override val statRemoved = "entfernt"
    override val statSafeActive = "sichere noch aktiv"
    override val statNote = "Die Zähler zeigen den Stand des ganzen Telefons – auch was das PC-Skript, Canta oder du selbst vorher abgeschaltet hast. Tippen öffnet die Liste."
    override val bulkButton = "Alle auf einmal …"
    override val bulkTitle = "Alle auf einmal einfrieren"
    override val bulkBody = "Wähle die Listen. Eingefroren wird alles daraus, was auf dem Telefon noch aktiv ist."
    override fun bulkList(name: String, n: Int) = "$name ($n aktiv)"
    override val bulkOptionalWarning = "Optional enthält u. a. Chrome, Google Fotos, Android Auto und den Privaten Safe (Inhalt danach nicht erreichbar). Besser einzeln auswählen."
    override val bulkNote = "Kern-Apps aus „Vorsicht“ sind nie dabei – die schaltest du einzeln ab. Du kannst auch in den Listen Häkchen setzen; die Auswahl gilt über alle Tabs."
    override fun bulkConfirm(n: Int) = if (n == 0) "Nichts zu tun" else "$n einfrieren"
    override fun driftTitle(n: Int) = if (n == 1) "1 App ist wieder aktiv" else "$n Apps sind wieder aktiv"
    override val driftBody = "Diese Apps hattest du eingefroren oder entfernt. Etwas – meist ein System-Update – hat sie zurückgebracht:"
    override val driftApply = "Erneut anwenden"
    override val allGoodTitle = "Alles noch wie eingestellt"
    override val allGoodBody = "Alle von dir eingefrorenen und entfernten Apps sind weiterhin aus. Nach einem System-Update lohnt sich ein Blick hierher."
    override val startTitle = "Loslegen"
    override val startBody = "Beginne mit der sicheren Liste: wähle aus, was weg soll, und tippe auf „Einfrieren“. Eigene Wünsche kommen in deine eigene Liste."
    override val resetTitle = "Alles zurücksetzen"
    override val resetBody = "Gibt alle Apps wieder frei, die diese App eingefroren oder stillgelegt hat, und holt entfernte und versteckte zurück. Was vorher schon aus war (PC-Skript, Canta), gibst du im Tab „Aus“ einzeln frei."
    override val resetButton = "Zurücksetzen"
    override fun resetConfirmBody(frozen: Int, removed: Int) =
        "$frozen eingefrorene oder stillgelegte Apps werden freigegeben, $removed entfernte oder versteckte zurückgeholt. Die Telemetrie-Sperre setzt du im Tab „Telemetrie“ zurück."
    override val logTitle = "Protokoll"
    override val showAll = "Alles zeigen"
    override val showLess = "Weniger"
    override val clearLog = "Leeren"
    override fun versionLine(version: String) = "Version $version · Vorabversion, auf wenigen Geräten getestet"

    override val telemetryTitle = "Telemetrie sperren"
    override val telemetryWhatTitle = "Was passiert hier?"
    override val telemetryWhatBody = "Fünf Dienste des Herstellers sammeln Nutzungs- und Diagnosedaten. „Sperren“ verbietet ihnen, im Hintergrund zu laufen und das Telefon wach zu halten (App-Ops). Nichts wird gelöscht; die Sperre übersteht einen Neustart.\n\nManche Telefone lehnen die Sperre für einzelne Dienste ab. Dann ist Einfrieren der wirksame Weg – die App bietet das direkt an. Eingefrorene Dienste laufen ohnehin nicht und brauchen keine Sperre."
    override val telemetryNeedsShellTitle = "Shizuku nötig"
    override val telemetryNeedsShellBody = "Den Stand der Sperren kann nur ADB oder Shizuku lesen und ändern. Ohne Verbindung siehst du hier nur, ob die Dienste installiert oder eingefroren sind."
    override val opsBlock = "Sperren"
    override val opsReset = "Sperre aufheben"
    override val opsLoading = "Lese Stand …"
    override val opsUnknown = "Stand unbekannt (Shizuku nicht verbunden)."
    override val telemetryLegend = "Hintergrund = darf im Hintergrund starten · Hintergrund (alle) = strengere Variante davon · Wachhalten = darf das Telefon am Schlafen hindern."
    override fun opName(op: String) = opLabel(op, "Hintergrund", "Hintergrund (alle)", "Wachhalten")
    override val opBlocked = "gesperrt"
    override val opOpen = "offen"
    override val opNotRunning = "läuft nicht"
    override fun opsReportLines(report: OpsReport): List<String> {
        val out = mutableListOf<String>()
        fun line(n: Int, text: String) { if (n > 0) out += "$n $text" }
        line(report.count(OpOutcome.BLOCKED), "Sperren gesetzt")
        line(report.count(OpOutcome.RESET), "Sperren aufgehoben")
        line(report.count(OpOutcome.SKIPPED_NOT_RUNNING), "übersprungen (Dienst ist eingefroren, Sperre unnötig)")
        line(report.count(OpOutcome.NOT_PRESENT), "nicht vorhanden")
        line(report.count(OpOutcome.REFUSED), "vom Telefon abgelehnt")
        return out
    }
    override val opsRefusedTitle = "Vom Telefon abgelehnt"
    override val opsRefusedBody = "Das Telefon nimmt den Befehl an, speichert ihn aber nicht. Das lässt sich nicht erzwingen. Wirksame Alternative: den Dienst einfrieren – dann läuft er gar nicht mehr."
    override fun freezeInstead(n: Int) = if (n == 1) "Stattdessen einfrieren" else "Diese $n einfrieren"

    override val helpTitle = "Hilfe"
    override val helpIntro = "Kurz erklärt, was die App macht, wie du sie einrichtest und wie du alles rückgängig machst."
    override val helpTopics = listOf(
        HelpTopic(
            "Was macht diese App?",
            listOf(
                "Sie friert vorinstallierte Apps ein, die du nicht brauchst: Werbung, Datensammler, Testwerkzeuge. Eingefroren heißt deaktiviert – die App startet nicht mehr, verbraucht keinen Akku und zeigt kein Icon. Gelöscht wird nichts.",
                "Die Listen „Sicher“ und „Optional“ stammen aus dem Windows-Skript dieses Projekts. Zu jeder App steht dabei, was sie tut und was ohne sie fehlt.",
                "Root ist nicht nötig. Die App nutzt dieselben Befehle wie ADB am PC.",
            ),
        ),
        HelpTopic(
            "Shizuku einrichten (ohne PC)",
            listOf(
                "Shizuku ist eine kostenlose, quelloffene App. Sie gibt anderen Apps ADB-Rechte, wenn du es erlaubst.",
                "1. Shizuku aus dem Play Store installieren.",
                "2. Entwickleroptionen freischalten: Einstellungen → Über das Gerät → Version → 7× auf „Build-Nummer“ tippen.",
                "3. In den Entwickleroptionen „Wireless-Debugging“ einschalten (WLAN muss verbunden sein).",
                "4. In Shizuku auf „Kopplung“ tippen. Dann in den Entwickleroptionen unter Wireless-Debugging „Gerät mit Kopplungscode koppeln“ wählen und den Code in der Shizuku-Benachrichtigung eingeben.",
                "5. In Shizuku auf „Starten“ tippen.",
                "6. Zurück in diese App und auf „Zugriff erlauben“ tippen.",
                "Nach jedem Neustart des Telefons muss Shizuku neu gestartet werden (Schritt 5). Die Kopplung bleibt bestehen.",
            ),
        ),
        HelpTopic(
            "Shizuku per PC starten (statt WLAN-Kopplung)",
            listOf(
                "Android kennt keine Berechtigung, die sich einmalig per ADB vergeben ließe und das Deaktivieren anderer Apps dauerhaft erlaubt. Der Weg über ADB ist deshalb: Shizuku vom PC aus starten.",
                "1. Shizuku installieren und einmal öffnen.",
                "2. USB-Debugging einschalten, Telefon per Kabel an den PC, Abfrage am Telefon bestätigen.",
                "3. Am PC ausführen: adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                "4. Zurück in diese App und „Zugriff erlauben“ tippen.",
                "Auch das hält nur bis zum nächsten Neustart des Telefons – genau wie der Start über Wireless-Debugging. Das ist eine Vorgabe von Android, keine Einschränkung dieser App.",
            ),
        ),
        HelpTopic(
            "Ohne Shizuku: Anzeige-Modus",
            listOf(
                "Android erlaubt normalen Apps nicht, andere Apps zu deaktivieren – diese Berechtigung lässt sich auch nicht von Hand vergeben. Ohne Shizuku kann die App deshalb nur anzeigen und anleiten.",
                "Was trotzdem geht: Du siehst zu jeder App den Zustand und die Erklärung. Über „App-Info öffnen“ kommst du direkt zur Systemseite der App; bietet das Telefon dort „Deaktivieren“ an, kannst du es selbst antippen.",
                "Für alles andere zeigt die App den passenden ADB-Befehl zum Kopieren. Am Windows-PC erledigt das Skript aus diesem Projekt dieselben Schritte.",
            ),
        ),
        HelpTopic(
            "Einfrieren oder Entfernen?",
            listOf(
                "Einfrieren (pm disable-user) ist der Standard: Die App bleibt installiert, ist aber abgeschaltet.",
                "Manche Hersteller-Apps lassen sich nicht einfrieren – das Telefon nimmt den Befehl an und lässt die App trotzdem aktiv (z. B. der Hersteller-Browser). Dann bietet die App „Entfernen“ an.",
                "Entfernen (pm uninstall -k --user) deinstalliert die App nur für dich. Die Kopie im System und die App-Daten bleiben. „Zurückholen“ stellt sie wieder her, ein Werksreset ebenfalls. Diesen Weg nutzt auch die App Canta.",
                "Neuere OxygenOS-Versionen lehnen bei manchen System-Apps auch das Entfernen ab („only root can delete system app for a particular user“). Dann hilft „Andere Wege …“: Entfernen, Verstecken als Geräteinhaber (falls eingerichtet, siehe unten) und als Letztes Stilllegen.",
                "Stilllegen (pm suspend, dazu Hintergrund verboten und Standby-Stufe „eingeschränkt“) ist schwächer: Die App lässt sich nicht öffnen und startet nicht von selbst, das System kann sie aber kurz binden. Mehr geht ohne Root nicht – pm disable-until-used, pm hide und das Abschalten einzelner Bausteine verweigert Android der Shell grundsätzlich.",
            ),
        ),
        HelpTopic(
            "Rückgängig machen",
            listOf(
                "Einzeln: App in einer Liste antippen → „Freigeben“ bzw. „Zurückholen“.",
                "Alles: Startseite → „Alles zurücksetzen“. Das betrifft alles, was diese App eingefroren, entfernt, versteckt oder stillgelegt hat.",
                "Die Telemetrie-Sperre hebst du im Tab „Telemetrie“ auf.",
                "Wenn gar nichts mehr geht: Ein Werksreset stellt alle System-Apps wieder her.",
            ),
        ),
        HelpTopic(
            "Nach einem System-Update",
            listOf(
                "Updates aktivieren manchmal Apps wieder oder installieren entfernte neu. Die Startseite zeigt das an und bietet „Erneut anwenden“.",
                "Auch die Telemetrie-Sperren können zurückgesetzt werden – ein Blick in den Tab „Telemetrie“ zeigt den Stand.",
            ),
        ),
        HelpTopic(
            "Was die App nie anfasst",
            listOf(
                "Gesperrt ist nur die System-Basis: Oberfläche, Einstellungen, Telefonie-Dienst, Installer und drei Systembausteine. Ohne sie startet das Telefon nicht mehr oder ist nicht bedienbar.",
                "Kern-Apps wie Telefon, SMS, Kontakte, Kalender, Gmail und Gboard stehen im Tab „Vorsicht“. Du kannst sie abschalten, wenn du Ersatz hast. Die App verweigert es nur, solange eine davon die Standard-App ist – dasselbe gilt für jede andere Tastatur, Telefon-, SMS- oder Launcher-App.",
                "Bei allem anderen entscheidest du. Lies die Beschreibung, besonders in der Liste „Optional“: Der Private Safe zum Beispiel ist eingefroren nicht mehr erreichbar.",
            ),
        ),
        HelpTopic(
            "Grenzen",
            listOf(
                "Die App kann nicht mehr als ADB. Was das Telefon bei ADB ignoriert oder ablehnt, lehnt es auch hier ab – die App zeigt dann die Antwort des Telefons.",
                "Die Paketlisten passen nicht zu jedem Modell. Fehlende Apps werden ausgeblendet.",
                "Netzwerk-Sperren (Firewall) und Systemeinstellungen wie Autostart gehören nicht zum Umfang; dazu steht mehr in der Projektbeschreibung.",
            ),
        ),
    )
    override val shizukuGuide = "Offizielle Anleitung"
    override val openRepo = "Projektseite"
    override val reportProblem = "Problem melden"
    override val disclaimer = "Vorabversion. Nutzung auf eigene Gefahr. Alles, was die App tut, lässt sich rückgängig machen."
}

object En : Txt {
    override val german = false

    override val tabHome = "Home"
    override val tabApps = "Apps"
    override val tabTelemetry = "Telemetry"
    override val tabHelp = "Help"
    override val ok = "OK"
    override val cancel = "Cancel"
    override val notNow = "Not now"
    override val add = "Add"
    override val refresh = "Refresh"
    override val howTo = "How to"
    override val searchHint = "Search name or package"
    override val systemApp = "System app"
    override fun andMore(n: Int) = "… and $n more"
    override fun phoneSays(text: String) = "Phone says: $text"
    override fun message(m: Message) = when (m) {
        Message.NEEDS_SHELL -> "Shizuku has to be connected for this (see Home)."
        Message.CONNECTION_LOST -> "Lost the connection to Shizuku. Restart Shizuku and try again."
        Message.IMPORTED -> "List imported."
        Message.IMPORT_FAILED -> "The file does not contain a valid list."
        Message.NOTHING_SELECTED -> "Nothing to do."
        Message.COPIED -> "Command copied."
    }

    override val stateActive = "active"
    override val stateFrozen = "frozen"
    override val stateRemoved = "removed"
    override val stateNotPresent = "not present"
    override val stateHidden = "hidden"
    override val stateSuspended = "suspended"
    override fun stateExplanation(state: PkgState) = when (state) {
        PkgState.ACTIVE -> "State: active – the app is installed and can run."
        PkgState.FROZEN -> "State: frozen – the app is disabled. It does not start, uses no battery and shows no icon. Nothing was deleted."
        PkgState.REMOVED -> "State: removed – uninstalled for you, the copy in the system is still there. “Bring back” restores it, and so does a factory reset."
        PkgState.NOT_PRESENT -> "This app does not exist on your phone. The lists cover many models; missing entries are normal."
        PkgState.HIDDEN -> "State: hidden – hidden for you through the device owner. The app does not run and shows no icon; the copy in the system and the data stay. “Bring back” makes it visible again."
        PkgState.SUSPENDED -> "State: suspended – the app is paused and cannot be opened. If this app suspended it, it may not run in the background either. Weaker than freezing: the system can still start it briefly. Digital Wellbeing's “Pause app” also leads to this state."
    }

    override val listSafe = "Safe"
    override val listOptional = "Optional"
    override val listCustom = "My list"
    override val listCaution = "Caution"
    override val listOff = "Off"
    override val locked = "locked"
    override fun listIntro(tab: Int) = when (tab) {
        0 -> "Telemetry, ads, logging and test tools. You can freeze these without missing anything day to day. Tap an entry for details."
        1 -> "A matter of taste: features you may use depend on these (Chrome, Photos, Android Auto, Private Safe …). Read the description before freezing."
        2 -> "Your own list. Add any installed app and freeze it the same way. The list can be saved to a file and moved to another phone."
        3 -> "Core apps such as phone, texts, contacts and keyboard. You can freeze them if you have a replacement – one at a time, after a prompt. While one of them is the phone's default app, the app refuses. Entries with a lock are the system base: they stay locked because the phone would no longer boot or be usable without them."
        else -> "Everything that is currently disabled or removed for you on this phone – whether by this app, the PC script, Canta, yourself or the factory. Tap an entry to release it or bring it back."
    }
    override fun listSummary(present: Int, all: Int) = "$present of $all present on this phone"
    override fun showMissing(n: Int) = "Show missing ($n)"
    override fun selectAllActive(n: Int) = "Select all active ($n)"
    override val selectNone = "Clear selection"
    override fun selectedCount(n: Int) = "$n selected"
    override val listEmpty = "No entries. Nothing from this list is present on this phone (any more)."
    override val customEmpty = "Still empty. Tap “Add apps” to choose installed apps."
    override val addApps = "Add apps"
    override val importList = "Import"
    override val exportList = "Export"
    override val pickerTitle = "Add apps"
    override val pickerIntro = "Choose apps for your own list. Adding changes nothing yet – freezing only happens when you start it from the list."
    override val showSystemApps = "Show system apps"
    override fun addCount(n: Int) = if (n == 0) "Add" else "Add $n"
    override val manualAddTitle = "Enter a package name by hand"

    override val freeze = "Freeze"
    override val thaw = "Release"
    override val remove = "Remove"
    override val restore = "Bring back"
    override val systemSettings = "Open app info"
    override val removeFromList = "Take off my list"
    override val freezeVsRemove = "Freezing is the gentle way. Some phones ignore it for certain system apps – then “Remove” helps (uninstall for you, can be brought back any time). If the phone refuses that too, “Other ways …” tries everything that works without root."
    override val protectedHint = "Locked: part of the system base. The app neither freezes nor removes it because the phone would no longer boot or be usable without it."
    override val cautionHint = "Caution: core app. Only freeze it if another app takes over its job."
    override val cautionTitle = "Really switch off a core app?"
    override val cautionBody = "First make sure another app takes over this job and is set as default in the settings. You can undo this at any time on the “Off” tab."
    override val cautionConfirm = "I have a replacement"
    override fun defaultRole(role: DefaultRole) = when (role) {
        DefaultRole.KEYBOARD -> "active keyboard"
        DefaultRole.DIALER -> "default phone app"
        DefaultRole.SMS -> "default SMS app"
        DefaultRole.LAUNCHER -> "default home screen"
    }
    override fun defaultHint(role: DefaultRole) = when (role) {
        DefaultRole.KEYBOARD -> "This is your active keyboard right now. Select another keyboard first (Settings → System → Keyboard), otherwise you cannot type anything – not even your PIN."
        DefaultRole.DIALER -> "This is your default phone app right now. Set another one first (Settings → Apps → Default apps), otherwise no calls are possible."
        DefaultRole.SMS -> "This is your default SMS app right now. Set another one first (Settings → Apps → Default apps), otherwise no texts arrive."
        DefaultRole.LAUNCHER -> "This is your home screen right now. Set another one first (Settings → Apps → Default apps), otherwise the phone has no interface."
    }
    override fun reportDefaultApp(n: Int) = "$n skipped because they are the phone's default app right now (keyboard, phone, SMS or home screen). Set a replacement as default first."
    override val criticalDescription = "Part of the system base (interface, settings, telephony or installer). Without it the phone becomes unusable."
    override val noDescription = "There is no description for this app – it is on none of the prepared lists. Only freeze what you know; when in doubt, search for the package name."
    override val manualTitle = "Without Shizuku"
    override val manualBody = "Without Shizuku the app may not change anything. You can do it yourself: “Open app info” → “Disable” (if the phone offers the button), or on a PC with this ADB command:"
    override val copyCommand = "Copy command"
    override val needsShellShort = "Shizuku has to be connected for this (Home → connection)."

    override fun progressTitle(kind: BatchKind) = when (kind) {
        BatchKind.FREEZE -> "Freezing …"
        BatchKind.REMOVE -> "Removing …"
        BatchKind.RESET -> "Restoring …"
        BatchKind.REAPPLY -> "Applying again …"
        BatchKind.HARD -> "Trying other ways …"
    }
    override val progressHint = "Please keep the app open."
    override val reportTitle = "Result"
    override fun reportLines(report: Report): List<String> {
        val out = mutableListOf<String>()
        fun line(n: Int, text: String) { if (n > 0) out += "$n $text" }
        line(report.count(Outcome.FROZEN), "frozen")
        line(report.count(Outcome.ALREADY_FROZEN), "were already frozen")
        line(report.count(Outcome.REMOVED), "removed")
        line(report.count(Outcome.HIDDEN), "hidden")
        line(report.count(Outcome.NEUTRALIZED), "suspended")
        line(report.count(Outcome.THAWED), "released")
        line(report.count(Outcome.RESTORED), "brought back")
        line(report.count(Outcome.ALREADY_ACTIVE), "were already active")
        line(report.count(Outcome.NOT_PRESENT), "not present")
        if (out.isEmpty() && report.results.none { it.isFailure }) out += "Nothing changed."
        return out
    }
    override fun reportIgnoredTitle(n: Int) = "$n cannot be frozen"
    override val reportIgnoredBody = "The phone accepts the command but keeps the app active. That is a vendor lock. “Other ways” then tries removing, hiding and suspending – all of it can be undone."
    override fun reportFailedTitle(n: Int) = "$n failed"
    override fun reportProtected(n: Int) = "$n locked system apps were skipped."
    override fun tryOtherWays(n: Int) = if (n == 1) "Try other ways" else "Other ways for these $n"
    override fun removeTitle(n: Int) = if (n == 1) "Remove app?" else "Remove $n apps?"
    override val removeBody = "The app is uninstalled for you only (like Canta does). The copy in the system and the app data are kept. Undo: “Bring back” in the app view or “Reset everything” on the home screen – a factory reset restores it too."
    override val removeConfirm = "Remove"
    override fun outcome(o: Outcome) = when (o) {
        Outcome.FROZEN -> "frozen"
        Outcome.ALREADY_FROZEN -> "was already frozen"
        Outcome.FREEZE_IGNORED -> "freeze ignored by the phone"
        Outcome.THAWED -> "released"
        Outcome.ALREADY_ACTIVE -> "was already active"
        Outcome.THAW_FAILED -> "release failed"
        Outcome.REMOVED -> "removed"
        Outcome.REMOVE_FAILED -> "remove failed"
        Outcome.HIDDEN -> "hidden"
        Outcome.NEUTRALIZED -> "suspended"
        Outcome.NOTHING_WORKED -> "no way worked"
        Outcome.RESTORED -> "brought back"
        Outcome.RESTORE_FAILED -> "bringing back failed"
        Outcome.NOT_PRESENT -> "not present"
        Outcome.PROTECTED -> "locked (system base)"
        Outcome.DEFAULT_APP -> "is the default app right now"
        Outcome.INVALID_NAME -> "invalid package name"
    }

    override val otherWays = "Other ways …"
    override fun otherWaysTitle(n: Int) = if (n == 1) "Switch off the app another way?" else "Switch off $n apps another way?"
    override val otherWaysBody = "The phone does not let this app be switched off the normal way. The app tries these ways in order and stops at the first one that really works – each is checked on the phone:"
    override fun otherWaysSteps(owner: Boolean) = listOf(
        "1. Remove for you – like Canta. The copy in the system and the data stay.",
        if (owner) "2. Hide as device owner – a different path vendors often leave open."
        else "2. Hide as device owner – skipped because it is not set up (Help → Device owner).",
        "3. Suspend – pause, stop, no background work. The app can no longer be opened; weaker than freezing.",
    )
    override val otherWaysUndo = "Undo: “Bring back” or “Release” in the app view, or “Reset everything” on the home screen."
    override val otherWaysConfirm = "Try"
    override fun wayName(way: Way) = when (way) {
        Way.REMOVE -> "Remove"
        Way.HIDE -> "Hide (device owner)"
        Way.NEUTRALIZE -> "Suspend"
    }
    override fun attemptLine(a: Attempt) = when {
        a.ok -> "✓ ${wayName(a.way)}"
        a.skipped -> "– ${wayName(a.way)}: skipped (not set up)"
        a.phoneSays.isNotEmpty() -> "✗ ${wayName(a.way)} – phone says: ${a.phoneSays}"
        else -> "✗ ${wayName(a.way)} – ignored by the phone"
    }
    override val reportChainsTitle = "Other ways"
    override val reportOwnerTip = "Tip: with device owner set up (Help → Device owner) a stronger way is added. Then choose “Other ways …” in the app view again."
    override val reportNeutralizedNote = "Suspended means: the app cannot be opened and may not run in the background. The system can still start it briefly – it is only fully off when frozen, removed or hidden."

    override val ownerTitle = "Device owner (for stubborn apps)"
    override fun ownerStatus(isOwner: Boolean) =
        if (isOwner) "Set up – “Other ways” can hide apps." else "Not set up (optional)."
    override fun ownerBody(isOwner: Boolean) = if (isOwner) listOf(
        "“Give up” first brings back every app hidden here and then gives up device owner. While the app is device owner it cannot be uninstalled.",
    ) else listOf(
        "Some vendors block both freezing and removing for system apps. As device owner this app may hide apps through the device policy API – a different path the lock often does not cover. Only trying shows whether it works on your phone.",
        "Android only allows the set-up while no account is signed in (Google, OnePlus …) and there is no second user, no work profile and no cloned apps. The app removes nothing for this; it shows the phone's answer. You can add the accounts again afterwards.",
        "Afterwards this app can only be uninstalled after “Give up”. There is only one device owner per phone – if another app is set up already (e.g. App Ops Shizuku), use its hiding instead.",
    )
    override fun ownerResult(r: OwnerSetup.Result) = when (r) {
        OwnerSetup.Result.OK -> "Set up."
        OwnerSetup.Result.ACCOUNTS -> "Refused: accounts are signed in on the phone. Remove them for now (Settings → Accounts), set up, then add them again."
        OwnerSetup.Result.USERS -> "Refused: there is a second user, a work profile or cloned apps. Remove those first."
        OwnerSetup.Result.OTHER_OWNER -> "Refused: another app is device owner already – there can only be one."
        OwnerSetup.Result.OTHER -> "Not set up."
    }
    override val ownerSetup = "Set up"
    override val ownerRelease = "Give up"
    override fun ownerReleaseBody(hidden: Int) =
        (if (hidden > 0) "$hidden hidden apps will be brought back first. " else "") +
            "Afterwards this app is no longer device owner and can be uninstalled normally."
    override val ownerReleaseFailed = "Giving up did not work: an app is still hidden, or Android refused. Details in the log."
    override val ownerPcHint = "Without Shizuku it works from a PC too – same rights, same conditions:"

    override val appTitle = "OnePlus Clean-Up"
    override val appSubtitle = "Freeze bloatware and telemetry on OxygenOS and ColorOS – no root, reversible at any time."
    override fun connectionTitle(state: ShizukuState) = when (state) {
        ShizukuState.NOT_INSTALLED -> "Shizuku is missing"
        ShizukuState.NOT_RUNNING -> "Shizuku is not running"
        ShizukuState.NEEDS_PERMISSION -> "Allow access"
        ShizukuState.CONNECTING -> "Connecting to Shizuku …"
        ShizukuState.READY -> "Connected through Shizuku"
    }
    override fun connectionBody(state: ShizukuState) = when (state) {
        ShizukuState.NOT_INSTALLED -> "To freeze apps this app needs the same rights as ADB. The free app Shizuku provides them – no PC and no root."
        ShizukuState.NOT_RUNNING -> "Shizuku is installed but not started. It has to be started once after every reboot of the phone."
        ShizukuState.NEEDS_PERMISSION -> "Shizuku is running. Allow this app to use it now – Shizuku will show a prompt."
        ShizukuState.CONNECTING -> "One moment, the helper service is starting."
        ShizukuState.READY -> "All set. You can freeze, remove and bring back apps and lock the telemetry."
    }
    override val getShizuku = "Install Shizuku"
    override val openShizuku = "Open Shizuku"
    override val allowAccess = "Allow access"
    override val viewOnlyHint = "Without Shizuku the app works in view-only mode: it shows the state of every app and explains how to disable it by hand."
    override val statFrozen = "frozen"
    override val statRemoved = "removed"
    override val statSafeActive = "safe ones still active"
    override val statNote = "The counters show the state of the whole phone – including what the PC script, Canta or you switched off before. Tap to open the list."
    override val bulkButton = "Everything at once …"
    override val bulkTitle = "Freeze everything at once"
    override val bulkBody = "Choose the lists. Everything from them that is still active on the phone gets frozen."
    override fun bulkList(name: String, n: Int) = "$name ($n active)"
    override val bulkOptionalWarning = "Optional includes Chrome, Google Photos, Android Auto and the Private Safe (content unreachable afterwards). Better pick those one by one."
    override val bulkNote = "Core apps from “Caution” are never included – switch those off one at a time. You can also tick entries in the lists; the selection is kept across all tabs."
    override fun bulkConfirm(n: Int) = if (n == 0) "Nothing to do" else "Freeze $n"
    override fun driftTitle(n: Int) = if (n == 1) "1 app is active again" else "$n apps are active again"
    override val driftBody = "You had frozen or removed these apps. Something – usually a system update – brought them back:"
    override val driftApply = "Apply again"
    override val allGoodTitle = "Everything still as you set it"
    override val allGoodBody = "All apps you froze or removed are still off. It is worth checking here after a system update."
    override val startTitle = "Get started"
    override val startBody = "Start with the safe list: choose what should go and tap “Freeze”. Your own wishes go into your own list."
    override val resetTitle = "Reset everything"
    override val resetBody = "Releases every app this app froze or suspended and brings back the removed and hidden ones. Whatever was already off before (PC script, Canta) is released one by one on the “Off” tab."
    override val resetButton = "Reset"
    override fun resetConfirmBody(frozen: Int, removed: Int) =
        "$frozen frozen or suspended apps will be released, $removed removed or hidden ones brought back. The telemetry lock is reset on the “Telemetry” tab."
    override val logTitle = "Log"
    override val showAll = "Show all"
    override val showLess = "Less"
    override val clearLog = "Clear"
    override fun versionLine(version: String) = "Version $version · pre-release, tested on few devices"

    override val telemetryTitle = "Lock telemetry"
    override val telemetryWhatTitle = "What happens here?"
    override val telemetryWhatBody = "Five vendor services collect usage and diagnostic data. “Lock” forbids them to run in the background and to keep the phone awake (app-ops). Nothing is deleted; the lock survives a reboot.\n\nSome phones refuse the lock for certain services. Freezing is the effective way then – the app offers it right away. Frozen services do not run anyway and need no lock."
    override val telemetryNeedsShellTitle = "Shizuku needed"
    override val telemetryNeedsShellBody = "Only ADB or Shizuku can read and change the locks. Without a connection you only see whether the services are installed or frozen."
    override val opsBlock = "Lock"
    override val opsReset = "Lift lock"
    override val opsLoading = "Reading state …"
    override val opsUnknown = "State unknown (Shizuku not connected)."
    override val telemetryLegend = "Background = may start in the background · Background (any) = the stricter variant · Keep awake = may keep the phone from sleeping."
    override fun opName(op: String) = opLabel(op, "Background", "Background (any)", "Keep awake")
    override val opBlocked = "locked"
    override val opOpen = "open"
    override val opNotRunning = "not running"
    override fun opsReportLines(report: OpsReport): List<String> {
        val out = mutableListOf<String>()
        fun line(n: Int, text: String) { if (n > 0) out += "$n $text" }
        line(report.count(OpOutcome.BLOCKED), "locks set")
        line(report.count(OpOutcome.RESET), "locks lifted")
        line(report.count(OpOutcome.SKIPPED_NOT_RUNNING), "skipped (service is frozen, lock unnecessary)")
        line(report.count(OpOutcome.NOT_PRESENT), "not present")
        line(report.count(OpOutcome.REFUSED), "refused by the phone")
        return out
    }
    override val opsRefusedTitle = "Refused by the phone"
    override val opsRefusedBody = "The phone accepts the command but does not store it. This cannot be forced. Effective alternative: freeze the service – then it does not run at all."
    override fun freezeInstead(n: Int) = if (n == 1) "Freeze instead" else "Freeze these $n"

    override val helpTitle = "Help"
    override val helpIntro = "What the app does, how to set it up and how to undo everything."
    override val helpTopics = listOf(
        HelpTopic(
            "What does this app do?",
            listOf(
                "It freezes pre-installed apps you do not need: ads, data collectors, test tools. Frozen means disabled – the app no longer starts, uses no battery and shows no icon. Nothing is deleted.",
                "The “Safe” and “Optional” lists come from this project's Windows script. Every app comes with a note on what it does and what you lose without it.",
                "No root needed. The app uses the same commands as ADB on a PC.",
            ),
        ),
        HelpTopic(
            "Setting up Shizuku (no PC)",
            listOf(
                "Shizuku is a free, open-source app. It gives other apps ADB rights when you allow it.",
                "1. Install Shizuku from the Play Store.",
                "2. Unlock developer options: Settings → About device → Version → tap “Build number” 7 times.",
                "3. In developer options turn on “Wireless debugging” (Wi-Fi must be connected).",
                "4. In Shizuku tap “Pairing”. Then, under Wireless debugging, choose “Pair device with pairing code” and enter the code in the Shizuku notification.",
                "5. In Shizuku tap “Start”.",
                "6. Come back to this app and tap “Allow access”.",
                "After every reboot Shizuku has to be started again (step 5). The pairing stays.",
            ),
        ),
        HelpTopic(
            "Starting Shizuku from a PC (instead of Wi-Fi pairing)",
            listOf(
                "Android has no permission that could be granted once over ADB and would allow disabling other apps for good. The ADB route therefore is: start Shizuku from the PC.",
                "1. Install Shizuku and open it once.",
                "2. Turn on USB debugging, connect the phone to the PC by cable, confirm the prompt on the phone.",
                "3. Run on the PC: adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh",
                "4. Come back to this app and tap “Allow access”.",
                "This too only lasts until the next reboot of the phone – just like starting through wireless debugging. That is an Android rule, not a limitation of this app.",
            ),
        ),
        HelpTopic(
            "Without Shizuku: view-only mode",
            listOf(
                "Android does not let normal apps disable other apps – and that permission cannot be granted by hand either. Without Shizuku the app can only show and guide.",
                "What still works: you see the state and the explanation of every app. “Open app info” takes you to the app's system page; if the phone offers “Disable” there, you can tap it yourself.",
                "For everything else the app shows the matching ADB command to copy. On a Windows PC the script from this project does the same steps.",
            ),
        ),
        HelpTopic(
            "Freeze or remove?",
            listOf(
                "Freezing (pm disable-user) is the default: the app stays installed but is switched off.",
                "Some vendor apps cannot be frozen – the phone accepts the command and keeps the app active anyway (e.g. the vendor browser). The app then offers “Remove”.",
                "Removing (pm uninstall -k --user) uninstalls the app for you only. The copy in the system and the app data stay. “Bring back” restores it, and so does a factory reset. The app Canta uses the same method.",
                "Newer OxygenOS versions refuse removing some system apps as well (“only root can delete system app for a particular user”). Then “Other ways …” helps: removing, hiding as device owner (if set up, see below) and, as the last step, suspending.",
                "Suspending (pm suspend, plus no background work and the “restricted” standby bucket) is weaker: the app cannot be opened and does not start on its own, but the system can still bind to it briefly. Without root there is nothing beyond this – Android refuses pm disable-until-used, pm hide and switching off single components to the shell user.",
            ),
        ),
        HelpTopic(
            "Undoing things",
            listOf(
                "One app: tap it in a list → “Release” or “Bring back”.",
                "Everything: Home → “Reset everything”. This covers all that this app froze, removed, hid or suspended.",
                "The telemetry lock is lifted on the “Telemetry” tab.",
                "If nothing works any more: a factory reset restores every system app.",
            ),
        ),
        HelpTopic(
            "After a system update",
            listOf(
                "Updates sometimes re-enable apps or reinstall removed ones. The home screen shows this and offers “Apply again”.",
                "The telemetry locks can be reset as well – the “Telemetry” tab shows where they stand.",
            ),
        ),
        HelpTopic(
            "What the app never touches",
            listOf(
                "Only the system base is locked: interface, settings, telephony service, installer and three system components. Without them the phone no longer boots or cannot be used.",
                "Core apps such as phone, texts, contacts, calendar, Gmail and Gboard are on the “Caution” tab. You can switch them off if you have a replacement. The app only refuses while one of them is the default app – the same goes for any other keyboard, phone, SMS or launcher app.",
                "Everything else is your call. Read the description, especially on the “Optional” list: the Private Safe, for example, is unreachable while frozen.",
            ),
        ),
        HelpTopic(
            "Limits",
            listOf(
                "The app cannot do more than ADB. What the phone ignores or refuses over ADB it refuses here too – the app then shows the phone's answer.",
                "The package lists do not fit every model. Missing apps are hidden.",
                "Network blocking (firewall) and system settings such as auto-start are out of scope; the project page has more on those.",
            ),
        ),
    )
    override val shizukuGuide = "Official guide"
    override val openRepo = "Project page"
    override val reportProblem = "Report a problem"
    override val disclaimer = "Pre-release. Use at your own risk. Everything the app does can be undone."
}
