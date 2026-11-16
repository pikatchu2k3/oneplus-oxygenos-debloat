package io.github.pikatchu2k3.oneplusdebloat

import io.github.pikatchu2k3.oneplusdebloat.core.Catalog
import io.github.pikatchu2k3.oneplusdebloat.core.CustomList
import io.github.pikatchu2k3.oneplusdebloat.core.ListKind
import io.github.pikatchu2k3.oneplusdebloat.core.isValidPackageName
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {
    private val catalog = Catalog.parse(File("src/main/assets/catalog.json").readText())

    /** The Windows script is the source of truth for the lists; the app must not drift from it. */
    @Test
    fun listsMatchTheWindowsScript() {
        val bat = File("../../oneplus_cleanup.bat").readText().replace("\r\n", "\n")
        fun list(name: String): List<String> =
            Regex("^set \"$name=([^\"]*)\"$", RegexOption.MULTILINE).find(bat)!!.groupValues[1].split(" ")

        assertEquals(list("SAFE_LIST"), catalog.ofKind(ListKind.SAFE).map { it.id })
        assertEquals(list("OPT_LIST"), catalog.ofKind(ListKind.OPTIONAL).map { it.id })
        assertEquals(list("NEVER_LIST"), catalog.ofKind(ListKind.NEVER).map { it.id })
        assertEquals(list("CRITICAL"), catalog.critical)
        assertEquals(list("APPOPS_LIST"), catalog.appOpsPackages)
    }

    @Test
    fun everyEntryIsValidAndExplainedInBothLanguages() {
        for (e in catalog.packages) {
            assertTrue(e.id, isValidPackageName(e.id))
            assertTrue(e.id, e.name.isNotBlank() && e.de.isNotBlank() && e.en.isNotBlank())
        }
        assertEquals(catalog.packages.size, catalog.packages.map { it.id }.toSet().size)
    }

    @Test
    fun onlyTheSystemBaseIsLocked() {
        assertTrue("com.android.systemui" in catalog.protectedPackages)
        assertTrue("com.oplus.sauhelper" in catalog.protectedPackages)
        assertTrue("com.google.android.overlay.modules.healthfitness.forframework" in catalog.protectedPackages)
        // Replaceable core apps are guarded by the default-app check instead of a hard lock.
        assertTrue("com.google.android.dialer" !in catalog.protectedPackages)
        assertTrue("com.google.android.inputmethod.latin" !in catalog.protectedPackages)
        assertTrue("com.heytap.browser" !in catalog.protectedPackages)
    }

    @Test
    fun customListRoundTripsAndDropsGarbage() {
        val text = CustomList.encode(listOf("com.b.app", "com.a.app"))
        assertEquals(listOf("com.a.app", "com.b.app"), CustomList.decode(text))
        val plain = "# my list\ncom.a.app\n\ncom.b.app  # comment\nnot a package; rm -rf /\ncom.a.app\n"
        assertEquals(listOf("com.a.app", "com.b.app"), CustomList.decode(plain))
    }
}
