package io.github.pikatchu2k3.oneplusdebloat

import io.github.pikatchu2k3.oneplusdebloat.core.DebloatEngine
import io.github.pikatchu2k3.oneplusdebloat.core.Drift
import io.github.pikatchu2k3.oneplusdebloat.core.Journal
import io.github.pikatchu2k3.oneplusdebloat.core.OpOutcome
import io.github.pikatchu2k3.oneplusdebloat.core.OpState
import io.github.pikatchu2k3.oneplusdebloat.core.Outcome
import io.github.pikatchu2k3.oneplusdebloat.core.PackageSource
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import io.github.pikatchu2k3.oneplusdebloat.core.ShellExecutor
import io.github.pikatchu2k3.oneplusdebloat.core.ShellResult
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
    override fun markFrozen(pkg: String) { frozen += pkg }
    override fun unmarkFrozen(pkg: String) { frozen -= pkg }
    override fun markRemoved(pkg: String) { removed += pkg }
    override fun unmarkRemoved(pkg: String) { removed -= pkg }
}

class DebloatEngineTest {
    private val phone = FakePhone()
    private val journal = MemoryJournal()
    private val defaults = mutableSetOf<String>()
    private val engine = DebloatEngine(
        phone, phone, journal, setOf("com.android.systemui"), userId = 0,
        isDefaultApp = { it in defaults },
    )

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
}
