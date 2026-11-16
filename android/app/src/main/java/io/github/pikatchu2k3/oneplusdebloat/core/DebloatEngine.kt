package io.github.pikatchu2k3.oneplusdebloat.core

/** Remembers what this app changed, so that "reset" and the status check know what to look at. */
interface Journal {
    val frozen: Set<String>
    val removed: Set<String>
    fun markFrozen(pkg: String)
    fun unmarkFrozen(pkg: String)
    fun markRemoved(pkg: String)
    fun unmarkRemoved(pkg: String)
}

enum class Outcome {
    FROZEN,
    ALREADY_FROZEN,

    /** The phone accepted `pm disable-user` but left the package enabled. Removing is the way out. */
    FREEZE_IGNORED,
    THAWED,
    ALREADY_ACTIVE,
    THAW_FAILED,
    REMOVED,
    REMOVE_FAILED,
    RESTORED,
    RESTORE_FAILED,
    NOT_PRESENT,

    /** System base or locked system component: freezing and removing are refused. */
    PROTECTED,

    /** Currently the phone's default dialer, SMS app, keyboard or launcher: refused until replaced. */
    DEFAULT_APP,
    INVALID_NAME,
}

data class ActionResult(val pkg: String, val outcome: Outcome, val phoneSays: String = "") {
    val changedSomething: Boolean
        get() = outcome in setOf(Outcome.FROZEN, Outcome.THAWED, Outcome.REMOVED, Outcome.RESTORED)
    val isFailure: Boolean
        get() = outcome in setOf(
            Outcome.FREEZE_IGNORED, Outcome.THAW_FAILED, Outcome.REMOVE_FAILED,
            Outcome.RESTORE_FAILED, Outcome.INVALID_NAME,
        )
}

enum class OpState { BLOCKED, OPEN, PACKAGE_FROZEN, NOT_PRESENT }

data class OpStatus(val pkg: String, val op: String, val state: OpState, val phoneSays: String = "")

enum class OpOutcome {
    BLOCKED,
    RESET,

    /** Package is frozen or removed: it cannot run, so blocking is pointless. */
    SKIPPED_NOT_RUNNING,
    NOT_PRESENT,

    /** The phone accepted `appops set` but did not store it (seen on OnePlus system packages). */
    REFUSED,
}

data class OpResult(val pkg: String, val op: String, val outcome: OpOutcome, val phoneSays: String = "")

enum class Drift {
    /** Frozen by this app, enabled again now (typically after a system update). */
    ACTIVE_AGAIN,

    /** Removed by this app, installed again now. */
    BACK_AGAIN,
}

data class DriftEntry(val pkg: String, val drift: Drift)

/**
 * The same steps the Windows script performs, one package at a time.
 * Every change is verified by reading the package state back instead of trusting the command output.
 */
class DebloatEngine(
    private val shell: ShellExecutor,
    private val packages: PackageSource,
    private val journal: Journal,
    private val protectedPackages: Set<String>,
    private val userId: Int = 0,
    /** True while the package is the phone's default for a job it cannot do without. */
    private val isDefaultApp: (String) -> Boolean = { false },
) {
    suspend fun freeze(pkg: String): ActionResult {
        precheck(pkg, changing = true)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT, PkgState.REMOVED -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.FROZEN -> {
                // Count it as ours from now on, so reset and the status check cover it.
                journal.markFrozen(pkg)
                return ActionResult(pkg, Outcome.ALREADY_FROZEN)
            }
            PkgState.ACTIVE -> Unit
        }
        val result = shell.exec("pm disable-user --user $userId $pkg")
        return if (packages.stateOf(pkg) == PkgState.FROZEN) {
            journal.markFrozen(pkg)
            ActionResult(pkg, Outcome.FROZEN)
        } else {
            ActionResult(pkg, Outcome.FREEZE_IGNORED, result.firstLine)
        }
    }

    /** Works for protected packages too: enabling something is always allowed. */
    suspend fun thaw(pkg: String): ActionResult {
        precheck(pkg, changing = false)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT, PkgState.REMOVED -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.ACTIVE -> {
                journal.unmarkFrozen(pkg)
                return ActionResult(pkg, Outcome.ALREADY_ACTIVE)
            }
            PkgState.FROZEN -> Unit
        }
        val result = shell.exec("pm enable --user $userId $pkg")
        return if (packages.stateOf(pkg) == PkgState.ACTIVE) {
            journal.unmarkFrozen(pkg)
            ActionResult(pkg, Outcome.THAWED)
        } else {
            ActionResult(pkg, Outcome.THAW_FAILED, result.firstLine)
        }
    }

    /**
     * Removes the package for the current user only (`pm uninstall -k --user`), the way Canta does.
     * The copy in the system image and the app data stay; [restore] brings it back.
     */
    suspend fun remove(pkg: String): ActionResult {
        precheck(pkg, changing = true)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.REMOVED -> {
                journal.markRemoved(pkg)
                return ActionResult(pkg, Outcome.REMOVED)
            }
            PkgState.ACTIVE, PkgState.FROZEN -> Unit
        }
        val result = shell.exec("pm uninstall -k --user $userId $pkg")
        return if (packages.stateOf(pkg) == PkgState.REMOVED) {
            journal.unmarkFrozen(pkg)
            journal.markRemoved(pkg)
            ActionResult(pkg, Outcome.REMOVED)
        } else {
            ActionResult(pkg, Outcome.REMOVE_FAILED, result.firstLine)
        }
    }

    suspend fun restore(pkg: String): ActionResult {
        precheck(pkg, changing = false)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.ACTIVE, PkgState.FROZEN -> {
                journal.unmarkRemoved(pkg)
                return ActionResult(pkg, Outcome.ALREADY_ACTIVE)
            }
            PkgState.REMOVED -> Unit
        }
        val result = shell.exec("pm install-existing --user $userId $pkg")
        return if (packages.stateOf(pkg).let { it == PkgState.ACTIVE || it == PkgState.FROZEN }) {
            journal.unmarkRemoved(pkg)
            ActionResult(pkg, Outcome.RESTORED)
        } else {
            ActionResult(pkg, Outcome.RESTORE_FAILED, result.firstLine)
        }
    }

    /** Undoes everything recorded in the journal. Entries that fail stay in the journal. */
    suspend fun resetAll(onProgress: (ActionResult) -> Unit = {}): List<ActionResult> {
        val results = mutableListOf<ActionResult>()
        for (pkg in journal.removed.toList()) {
            results += restore(pkg).also {
                if (it.outcome == Outcome.NOT_PRESENT) journal.unmarkRemoved(pkg)
                onProgress(it)
            }
        }
        for (pkg in journal.frozen.toList()) {
            results += thaw(pkg).also {
                if (it.outcome == Outcome.NOT_PRESENT) journal.unmarkFrozen(pkg)
                onProgress(it)
            }
        }
        return results
    }

    /** What changed behind our back, e.g. after a system update. Needs no shell access. */
    fun drift(): List<DriftEntry> {
        val out = mutableListOf<DriftEntry>()
        for (pkg in journal.frozen) {
            if (packages.stateOf(pkg) == PkgState.ACTIVE) out += DriftEntry(pkg, Drift.ACTIVE_AGAIN)
        }
        for (pkg in journal.removed) {
            val s = packages.stateOf(pkg)
            if (s == PkgState.ACTIVE || s == PkgState.FROZEN) out += DriftEntry(pkg, Drift.BACK_AGAIN)
        }
        return out.sortedBy { it.pkg }
    }

    suspend fun readOp(pkg: String, op: String): OpStatus {
        if (!isValidPackageName(pkg) || !isValidOpName(op)) return OpStatus(pkg, op, OpState.NOT_PRESENT)
        return when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> OpStatus(pkg, op, OpState.NOT_PRESENT)
            PkgState.FROZEN, PkgState.REMOVED -> OpStatus(pkg, op, OpState.PACKAGE_FROZEN)
            PkgState.ACTIVE -> {
                val raw = shell.exec("cmd appops get $pkg $op").output
                if (isOpBlocked(raw)) OpStatus(pkg, op, OpState.BLOCKED)
                else OpStatus(pkg, op, OpState.OPEN, flatten(raw))
            }
        }
    }

    /**
     * Blocks (`ignore`) or resets (`default`) one app-op and reads it back.
     * If the package-level mode does not stick, the uid-level mode is tried as well.
     */
    suspend fun setOp(pkg: String, op: String, block: Boolean): OpResult {
        if (!isValidPackageName(pkg) || !isValidOpName(op)) return OpResult(pkg, op, OpOutcome.NOT_PRESENT)
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> return OpResult(pkg, op, OpOutcome.NOT_PRESENT)
            PkgState.FROZEN, PkgState.REMOVED ->
                if (block) return OpResult(pkg, op, OpOutcome.SKIPPED_NOT_RUNNING)
            PkgState.ACTIVE -> Unit
        }
        if (block) {
            shell.exec("cmd appops set $pkg $op ignore")
            var raw = shell.exec("cmd appops get $pkg $op").output
            if (!isOpBlocked(raw)) {
                shell.exec("cmd appops set --uid $pkg $op ignore")
                raw = shell.exec("cmd appops get $pkg $op").output
            }
            return if (isOpBlocked(raw)) OpResult(pkg, op, OpOutcome.BLOCKED)
            else OpResult(pkg, op, OpOutcome.REFUSED, flatten(raw))
        }
        shell.exec("cmd appops set $pkg $op default")
        shell.exec("cmd appops set --uid $pkg $op default")
        val raw = shell.exec("cmd appops get $pkg $op").output
        return if (!isOpBlocked(raw)) OpResult(pkg, op, OpOutcome.RESET)
        else OpResult(pkg, op, OpOutcome.REFUSED, flatten(raw))
    }

    private fun precheck(pkg: String, changing: Boolean): ActionResult? = when {
        !isValidPackageName(pkg) -> ActionResult(pkg, Outcome.INVALID_NAME)
        changing && pkg in protectedPackages -> ActionResult(pkg, Outcome.PROTECTED)
        changing && isDefaultApp(pkg) -> ActionResult(pkg, Outcome.DEFAULT_APP)
        else -> null
    }

    companion object {
        private val OP_NAME = Regex("^[A-Z_]+$")
        fun isValidOpName(op: String) = OP_NAME.matches(op)

        /**
         * `cmd appops get` prints one line per level, e.g. "Uid mode: WAKE_LOCK: allow" and
         * "WAKE_LOCK: ignore; time=...". The op is blocked as soon as one level says ignore or deny.
         */
        fun isOpBlocked(output: String): Boolean =
            output.lineSequence().any { it.contains(": ignore") || it.contains(": deny") }

        private fun flatten(output: String): String =
            output.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")
    }
}
