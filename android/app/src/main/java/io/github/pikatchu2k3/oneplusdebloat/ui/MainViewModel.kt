package io.github.pikatchu2k3.oneplusdebloat.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.pikatchu2k3.oneplusdebloat.core.ActionResult
import io.github.pikatchu2k3.oneplusdebloat.core.Catalog
import io.github.pikatchu2k3.oneplusdebloat.core.CatalogEntry
import io.github.pikatchu2k3.oneplusdebloat.core.CustomList
import io.github.pikatchu2k3.oneplusdebloat.core.DebloatEngine
import io.github.pikatchu2k3.oneplusdebloat.core.DriftEntry
import io.github.pikatchu2k3.oneplusdebloat.core.ListKind
import io.github.pikatchu2k3.oneplusdebloat.core.OpOutcome
import io.github.pikatchu2k3.oneplusdebloat.core.OpResult
import io.github.pikatchu2k3.oneplusdebloat.core.OpStatus
import io.github.pikatchu2k3.oneplusdebloat.core.Outcome
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import io.github.pikatchu2k3.oneplusdebloat.core.ShellUnavailableException
import io.github.pikatchu2k3.oneplusdebloat.core.isValidPackageName
import io.github.pikatchu2k3.oneplusdebloat.data.AndroidPackageSource
import io.github.pikatchu2k3.oneplusdebloat.data.AppStore
import io.github.pikatchu2k3.oneplusdebloat.data.DefaultRole
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuBackend
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/** One line in an app list. */
data class AppRow(
    val pkg: String,
    val label: String,
    val state: PkgState,
    val isSystem: Boolean,
    val entry: CatalogEntry?,
    /** System base or locked system component: never frozen or removed. */
    val isProtected: Boolean,
    /** Replaceable core app (dialer, keyboard ...): allowed after a warning, one at a time. */
    val isCaution: Boolean = false,
    /** Set while this package is the phone's default for a job; freezing is refused until replaced. */
    val defaultRole: DefaultRole? = null,
) {
    /** May be ticked for a batch freeze. */
    val bulkSelectable: Boolean
        get() = state == PkgState.ACTIVE && !isProtected && !isCaution && defaultRole == null
}

/** An installed app offered in the "add to my list" picker. */
data class PickerApp(val pkg: String, val label: String, val isSystem: Boolean, val state: PkgState)

enum class BatchKind { FREEZE, REMOVE, RESET, REAPPLY }

data class Progress(val kind: BatchKind, val done: Int, val total: Int, val current: String)

/** What a batch did; shown in a result dialog. */
data class Report(val kind: BatchKind, val results: List<ActionResult>) {
    fun count(vararg outcomes: Outcome) = results.count { it.outcome in outcomes }
    val ignored: List<ActionResult> get() = results.filter { it.outcome == Outcome.FREEZE_IGNORED }
    val failed: List<ActionResult>
        get() = results.filter { it.isFailure && it.outcome != Outcome.FREEZE_IGNORED }
}

data class OpsReport(val blocking: Boolean, val results: List<OpResult>) {
    fun count(outcome: OpOutcome) = results.count { it.outcome == outcome }
    val refusedPackages: List<String>
        get() = results.filter { it.outcome == OpOutcome.REFUSED }.map { it.pkg }.distinct()
}

data class UiState(
    val shizuku: ShizukuState = ShizukuState.NOT_INSTALLED,
    val rows: Map<String, AppRow> = emptyMap(),
    val custom: List<String> = emptyList(),
    val journalFrozen: Set<String> = emptySet(),
    val journalRemoved: Set<String> = emptySet(),
    /** Everything disabled / removed for the user on this phone, whoever did it. */
    val deviceFrozen: List<String> = emptyList(),
    val deviceRemoved: List<String> = emptyList(),
    val selected: Set<String> = emptySet(),
    val drift: List<DriftEntry> = emptyList(),
    val progress: Progress? = null,
    val report: Report? = null,
    val ops: List<OpStatus>? = null,
    val opsBusy: Boolean = false,
    val opsReport: OpsReport? = null,
    val log: List<String> = emptyList(),
    val picker: List<PickerApp>? = null,
    /** One-shot message key for a snackbar. */
    val message: Message? = null,
    val loaded: Boolean = false,
) {
    val ready: Boolean get() = shizuku == ShizukuState.READY
    fun rowsOf(pkgs: List<String>): List<AppRow> = pkgs.mapNotNull { rows[it] }
}

enum class Message { NEEDS_SHELL, CONNECTION_LOST, IMPORTED, IMPORT_FAILED, NOTHING_SELECTED, COPIED }

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val catalog: Catalog = Catalog.parse(app.assets.open("catalog.json").bufferedReader().use { it.readText() })
    private val packages = AndroidPackageSource(app)
    private val store = AppStore(app)
    private val shizuku = ShizukuBackend(app)
    private val userId = android.os.Process.myUid() / 100_000
    private val engine = DebloatEngine(
        shizuku, packages, store, catalog.protectedPackages, userId,
        isDefaultApp = { packages.defaultRoleOf(it) != null },
    )
    private val icons = ConcurrentHashMap<String, Bitmap>()

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    val safeIds = catalog.ofKind(ListKind.SAFE).map { it.id }
    val optionalIds = catalog.ofKind(ListKind.OPTIONAL).map { it.id }
    /** The "caution" tab: replaceable core apps first, then the locked system parts. */
    val cautionIds = catalog.ofKind(ListKind.NEVER).sortedBy { it.locked }.map { it.id } + catalog.critical
    val userIdForCommands: Int get() = userId

    init {
        viewModelScope.launch {
            shizuku.state.collect { s ->
                _ui.update { it.copy(shizuku = s) }
                if (s == ShizukuState.READY && _ui.value.ops == null) loadOps()
            }
        }
        shizuku.start()
        refresh()
    }

    override fun onCleared() {
        shizuku.stop()
    }

    /** Re-reads everything that needs no shell: package states, journal, drift. */
    fun refresh() {
        shizuku.refresh()
        viewModelScope.launch {
            val snapshot = withContext(Dispatchers.IO) { buildRows() }
            _ui.update {
                it.copy(
                    rows = snapshot,
                    custom = store.custom.sorted(),
                    journalFrozen = store.frozen,
                    journalRemoved = store.removed,
                    deviceFrozen = snapshot.values.filter { r -> r.state == PkgState.FROZEN }
                        .sortedBy { r -> r.label.lowercase() }.map { r -> r.pkg },
                    deviceRemoved = snapshot.values.filter { r -> r.state == PkgState.REMOVED }
                        .sortedBy { r -> r.label.lowercase() }.map { r -> r.pkg },
                    drift = engine.drift(),
                    log = store.log,
                    selected = it.selected.filter { p -> snapshot[p]?.bulkSelectable == true }.toSet(),
                    loaded = true,
                )
            }
        }
    }

    private fun buildRows(): Map<String, AppRow> {
        val protectedPkgs = catalog.protectedPackages
        val ids = LinkedHashSet<String>()
        ids += catalog.packages.map { it.id }
        ids += catalog.critical
        ids += store.custom
        ids += store.frozen
        ids += store.removed
        // Also everything that is already disabled or removed on this phone - by the PC script,
        // Canta, the user or the factory - so the counters and the "Off" tab show the real picture.
        val own = getApplication<Application>().packageName
        for (info in packages.allApps()) {
            val pkg = info.packageName
            if (pkg == own || pkg in ids || !isValidPackageName(pkg)) continue
            if (info.flags and ApplicationInfo.FLAG_INSTALLED == 0 || !info.enabled) ids += pkg
        }
        return ids.associateWith { pkg ->
            val info = packages.infoOf(pkg)
            val entry = catalog.find(pkg)
            AppRow(
                pkg = pkg,
                label = info?.let { packages.labelOf(it) }?.takeIf { it.isNotBlank() && it != pkg }
                    ?: entry?.name?.takeIf { it.isNotBlank() } ?: pkg,
                state = packages.stateOf(pkg),
                isSystem = info != null && info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                entry = entry,
                isProtected = pkg in protectedPkgs,
                isCaution = entry?.kind == ListKind.NEVER && !entry.locked,
                defaultRole = packages.defaultRoleOf(pkg),
            )
        }
    }

    fun iconOf(pkg: String): Bitmap? {
        icons[pkg]?.let { return it }
        val info = packages.infoOf(pkg) ?: return null
        return try {
            info.loadIcon(getApplication<Application>().packageManager).toBitmap(96, 96).also { icons[pkg] = it }
        } catch (_: Exception) {
            null
        }
    }

    fun requestShizukuPermission() = shizuku.requestPermission()

    fun consumeMessage() = _ui.update { it.copy(message = null) }
    fun notify(message: Message) = _ui.update { it.copy(message = message) }
    fun dismissReport() = _ui.update { it.copy(report = null) }
    fun dismissOpsReport() = _ui.update { it.copy(opsReport = null) }

    // ---- selection

    fun toggle(pkg: String) = _ui.update {
        it.copy(selected = if (pkg in it.selected) it.selected - pkg else it.selected + pkg)
    }

    fun selectAllActive(pkgs: List<String>) = _ui.update { s ->
        s.copy(selected = s.selected + pkgs.filter { p -> s.rows[p]?.bulkSelectable == true })
    }

    fun clearSelection() = _ui.update { it.copy(selected = emptySet()) }
    fun deselect(pkgs: Collection<String>) = _ui.update { it.copy(selected = it.selected - pkgs.toSet()) }

    /** Everything from the given lists that is still active and may be frozen in a batch. */
    fun bulkCandidates(safe: Boolean, optional: Boolean, custom: Boolean): List<String> {
        val s = _ui.value
        val ids = LinkedHashSet<String>()
        if (safe) ids += safeIds
        if (optional) ids += optionalIds
        if (custom) ids += s.custom
        return ids.filter { s.rows[it]?.bulkSelectable == true }
    }

    // ---- actions that need the shell

    fun freeze(pkgs: Collection<String>) = batch(BatchKind.FREEZE, pkgs.toList()) { engine.freeze(it) }
    fun remove(pkgs: Collection<String>) = batch(BatchKind.REMOVE, pkgs.toList()) { engine.remove(it) }
    fun thaw(pkg: String) = batch(BatchKind.RESET, listOf(pkg), silent = true) { engine.thaw(it) }
    fun restore(pkg: String) = batch(BatchKind.RESET, listOf(pkg), silent = true) { engine.restore(it) }

    fun resetAll() {
        val all = (store.removed + store.frozen).toList()
        batch(BatchKind.RESET, all) { pkg ->
            if (pkg in store.removed) engine.restore(pkg).also { if (it.outcome == Outcome.NOT_PRESENT) store.unmarkRemoved(pkg) }
            else engine.thaw(pkg).also { if (it.outcome == Outcome.NOT_PRESENT) store.unmarkFrozen(pkg) }
        }
    }

    /** Freezes or removes again whatever an update brought back. */
    fun reapplyDrift() {
        val removedAgain = store.removed
        batch(BatchKind.REAPPLY, _ui.value.drift.map { it.pkg }) { pkg ->
            if (pkg in removedAgain) engine.remove(pkg) else {
                // The journal still lists it as frozen; freeze() handles the "active again" case.
                engine.freeze(pkg)
            }
        }
    }

    private fun batch(
        kind: BatchKind,
        pkgs: List<String>,
        silent: Boolean = false,
        action: suspend (String) -> ActionResult,
    ) {
        if (pkgs.isEmpty()) {
            notify(Message.NOTHING_SELECTED)
            return
        }
        if (!shizuku.isReady) {
            notify(Message.NEEDS_SHELL)
            return
        }
        if (_ui.value.progress != null) return
        viewModelScope.launch {
            val results = mutableListOf<ActionResult>()
            try {
                pkgs.forEachIndexed { i, pkg ->
                    _ui.update { it.copy(progress = Progress(kind, i, pkgs.size, pkg)) }
                    val r = withContext(Dispatchers.IO) { action(pkg) }
                    results += r
                    if (r.changedSomething || r.isFailure) log(r)
                }
            } catch (_: ShellUnavailableException) {
                notify(Message.CONNECTION_LOST)
            }
            _ui.update {
                it.copy(
                    progress = null,
                    selected = it.selected - pkgs.toSet(),
                    report = if (silent && results.none { r -> r.isFailure }) it.report else Report(kind, results),
                )
            }
            refresh()
            if (_ui.value.ops != null) loadOps()
        }
    }

    private fun log(r: ActionResult) {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val extra = if (r.phoneSays.isNotEmpty()) "  (${r.phoneSays})" else ""
        store.appendLog("$time  ${r.outcome}  ${r.pkg}$extra")
    }

    fun clearLog() {
        store.clearLog()
        _ui.update { it.copy(log = emptyList()) }
    }

    // ---- app-ops (telemetry lock)

    fun loadOps() {
        if (!shizuku.isReady || _ui.value.opsBusy) return
        viewModelScope.launch {
            _ui.update { it.copy(opsBusy = true) }
            val list = try {
                withContext(Dispatchers.IO) {
                    catalog.appOpsPackages.flatMap { pkg -> catalog.appOps.map { op -> engine.readOp(pkg, op) } }
                }
            } catch (_: ShellUnavailableException) {
                null
            }
            _ui.update { it.copy(ops = list ?: it.ops, opsBusy = false) }
        }
    }

    fun setOps(block: Boolean) {
        if (!shizuku.isReady) {
            notify(Message.NEEDS_SHELL)
            return
        }
        if (_ui.value.opsBusy) return
        viewModelScope.launch {
            _ui.update { it.copy(opsBusy = true) }
            val results = try {
                withContext(Dispatchers.IO) {
                    catalog.appOpsPackages.flatMap { pkg -> catalog.appOps.map { op -> engine.setOp(pkg, op, block) } }
                }
            } catch (_: ShellUnavailableException) {
                notify(Message.CONNECTION_LOST)
                null
            }
            _ui.update { it.copy(opsBusy = false, opsReport = results?.let { r -> OpsReport(block, r) }) }
            loadOps()
        }
    }

    // ---- custom list

    fun loadPicker() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val protectedPkgs = catalog.protectedPackages
                val own = getApplication<Application>().packageName
                packages.allApps()
                    .filter { it.packageName !in protectedPkgs && it.packageName != own && isValidPackageName(it.packageName) }
                    .map {
                        PickerApp(
                            pkg = it.packageName,
                            label = packages.labelOf(it),
                            isSystem = it.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                            state = packages.stateOf(it.packageName),
                        )
                    }
                    .sortedBy { it.label.lowercase() }
            }
            _ui.update { it.copy(picker = apps) }
        }
    }

    fun closePicker() = _ui.update { it.copy(picker = null) }

    fun addCustom(pkgs: Collection<String>) {
        val protectedPkgs = catalog.protectedPackages
        store.addCustom(pkgs.filter { isValidPackageName(it) && it !in protectedPkgs })
        refresh()
    }

    fun removeCustom(pkg: String) {
        store.removeCustom(pkg)
        _ui.update { it.copy(selected = it.selected - pkg) }
        refresh()
    }

    fun exportCustom(): String = CustomList.encode(store.custom)

    fun importCustom(text: String) {
        val names = try {
            CustomList.decode(text)
        } catch (_: Exception) {
            emptyList()
        }
        if (names.isEmpty()) {
            notify(Message.IMPORT_FAILED)
        } else {
            addCustom(names)
            notify(Message.IMPORTED)
        }
    }
}
