package io.github.pikatchu2k3.oneplusdebloat

import io.github.pikatchu2k3.oneplusdebloat.core.DebloatEngine
import io.github.pikatchu2k3.oneplusdebloat.core.DeviceOwner
import io.github.pikatchu2k3.oneplusdebloat.core.Drift
import io.github.pikatchu2k3.oneplusdebloat.core.Journal
import io.github.pikatchu2k3.oneplusdebloat.core.OpOutcome
import io.github.pikatchu2k3.oneplusdebloat.core.OpState
import io.github.pikatchu2k3.oneplusdebloat.core.Outcome
import io.github.pikatchu2k3.oneplusdebloat.core.OwnerSetup
import io.github.pikatchu2k3.oneplusdebloat.core.PackageSource
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import io.github.pikatchu2k3.oneplusdebloat.core.ShellExecutor
import io.github.pikatchu2k3.oneplusdebloat.core.ShellResult
import io.github.pikatchu2k3.oneplusdebloat.core.Way
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A phone that answers the way the real OnePlus devices in the bug reports did:
 * some packages ignore `pm disable-user` ("new state: default"), some ignore `appops set`
 * ("No operations."), and `appops get` prints a "Uid mode:" line before the package line.
 */
private class FakePhone : ShellExecutor, PackageSource {
    val states = mutableMapOf<String, PkgState>()
    val ignoresDisable = mutableSetOf<String>()
    val refusesUninstall = mutableSetOf<String>()
    val refusesAppOps = mutableSetOf<String>()
    val refusesSuspend = mutableSetOf<String>()
    val buckets = mutableMapOf<String, String>()
    var shellIsRoot = false
    /** Packages whose package-level mode is dropped, so only `--uid` sticks. */
    val uidOnlyAppOps = mutableSetOf<String>()
    val pkgOps = mutableMapOf<String, String>()
    val uidOps = mutableMapOf<String, String>()
    val commands = mutableListOf<String>()

    override val isReady = true
    override fun stateOf(pkg: String) = states[pkg] ?: PkgState.NOT_PRESENT

    override suspend fun exec(command: String): ShellResult {
        commands += command
        val w = command.split(" ")
        return when {
            command.startsWith("pm disable-user --user 0 ") -> {
                val pkg = w.last()
                if (pkg in ignoresDisable) ShellResult(0, "Package $pkg new state: default\n")
                else {
                    states[pkg] = PkgState.FROZEN
                    ShellResult(0, "Package $pkg new state: disabled-user\n")
                }
            }
            command.startsWith("pm enable --user 0 ") -> {
                states[w.last()] = PkgState.ACTIVE
                ShellResult(0, "Package ${w.last()} new state: enabled\n")
            }
            command.startsWith("pm uninstall -k --user 0 ") -> {
                val pkg = w.last()
                if (pkg in refusesUninstall) ShellResult(1, "Failure [DELETE_FAILED_INTERNAL_ERROR]\n")
                else {
                    states[pkg] = PkgState.REMOVED
                    ShellResult(0, "Success\n")
                }
            }
            command.startsWith("pm suspend --user 0 ") -> {
                val pkg = w.last()
                if (pkg in refusesSuspend) ShellResult(1, "Error: Could not suspend $pkg\n")
                else {
                    states[pkg] = PkgState.SUSPENDED
                    ShellResult(0, "Package $pkg new suspended state: true\n")
                }
            }
            command.startsWith("pm unsuspend --user 0 ") -> {
                states[w.last()] = PkgState.ACTIVE
                ShellResult(0, "Package ${w.last()} new suspended state: false\n")
            }
            command.startsWith("pm unhide --user 0 ") -> {
                if (!shellIsRoot) ShellResult(255, "Security exception: Neither user 2000 nor current process has android.permission.MANAGE_USERS.\n")
                else {
                    states[w.last()] = PkgState.ACTIVE
                    ShellResult(0, "Package ${w.last()} new hidden state: false\n")
                }
            }
            command.startsWith("am force-stop --user 0 ") -> ShellResult(0, "")
            command.startsWith("am set-standby-bucket --user 0 ") -> {
                buckets[w[4]] = w[5]
                ShellResult(0, "")
            }
            command.startsWith("pm install-existing --user 0 ") -> {
                states[w.last()] = PkgState.ACTIVE
                ShellResult(0, "Package ${w.last()} installed for user: 0\n")
            }
            command.startsWith("cmd appops set --uid ") -> {
                val (pkg, op, mode) = Triple(w[4], w[5], w[6])
                if (pkg !in refusesAppOps) {
                    if (mode == "default") uidOps.remove("$pkg $op") else uidOps["$pkg $op"] = mode
                }
                ShellResult(0, "")
            }
            command.startsWith("cmd appops set ") -> {
                val (pkg, op, mode) = Triple(w[3], w[4], w[5])
                if (pkg !in refusesAppOps && pkg !in uidOnlyAppOps) {
                    if (mode == "default") pkgOps.remove("$pkg $op") else pkgOps["$pkg $op"] = mode
                }
                ShellResult(0, "")
            }
            command.startsWith("cmd appops get ") -> {
                val key = "${w[3]} ${w[4]}"
                val lines = mutableListOf<String>()
                uidOps[key]?.let { lines += "Uid mode: ${w[4]}: $it" }
                pkgOps[key]?.let { lines += "${w[4]}: $it; time=+3m12s ago" }
                if (lines.isEmpty()) lines += listOf("No operations.", "Default mode: allow")
                ShellResult(0, lines.joinToString("\n") + "\n")
            }
            else -> error("unexpected command: $command")
        }
    }
}

private class MemoryJournal : Journal {
    override val frozen = mutableSetOf<String>()
    override val removed = mutableSetOf<String>()
    override val hidden = mutableSetOf<String>()
    override val neutralized = mutableSetOf<String>()
    override fun markFrozen(pkg: String) { frozen += pkg }
    override fun unmarkFrozen(pkg: String) { frozen -= pkg }
    override fun markRemoved(pkg: String) { removed += pkg }
    override fun unmarkRemoved(pkg: String) { removed -= pkg }
    override fun markHidden(pkg: String) { hidden += pkg }
    override fun unmarkHidden(pkg: String) { hidden -= pkg }
    override fun markNeutralized(pkg: String) { neutralized += pkg }
    override fun unmarkNeutralized(pkg: String) { neutralized -= pkg }
}

/** Device owner as the device policy API would act: hides and shows, or refuses. */
private class FakeOwner(private val phone: FakePhone) : DeviceOwner {
    override var isOwner = false
    val refuses = mutableSetOf<String>()
    override fun setHidden(pkg: String, hidden: Boolean): String? {
        if (pkg in refuses) return "Cannot hide package: $pkg"
        phone.states[pkg] = if (hidden) PkgState.HIDDEN else PkgState.ACTIVE
        return null
    }
}

class DebloatEngineTest {
    private val phone = FakePhone()
    private val journal = MemoryJournal()
    private val defaults = mutableSetOf<String>()
    private val owner = FakeOwner(phone)
    private val engine = DebloatEngine(
        phone, phone, journal, setOf("com.android.systemui"), userId = 0,
        isDefaultApp = { it in defaults },
        owner = owner,
    )

    /** OxygenOS on Android 17 as measured: freeze ignored, removal refused for system apps. */
    private fun lockedByVendor(pkg: String) {
        phone.states[pkg] = PkgState.ACTIVE
        phone.ignoresDisable += pkg
        phone.refusesUninstall += pkg
    }

    private val browser = "com.heytap.browser"
    private val egg = "com.android.egg"
    private val stats = "com.oplus.statistics.rom"
    private val wake = "WAKE_LOCK"

    @Test
    fun freezeDisablesAndRecords() = runTest {
        phone.states[egg] = PkgState.ACTIVE
        assertEquals(Outcome.FROZEN, engine.freeze(egg).outcome)
        assertEquals(PkgState.FROZEN, phone.stateOf(egg))
        assertTrue(egg in journal.frozen)
    }

    @Test
    fun freezeIgnoredByPhoneIsReportedWithThePhonesAnswer() = runTest {
        phone.states[browser] = PkgState.ACTIVE
        phone.ignoresDisable += browser
        val r = engine.freeze(browser)
        assertEquals(Outcome.FREEZE_IGNORED, r.outcome)
        assertEquals("Package com.heytap.browser new state: default", r.phoneSays)
        assertFalse(browser in journal.frozen)
    }

    @Test
    fun removeIsTheWayOutAndCanBeUndone() = runTest {
        phone.states[browser] = PkgState.ACTIVE
        phone.ignoresDisable += browser
        assertEquals(Outcome.REMOVED, engine.remove(browser).outcome)
        assertTrue(browser in journal.removed)
        assertEquals(Outcome.RESTORED, engine.restore(browser).outcome)
        assertEquals(PkgState.ACTIVE, phone.stateOf(browser))
        assertFalse(browser in journal.removed)
    }

    @Test
    fun refusedUninstallChangesNothing() = runTest {
        phone.states[browser] = PkgState.ACTIVE
        phone.refusesUninstall += browser
        val r = engine.remove(browser)
        assertEquals(Outcome.REMOVE_FAILED, r.outcome)
        assertEquals("Failure [DELETE_FAILED_INTERNAL_ERROR]", r.phoneSays)
        assertTrue(journal.removed.isEmpty())
    }

    @Test
    fun protectedPackagesAreNeverFrozenOrRemovedButCanBeReleased() = runTest {
        val systemUi = "com.android.systemui"
        phone.states[systemUi] = PkgState.ACTIVE
        assertEquals(Outcome.PROTECTED, engine.freeze(systemUi).outcome)
        assertEquals(Outcome.PROTECTED, engine.remove(systemUi).outcome)
        assertTrue(phone.commands.isEmpty())
        phone.states[systemUi] = PkgState.FROZEN
        assertEquals(Outcome.THAWED, engine.thaw(systemUi).outcome)
    }

    @Test
    fun theCurrentDefaultAppIsRefusedUntilItIsReplaced() = runTest {
        val keyboard = "com.google.android.inputmethod.latin"
        phone.states[keyboard] = PkgState.ACTIVE
        defaults += keyboard
        assertEquals(Outcome.DEFAULT_APP, engine.freeze(keyboard).outcome)
        assertEquals(Outcome.DEFAULT_APP, engine.remove(keyboard).outcome)
        assertTrue(phone.commands.isEmpty())
        defaults -= keyboard   // the user selected another keyboard
        assertEquals(Outcome.FROZEN, engine.freeze(keyboard).outcome)
    }

    @Test
    fun shellMetacharactersNeverReachTheShell() = runTest {
        for (bad in listOf("com.x; rm -rf /", "com.x && reboot", "com.x\$(id)", "com x", "nodots", "")) {
            assertEquals(Outcome.INVALID_NAME, engine.freeze(bad).outcome)
            assertEquals(Outcome.INVALID_NAME, engine.remove(bad).outcome)
        }
        assertTrue(phone.commands.isEmpty())
    }

    @Test
    fun missingPackagesAreSkippedWithoutCommands() = runTest {
        assertEquals(Outcome.NOT_PRESENT, engine.freeze(egg).outcome)
        assertEquals(Outcome.NOT_PRESENT, engine.remove(egg).outcome)
        assertTrue(phone.commands.isEmpty())
    }

    @Test
    fun resetAllBringsEverythingBack() = runTest {
        phone.states[egg] = PkgState.ACTIVE
        phone.states[browser] = PkgState.ACTIVE
        engine.freeze(egg)
        engine.remove(browser)
        val results = engine.resetAll()
        assertEquals(setOf(Outcome.RESTORED, Outcome.THAWED), results.map { it.outcome }.toSet())
        assertEquals(PkgState.ACTIVE, phone.stateOf(egg))
        assertEquals(PkgState.ACTIVE, phone.stateOf(browser))
        assertTrue(journal.frozen.isEmpty() && journal.removed.isEmpty())
    }

    @Test
    fun driftFindsWhatAnUpdateBroughtBack() = runTest {
        phone.states[egg] = PkgState.ACTIVE
        phone.states[browser] = PkgState.ACTIVE
        engine.freeze(egg)
        engine.remove(browser)
        assertTrue(engine.drift().isEmpty())
        phone.states[egg] = PkgState.ACTIVE      // update re-enabled it
        phone.states[browser] = PkgState.ACTIVE  // update reinstalled it
        assertEquals(
            listOf(egg to Drift.ACTIVE_AGAIN, browser to Drift.BACK_AGAIN),
            engine.drift().map { it.pkg to it.drift },
        )
    }

    @Test
    fun appOpIsBlockedEvenWhenTheUidLineComesFirst() = runTest {
        phone.states[stats] = PkgState.ACTIVE
        phone.uidOps["$stats $wake"] = "allow"
        assertEquals(OpOutcome.BLOCKED, engine.setOp(stats, wake, block = true).outcome)
        assertEquals(OpState.BLOCKED, engine.readOp(stats, wake).state)
    }

    @Test
    fun appOpFallsBackToUidMode() = runTest {
        phone.states[stats] = PkgState.ACTIVE
        phone.uidOnlyAppOps += stats
        assertEquals(OpOutcome.BLOCKED, engine.setOp(stats, wake, block = true).outcome)
        assertEquals("ignore", phone.uidOps["$stats $wake"])
        assertEquals(OpOutcome.RESET, engine.setOp(stats, wake, block = false).outcome)
        assertTrue(phone.uidOps.isEmpty())
    }

    @Test
    fun appOpRefusedByPhoneIsReported() = runTest {
        phone.states[stats] = PkgState.ACTIVE
        phone.refusesAppOps += stats
        val r = engine.setOp(stats, wake, block = true)
        assertEquals(OpOutcome.REFUSED, r.outcome)
        assertEquals("No operations. Default mode: allow", r.phoneSays)
        assertEquals(OpState.OPEN, engine.readOp(stats, wake).state)
    }

    @Test
    fun frozenPackagesNeedNoAppOpLock() = runTest {
        phone.states[stats] = PkgState.FROZEN
        assertEquals(OpOutcome.SKIPPED_NOT_RUNNING, engine.setOp(stats, wake, block = true).outcome)
        assertEquals(OpState.PACKAGE_FROZEN, engine.readOp(stats, wake).state)
        assertTrue(phone.commands.isEmpty())
    }

    // ---- other ways

    @Test
    fun otherWaysStopAtRemovingWhenThatWorks() = runTest {
        phone.states[browser] = PkgState.ACTIVE
        phone.ignoresDisable += browser
        val r = engine.switchOffHard(browser)
        assertEquals(Outcome.REMOVED, r.outcome)
        assertEquals(listOf(Way.REMOVE), r.attempts.map { it.way })
        assertFalse(phone.commands.any { it.startsWith("pm suspend") })
    }

    @Test
    fun withoutDeviceOwnerTheLockedAppIsNeutralized() = runTest {
        lockedByVendor(browser)
        val r = engine.switchOffHard(browser)
        assertEquals(Outcome.NEUTRALIZED, r.outcome)
        assertEquals(listOf(Way.REMOVE, Way.HIDE, Way.NEUTRALIZE), r.attempts.map { it.way })
        assertEquals("Failure [DELETE_FAILED_INTERNAL_ERROR]", r.attempts[0].phoneSays)
        assertTrue(r.attempts[1].skipped)
        assertEquals(PkgState.SUSPENDED, phone.stateOf(browser))
        assertEquals("ignore", phone.pkgOps["$browser RUN_ANY_IN_BACKGROUND"])
        assertEquals("ignore", phone.pkgOps["$browser RUN_IN_BACKGROUND"])
        assertEquals("restricted", phone.buckets[browser])
        assertTrue(browser in journal.neutralized)
        // Never per uid: many vendor apps share the system uid.
        assertFalse(phone.commands.any { it.startsWith("cmd appops set --uid") })
    }

    @Test
    fun deviceOwnerHidesBeforeFallingBackToNeutralize() = runTest {
        lockedByVendor(browser)
        owner.isOwner = true
        val r = engine.switchOffHard(browser)
        assertEquals(Outcome.HIDDEN, r.outcome)
        assertEquals(PkgState.HIDDEN, phone.stateOf(browser))
        assertTrue(browser in journal.hidden)
        assertFalse(phone.commands.any { it.startsWith("pm suspend") })
    }

    @Test
    fun refusedHideFallsBackToNeutralizeWithTheReason() = runTest {
        lockedByVendor(browser)
        owner.isOwner = true
        owner.refuses += browser
        val r = engine.switchOffHard(browser)
        assertEquals(Outcome.NEUTRALIZED, r.outcome)
        assertEquals("Cannot hide package: $browser", r.attempts[1].phoneSays)
    }

    @Test
    fun nothingWorkedKeepsEveryAnswer() = runTest {
        lockedByVendor(browser)
        phone.refusesSuspend += browser
        val r = engine.switchOffHard(browser)
        assertEquals(Outcome.NOTHING_WORKED, r.outcome)
        assertTrue(r.isFailure)
        assertEquals("Error: Could not suspend $browser", r.attempts[2].phoneSays)
        assertEquals(PkgState.ACTIVE, phone.stateOf(browser))
        assertTrue(journal.neutralized.isEmpty())
        assertTrue(phone.pkgOps.isEmpty())
    }

    @Test
    fun otherWaysRespectLocksAndDefaults() = runTest {
        val systemUi = "com.android.systemui"
        phone.states[systemUi] = PkgState.ACTIVE
        assertEquals(Outcome.PROTECTED, engine.switchOffHard(systemUi).outcome)
        val keyboard = "com.google.android.inputmethod.latin"
        phone.states[keyboard] = PkgState.ACTIVE
        defaults += keyboard
        assertEquals(Outcome.DEFAULT_APP, engine.switchOffHard(keyboard).outcome)
        assertTrue(phone.commands.isEmpty())
    }

    @Test
    fun undoLiftsTheNeutralizationCompletely() = runTest {
        lockedByVendor(browser)
        engine.switchOffHard(browser)
        assertEquals(Outcome.THAWED, engine.undo(browser).outcome)
        assertEquals(PkgState.ACTIVE, phone.stateOf(browser))
        assertTrue(phone.pkgOps.isEmpty())
        assertEquals("active", phone.buckets[browser])
        assertTrue(journal.neutralized.isEmpty())
    }

    @Test
    fun undoOfSomeoneElsesSuspensionLeavesTheirSettingsAlone() = runTest {
        phone.states[egg] = PkgState.SUSPENDED   // e.g. paused by Digital Wellbeing
        phone.pkgOps["$egg RUN_IN_BACKGROUND"] = "ignore"
        assertEquals(Outcome.THAWED, engine.undo(egg).outcome)
        assertEquals("ignore", phone.pkgOps["$egg RUN_IN_BACKGROUND"])
        assertFalse(phone.commands.any { it.startsWith("am set-standby-bucket") })
    }

    @Test
    fun undoUnhidesThroughTheDeviceOwner() = runTest {
        lockedByVendor(browser)
        owner.isOwner = true
        engine.switchOffHard(browser)
        assertEquals(Outcome.RESTORED, engine.undo(browser).outcome)
        assertEquals(PkgState.ACTIVE, phone.stateOf(browser))
        assertTrue(journal.hidden.isEmpty())
    }

    @Test
    fun withoutDeviceOwnerHiddenAppsNeedRoot() = runTest {
        phone.states[browser] = PkgState.HIDDEN
        journal.hidden += browser
        val r = engine.undo(browser)
        assertEquals(Outcome.RESTORE_FAILED, r.outcome)
        assertTrue(r.phoneSays.contains("MANAGE_USERS"))
        assertTrue(browser in journal.hidden)
        phone.shellIsRoot = true
        assertEquals(Outcome.RESTORED, engine.undo(browser).outcome)
    }

    @Test
    fun aRealFreezeReplacesTheNeutralization() = runTest {
        lockedByVendor(browser)
        engine.switchOffHard(browser)
        phone.ignoresDisable -= browser   // e.g. a later firmware lifts the lock
        assertEquals(Outcome.FROZEN, engine.freeze(browser).outcome)
        assertTrue(journal.neutralized.isEmpty())
        assertTrue(phone.pkgOps.isEmpty())
    }

    @Test
    fun resetAllCoversEveryWay() = runTest {
        phone.states[egg] = PkgState.ACTIVE
        engine.freeze(egg)
        lockedByVendor(browser)
        engine.switchOffHard(browser)
        val stubborn = "com.oplus.games"
        lockedByVendor(stubborn)
        owner.isOwner = true
        engine.switchOffHard(stubborn)
        engine.resetAll()
        for (pkg in listOf(egg, browser, stubborn)) assertEquals(pkg, PkgState.ACTIVE, phone.stateOf(pkg))
        assertTrue(journal.frozen.isEmpty() && journal.hidden.isEmpty() && journal.neutralized.isEmpty())
    }

    @Test
    fun driftAndReapplyCoverHiddenAndNeutralized() = runTest {
        lockedByVendor(browser)
        engine.switchOffHard(browser)
        val stubborn = "com.oplus.games"
        lockedByVendor(stubborn)
        owner.isOwner = true
        engine.switchOffHard(stubborn)
        assertTrue(engine.drift().isEmpty())
        phone.states[browser] = PkgState.ACTIVE   // an update lifted the suspension
        phone.states[stubborn] = PkgState.ACTIVE  // ... and brought the hidden app back
        assertEquals(
            listOf(browser to Drift.ACTIVE_AGAIN, stubborn to Drift.BACK_AGAIN),
            engine.drift().map { it.pkg to it.drift },
        )
        assertEquals(Outcome.NEUTRALIZED, engine.reapply(browser).outcome)
        assertEquals(Outcome.HIDDEN, engine.reapply(stubborn).outcome)
        assertTrue(engine.drift().isEmpty())
    }

    @Test
    fun deviceOwnerAnswersAreSorted() {
        assertEquals(OwnerSetup.Result.OK, OwnerSetup.classify("Success: Device owner set to package ComponentInfo{x/y}\nActive admin set to component {x/y}"))
        assertEquals(
            OwnerSetup.Result.ACCOUNTS,
            OwnerSetup.classify("java.lang.IllegalStateException: Not allowed to set the device owner because there are already some accounts on the device."),
        )
        assertEquals(
            OwnerSetup.Result.USERS,
            OwnerSetup.classify("java.lang.IllegalStateException: Not allowed to set the device owner because there are already several users on the device."),
        )
        assertEquals(
            OwnerSetup.Result.OTHER_OWNER,
            OwnerSetup.classify("java.lang.IllegalStateException: Trying to set the device owner (x/y), but device owner (a/b) is already set."),
        )
        assertEquals(OwnerSetup.Result.OTHER, OwnerSetup.classify("java.lang.IllegalArgumentException: Unknown admin: ComponentInfo{x/y}"))
    }
}
