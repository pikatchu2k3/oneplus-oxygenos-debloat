package io.github.pikatchu2k3.oneplusdebloat

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureScreenRoboImage
import io.github.pikatchu2k3.oneplusdebloat.core.Drift
import io.github.pikatchu2k3.oneplusdebloat.core.DriftEntry
import io.github.pikatchu2k3.oneplusdebloat.core.OpState
import io.github.pikatchu2k3.oneplusdebloat.core.OpStatus
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuState
import io.github.pikatchu2k3.oneplusdebloat.ui.AppsScreen
import io.github.pikatchu2k3.oneplusdebloat.ui.CleanupTheme
import io.github.pikatchu2k3.oneplusdebloat.ui.DetailSheet
import io.github.pikatchu2k3.oneplusdebloat.ui.HelpScreen
import io.github.pikatchu2k3.oneplusdebloat.ui.HomeScreen
import io.github.pikatchu2k3.oneplusdebloat.ui.MainViewModel
import io.github.pikatchu2k3.oneplusdebloat.ui.TelemetryScreen
import io.github.pikatchu2k3.oneplusdebloat.ui.UiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the screens on the JVM so the layout can be checked without a phone.
 * Record with: ./gradlew recordRoborazziDebug   (images land in app/build/outputs/roborazzi)
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-420dpi")
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var vm: MainViewModel

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val pm = shadowOf(app.packageManager)
        fun install(pkg: String, label: String, enabled: Boolean = true) {
            pm.installPackage(PackageInfo().apply {
                packageName = pkg
                applicationInfo = ApplicationInfo().apply {
                    packageName = pkg
                    nonLocalizedLabel = label
                    flags = ApplicationInfo.FLAG_INSTALLED or ApplicationInfo.FLAG_SYSTEM
                    this.enabled = enabled
                }
            })
        }
        install("com.heytap.browser", "Internet")
        install("com.oplus.games", "Spiele")
        install("com.oplus.statistics.rom", "Nutzungsstatistik", enabled = false)
        install("com.oplus.onetrace", "OneTrace", enabled = false)
        install("com.oplus.postmanservice", "Postman-Dienst")
        install("com.oplus.logkit", "Log-Kit")
        install("com.facebook.services", "Facebook Services", enabled = false)
        install("com.facebook.system", "Facebook App Installer", enabled = false)
        install("com.google.android.apps.wellbeing", "Digital Wellbeing")
        install("com.coloros.video", "Videos")
        install("net.oneplus.weather", "Wetter")
        install("com.android.chrome", "Chrome")
        install("com.oplus.encryption", "Privater Safe")
        install("com.google.android.dialer", "Telefon")
        install("com.google.android.inputmethod.latin", "Gboard")
        install("com.google.android.gm", "Gmail", enabled = false)
        install("com.example.oldgame", "Altes Spiel", enabled = false)
        vm = MainViewModel(app)
        // The view model loads on a background thread and publishes on the main looper,
        // which Robolectric only runs when asked to.
        val deadline = System.currentTimeMillis() + 10_000
        while (!vm.ui.value.loaded && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(20)
        }
        check(vm.ui.value.loaded) { "view model did not finish loading" }
    }

    private val frozenByUs = setOf(
        "com.oplus.statistics.rom", "com.oplus.onetrace", "com.facebook.services", "com.facebook.system",
    )

    private fun ready(): UiState = vm.ui.value.copy(shizuku = ShizukuState.READY, journalFrozen = frozenByUs)

    private fun shot(name: String, dark: Boolean = false, german: Boolean = true, content: @Composable () -> Unit) {
        rule.setContent {
            CleanupTheme(dark = dark, german = german, dynamic = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Box(Modifier.fillMaxSize()) { content() }
                }
            }
        }
        rule.waitForIdle()
        captureScreenRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test
    fun home_notConnected() = shot("01_home_not_connected") {
        HomeScreen(vm.ui.value, vm, {}, {}, {})
    }

    @Test
    fun home_ready() = shot("02_home_ready") {
        HomeScreen(ready(), vm, {}, {}, {})
    }

    @Test
    fun home_drift_dark() = shot("03_home_drift_dark", dark = true) {
        HomeScreen(
            ready().copy(
                drift = listOf(DriftEntry("com.facebook.services", Drift.ACTIVE_AGAIN)),
                log = listOf("2026-11-16 08:12  FROZEN  com.oplus.onetrace", "2026-11-16 08:12  FROZEN  com.facebook.system"),
            ),
            vm, {}, {}, {},
        )
    }

    @Test
    fun apps_safe_selection() = shot("04_apps_safe") {
        AppsScreen(ready().copy(selected = setOf("com.heytap.browser", "com.oplus.games")), vm, 0, {}, {})
    }

    @Test
    fun apps_caution() = shot("05_apps_caution") {
        AppsScreen(ready(), vm, 3, {}, {})
    }

    @Test
    fun apps_off_english() = shot("11_apps_off_en", german = false) {
        AppsScreen(ready(), vm, 4, {}, {})
    }

    @Test
    fun detail_caution_dialer() = shot("12_detail_caution") {
        val ui = ready()
        AppsScreen(ui, vm, 3, {}, {})
        DetailSheet(ui.rows.getValue("com.google.android.dialer"), ui, vm, {}, {}, {})
    }

    @Test
    fun apps_custom_empty() = shot("06_apps_custom_empty") {
        AppsScreen(ready(), vm, 2, {}, {})
    }

    @Test
    fun detail_sheet() = shot("07_detail_browser") {
        val ui = ready()
        AppsScreen(ui, vm, 0, {}, {})
        DetailSheet(ui.rows.getValue("com.heytap.browser"), ui, vm, {}, {}, {})
    }

    @Test
    fun detail_sheet_view_only() = shot("08_detail_view_only") {
        val ui = vm.ui.value
        AppsScreen(ui, vm, 1, {}, {})
        DetailSheet(ui.rows.getValue("com.oplus.encryption"), ui, vm, {}, {}, {})
    }

    @Test
    fun telemetry() = shot("09_telemetry") {
        val ops = vm.catalog.appOpsPackages.flatMap { pkg ->
            vm.catalog.appOps.map { op ->
                val state = when (pkg) {
                    "com.oplus.statistics.rom", "com.oplus.onetrace" -> OpState.PACKAGE_FROZEN
                    "com.oplus.logkit" -> OpState.BLOCKED
                    "com.oplus.postmanservice" -> OpState.OPEN
                    else -> OpState.NOT_PRESENT
                }
                OpStatus(pkg, op, state, if (state == OpState.OPEN) "No operations. Default mode: allow" else "")
            }
        }
        TelemetryScreen(ready().copy(ops = ops), vm, {})
    }

    @Test
    fun help() = shot("10_help") {
        HelpScreen(vm.ui.value, vm)
    }
}
