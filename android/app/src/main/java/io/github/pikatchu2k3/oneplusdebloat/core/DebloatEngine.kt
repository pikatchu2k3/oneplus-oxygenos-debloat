package io.github.pikatchu2k3.oneplusdebloat.core

/** Remembers what this app changed, so that "reset" and the status check know what to look at. */
interface Journal {
    val frozen: Set<String>
    val removed: Set<String>

    /** Hidden by this app as device owner. */
    val hidden: Set<String>

    /** Suspended and background-restricted by this app ("neutralized"). */
    val neutralized: Set<String>
    fun markFrozen(pkg: String)
    fun unmarkFrozen(pkg: String)
    fun markRemoved(pkg: String)
    fun unmarkRemoved(pkg: String)
    fun markHidden(pkg: String)
    fun unmarkHidden(pkg: String)
    fun markNeutralized(pkg: String)
    fun unmarkNeutralized(pkg: String)
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

    /** Hidden through the device owner (step 2 of the "other ways"). */
    HIDDEN,

    /** Suspended and background-restricted (step 3 of the "other ways", the weakest one). */
    NEUTRALIZED,

    /** None of the "other ways" worked; [ActionResult.attempts] has the phone's answers. */
    NOTHING_WORKED,
    NOT_PRESENT,

    /** System base or locked system component: freezing and removing are refused. */
    PROTECTED,

    /** Currently the phone's default dialer, SMS app, keyboard or launcher: refused until replaced. */
    DEFAULT_APP,
    INVALID_NAME,
}

/** The "other ways" for apps the vendor will not let go, strongest first. */
enum class Way {
    /** `pm uninstall -k --user` - gone for the user, copy in the system and data stay. */
    REMOVE,

    /** Device owner hides the app - a different code path that vendors often leave open. */
    HIDE,

    /** `pm suspend` + force-stop + no background + restricted standby bucket. */
    NEUTRALIZE,
}

/** One step of the "other ways": worked, skipped (device owner not set up) or the phone's answer. */
data class Attempt(val way: Way, val ok: Boolean, val skipped: Boolean = false, val phoneSays: String = "")

data class ActionResult(
    val pkg: String,
    val outcome: Outcome,
    val phoneSays: String = "",
    val attempts: List<Attempt> = emptyList(),
) {
    val changedSomething: Boolean
        get() = outcome in setOf(
            Outcome.FROZEN, Outcome.THAWED, Outcome.REMOVED, Outcome.RESTORED, Outcome.HIDDEN, Outcome.NEUTRALIZED,
        )
    val isFailure: Boolean
        get() = outcome in setOf(
            Outcome.FREEZE_IGNORED, Outcome.THAW_FAILED, Outcome.REMOVE_FAILED,
            Outcome.RESTORE_FAILED, Outcome.INVALID_NAME, Outcome.NOTHING_WORKED,
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
    /** Device owner rights, if the user set them up; null where the platform offers none. */
    private val owner: DeviceOwner? = null,
) {
    suspend fun freeze(pkg: String): ActionResult {
        precheck(pkg, changing = true)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT, PkgState.REMOVED, PkgState.HIDDEN -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.FROZEN -> {
                // Count it as ours from now on, so reset and the status check cover it.
                journal.markFrozen(pkg)
                return ActionResult(pkg, Outcome.ALREADY_FROZEN)
            }
            PkgState.ACTIVE, PkgState.SUSPENDED -> Unit
        }
        val result = shell.exec("pm disable-user --user $userId $pkg")
        return if (packages.stateOf(pkg) == PkgState.FROZEN) {
            journal.markFrozen(pkg)
            // Freezing is stronger than neutralizing; drop the weaker stand-in so nothing is left behind.
            if (pkg in journal.neutralized) liftNeutralize(pkg)
            ActionResult(pkg, Outcome.FROZEN)
        } else {
            ActionResult(pkg, Outcome.FREEZE_IGNORED, result.firstLine)
        }
    }

    /** Works for protected packages too: enabling something is always allowed. */
    suspend fun thaw(pkg: String): ActionResult {
        precheck(pkg, changing = false)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT, PkgState.REMOVED, PkgState.HIDDEN -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.ACTIVE, PkgState.SUSPENDED -> {
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
            PkgState.HIDDEN -> return ActionResult(pkg, Outcome.HIDDEN)
            PkgState.ACTIVE, PkgState.FROZEN, PkgState.SUSPENDED -> Unit
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
            PkgState.ACTIVE, PkgState.FROZEN, PkgState.SUSPENDED, PkgState.HIDDEN -> {
                journal.unmarkRemoved(pkg)
                return ActionResult(pkg, Outcome.ALREADY_ACTIVE)
            }
            PkgState.REMOVED -> Unit
        }
        val result = shell.exec("pm install-existing --user $userId $pkg")
        return if (packages.stateOf(pkg).let { it != PkgState.REMOVED && it != PkgState.NOT_PRESENT }) {
            journal.unmarkRemoved(pkg)
            ActionResult(pkg, Outcome.RESTORED)
        } else {
            ActionResult(pkg, Outcome.RESTORE_FAILED, result.firstLine)
        }
    }

    /**
     * The "other ways" for an app the vendor will not let freeze: tries [Way.REMOVE], then [Way.HIDE]
     * (only if this app is device owner) and finally [Way.NEUTRALIZE], reading the state back after
     * each step and stopping at the first one that works. Every step can be undone with [undo].
     *
     * Deliberately left out: `pm disable-until-used`, `pm hide` and disabling single components.
     * AOSP refuses all three to the shell user (only ENABLED / DISABLED_USER for whole packages,
     * MANAGE_USERS for hide), so through Shizuku-as-adb they can never work.
     */
    suspend fun switchOffHard(pkg: String): ActionResult {
        precheck(pkg, changing = true)?.let { return it }
        when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> return ActionResult(pkg, Outcome.NOT_PRESENT)
            PkgState.REMOVED -> return ActionResult(pkg, Outcome.REMOVED)
            PkgState.HIDDEN -> return ActionResult(pkg, Outcome.HIDDEN)
            PkgState.FROZEN -> return ActionResult(pkg, Outcome.ALREADY_FROZEN)
            PkgState.ACTIVE, PkgState.SUSPENDED -> Unit
        }
        // A neutralized app gets a fresh start, so a stronger way does not leave our suspension behind.
        if (pkg in journal.neutralized) liftNeutralize(pkg)

        val attempts = mutableListOf<Attempt>()
        val removed = remove(pkg)
        attempts += Attempt(Way.REMOVE, removed.outcome == Outcome.REMOVED, phoneSays = removed.phoneSays)
        if (removed.outcome == Outcome.REMOVED) return ActionResult(pkg, Outcome.REMOVED, attempts = attempts)

        val hidden = hide(pkg)
        attempts += hidden
        if (hidden.ok) return ActionResult(pkg, Outcome.HIDDEN, attempts = attempts)

        val neutralized = neutralize(pkg)
        attempts += neutralized
        if (neutralized.ok) return ActionResult(pkg, Outcome.NEUTRALIZED, attempts = attempts)

        return ActionResult(
            pkg, Outcome.NOTHING_WORKED,
            phoneSays = attempts.filter { !it.skipped && it.phoneSays.isNotEmpty() }.joinToString(" | ") { it.phoneSays },
            attempts = attempts,
        )
    }

    /** Device owner hides the package; skipped when this app is not device owner. */
    suspend fun hide(pkg: String): Attempt {
        if (!isValidPackageName(pkg) || pkg in protectedPackages || isDefaultApp(pkg)) return Attempt(Way.HIDE, false, skipped = true)
        val o = owner
        if (o == null || !o.isOwner) return Attempt(Way.HIDE, false, skipped = true)
        val error = o.setHidden(pkg, true)
        return if (packages.stateOf(pkg) == PkgState.HIDDEN) {
            journal.markHidden(pkg)
            journal.unmarkFrozen(pkg)
            Attempt(Way.HIDE, true)
        } else {
            Attempt(Way.HIDE, false, phoneSays = error.orEmpty())
        }
    }

    /**
     * Suspend, stop, forbid background work, restricted standby bucket. Weaker than freezing: the app
     * cannot be opened and is kept from running on its own, but the system can still bind to it.
     * App-ops are set per package only - never per uid, because many vendor apps share the system uid.
     */
    suspend fun neutralize(pkg: String): Attempt {
        if (!isValidPackageName(pkg) || pkg in protectedPackages || isDefaultApp(pkg)) return Attempt(Way.NEUTRALIZE, false, skipped = true)
        val result = shell.exec("pm suspend --user $userId $pkg")
        if (packages.stateOf(pkg) != PkgState.SUSPENDED) {
            return Attempt(Way.NEUTRALIZE, false, phoneSays = result.firstLine)
        }
        shell.exec("am force-stop --user $userId $pkg")
        for (op in NEUTRALIZE_OPS) shell.exec("cmd appops set $pkg $op ignore")
        shell.exec("am set-standby-bucket --user $userId $pkg restricted")
        journal.markNeutralized(pkg)
        return Attempt(Way.NEUTRALIZE, true)
    }

    /** Brings the package back to normal, whichever way it was switched off. */
    suspend fun undo(pkg: String): ActionResult {
        precheck(pkg, changing = false)?.let { return it }
        val result = when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> {
                forget(pkg)
                return ActionResult(pkg, Outcome.NOT_PRESENT)
            }
            PkgState.ACTIVE -> {
                if (pkg in journal.neutralized) liftNeutralize(pkg)
                forget(pkg)
                return ActionResult(pkg, Outcome.ALREADY_ACTIVE)
            }
            PkgState.FROZEN -> thaw(pkg)
            PkgState.REMOVED -> restore(pkg)
            PkgState.HIDDEN -> unhide(pkg)
            PkgState.SUSPENDED -> {
                val res = liftNeutralize(pkg)
                if (packages.stateOf(pkg) == PkgState.SUSPENDED) ActionResult(pkg, Outcome.THAW_FAILED, res)
                else ActionResult(pkg, Outcome.THAWED)
            }
        }
        // Whatever came back may still carry our background restrictions from an earlier step.
        if (!result.isFailure && pkg in journal.neutralized) liftNeutralize(pkg)
        return result
    }

    private suspend fun unhide(pkg: String): ActionResult {
        val o = owner
        val error = if (o != null && o.isOwner) {
            o.setHidden(pkg, false)
        } else {
            // Without device owner only root can unhide; worth a try if Shizuku runs as root.
            shell.exec("pm unhide --user $userId $pkg").firstLine
        }
        return if (packages.stateOf(pkg) != PkgState.HIDDEN) {
            journal.unmarkHidden(pkg)
            ActionResult(pkg, Outcome.RESTORED)
        } else {
            ActionResult(pkg, Outcome.RESTORE_FAILED, error.orEmpty())
        }
    }

    /**
     * Undoes [neutralize]. App-ops and standby bucket are only reset if this app set them; a package
     * suspended by someone else (e.g. Digital Wellbeing) is only unsuspended.
     * Returns the phone's answer to `pm unsuspend`.
     */
    private suspend fun liftNeutralize(pkg: String): String {
        var says = ""
        if (packages.stateOf(pkg) == PkgState.SUSPENDED) {
            says = shell.exec("pm unsuspend --user $userId $pkg").firstLine
        }
        if (pkg in journal.neutralized) {
            for (op in NEUTRALIZE_OPS) shell.exec("cmd appops set $pkg $op default")
            shell.exec("am set-standby-bucket --user $userId $pkg active")
            if (packages.stateOf(pkg) != PkgState.SUSPENDED) journal.unmarkNeutralized(pkg)
        }
        return says
    }

    private fun forget(pkg: String) {
        journal.unmarkFrozen(pkg)
        journal.unmarkRemoved(pkg)
        journal.unmarkHidden(pkg)
        journal.unmarkNeutralized(pkg)
    }

    /** Everything this app switched off, in the order it is best undone. */
    val journalAll: List<String>
        get() = (journal.removed + journal.hidden + journal.neutralized + journal.frozen).toList()

    /** Undoes everything recorded in the journal. Entries that fail stay in the journal. */
    suspend fun resetAll(onProgress: (ActionResult) -> Unit = {}): List<ActionResult> =
        journalAll.map { pkg -> undo(pkg).also(onProgress) }

    /** Switches a package off again the same way as before, e.g. after an update brought it back. */
    suspend fun reapply(pkg: String): ActionResult = when (pkg) {
        in journal.removed -> remove(pkg)
        in journal.hidden -> {
            val a = hide(pkg)
            if (a.ok) ActionResult(pkg, Outcome.HIDDEN) else switchOffHard(pkg)
        }
        in journal.neutralized -> {
            val a = neutralize(pkg)
            ActionResult(pkg, if (a.ok) Outcome.NEUTRALIZED else Outcome.NOTHING_WORKED, a.phoneSays)
        }
        else -> freeze(pkg)
    }

    /** What changed behind our back, e.g. after a system update. Needs no shell access. */
    fun drift(): List<DriftEntry> {
        val out = LinkedHashMap<String, DriftEntry>()
        val back = setOf(PkgState.ACTIVE, PkgState.FROZEN, PkgState.SUSPENDED)
        for (pkg in journal.removed + journal.hidden) {
            if (packages.stateOf(pkg) in back) out[pkg] = DriftEntry(pkg, Drift.BACK_AGAIN)
        }
        for (pkg in journal.frozen + journal.neutralized) {
            if (pkg !in out && packages.stateOf(pkg) == PkgState.ACTIVE) out[pkg] = DriftEntry(pkg, Drift.ACTIVE_AGAIN)
        }
        return out.values.sortedBy { it.pkg }
    }

    suspend fun readOp(pkg: String, op: String): OpStatus {
        if (!isValidPackageName(pkg) || !isValidOpName(op)) return OpStatus(pkg, op, OpState.NOT_PRESENT)
        return when (packages.stateOf(pkg)) {
            PkgState.NOT_PRESENT -> OpStatus(pkg, op, OpState.NOT_PRESENT)
            PkgState.FROZEN, PkgState.REMOVED, PkgState.HIDDEN -> OpStatus(pkg, op, OpState.PACKAGE_FROZEN)
            PkgState.ACTIVE, PkgState.SUSPENDED -> {
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
            PkgState.FROZEN, PkgState.REMOVED, PkgState.HIDDEN ->
                if (block) return OpResult(pkg, op, OpOutcome.SKIPPED_NOT_RUNNING)
            PkgState.ACTIVE, PkgState.SUSPENDED -> Unit
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
        /** Background app-ops denied while an app is neutralized. */
        val NEUTRALIZE_OPS = listOf("RUN_ANY_IN_BACKGROUND", "RUN_IN_BACKGROUND")

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
