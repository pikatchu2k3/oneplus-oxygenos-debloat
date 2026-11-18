package io.github.pikatchu2k3.oneplusdebloat.data

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.github.pikatchu2k3.oneplusdebloat.core.Journal
import io.github.pikatchu2k3.oneplusdebloat.core.PackageSource
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

enum class DefaultRole { KEYBOARD, DIALER, SMS, LAUNCHER }

/** Reads package states through the normal PackageManager; needs no special rights. */
class AndroidPackageSource(context: Context) : PackageSource {
    private val context: Context = context.applicationContext
    private val pm: PackageManager = context.applicationContext.packageManager

    override fun stateOf(pkg: String): PkgState = stateOf(infoOf(pkg))

    fun stateOf(info: ApplicationInfo?): PkgState {
        if (info == null) return PkgState.NOT_PRESENT
        if (info.flags and ApplicationInfo.FLAG_INSTALLED == 0) return PkgState.REMOVED
        if (!info.enabled) return PkgState.FROZEN
        if (info.flags and ApplicationInfo.FLAG_SUSPENDED != 0) return PkgState.SUSPENDED
        // A hidden package is still "installed", but only shows up when uninstalled ones are included.
        return if (isVisible(info.packageName)) PkgState.ACTIVE else PkgState.HIDDEN
    }

    private fun isVisible(pkg: String): Boolean = try {
        pm.getApplicationInfo(pkg, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Also finds packages that were removed for the user but still sit in the system image. */
    fun infoOf(pkg: String): ApplicationInfo? = try {
        pm.getApplicationInfo(pkg, PackageManager.MATCH_UNINSTALLED_PACKAGES)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun labelOf(info: ApplicationInfo): String = info.loadLabel(pm).toString()

    /**
     * The job this package currently does as the phone's default app, if any.
     * Freezing the default keyboard, dialer, SMS app or launcher can lock the user out.
     */
    fun defaultRoleOf(pkg: String): DefaultRole? = try {
        when (pkg) {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                ?.substringBefore('/') -> DefaultRole.KEYBOARD
            (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage ->
                DefaultRole.DIALER
            Telephony.Sms.getDefaultSmsPackage(context) -> DefaultRole.SMS
            pm.resolveActivity(
                Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
                PackageManager.MATCH_DEFAULT_ONLY,
            )?.activityInfo?.packageName -> DefaultRole.LAUNCHER
            else -> null
        }
    } catch (_: Exception) {
        null
    }

    fun allApps(): List<ApplicationInfo> =
        pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)
}

@Serializable
private data class Saved(
    val frozen: Set<String> = emptySet(),
    val removed: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
    val neutralized: Set<String> = emptySet(),
    val custom: Set<String> = emptySet(),
    val log: List<String> = emptyList(),
)

/** Journal, custom list and a short activity log, kept in one small JSON file. */
class AppStore(private val file: File) : Journal {
    private val json = Json { ignoreUnknownKeys = true }
    private var saved: Saved = load()

    constructor(context: Context) : this(File(context.applicationContext.filesDir, "state.json"))

    override val frozen: Set<String> get() = saved.frozen
    override val removed: Set<String> get() = saved.removed
    override val hidden: Set<String> get() = saved.hidden
    override val neutralized: Set<String> get() = saved.neutralized
    val custom: Set<String> get() = saved.custom
    val log: List<String> get() = saved.log

    override fun markFrozen(pkg: String) = update { it.copy(frozen = it.frozen + pkg) }
    override fun unmarkFrozen(pkg: String) = update { it.copy(frozen = it.frozen - pkg) }
    override fun markRemoved(pkg: String) = update { it.copy(removed = it.removed + pkg) }
    override fun unmarkRemoved(pkg: String) = update { it.copy(removed = it.removed - pkg) }
    override fun markHidden(pkg: String) = update { it.copy(hidden = it.hidden + pkg) }
    override fun unmarkHidden(pkg: String) = update { it.copy(hidden = it.hidden - pkg) }
    override fun markNeutralized(pkg: String) = update { it.copy(neutralized = it.neutralized + pkg) }
    override fun unmarkNeutralized(pkg: String) = update { it.copy(neutralized = it.neutralized - pkg) }

    fun addCustom(pkgs: Collection<String>) = update { it.copy(custom = it.custom + pkgs) }
    fun removeCustom(pkg: String) = update { it.copy(custom = it.custom - pkg) }

    fun appendLog(line: String) = update { it.copy(log = (it.log + line).takeLast(MAX_LOG)) }
    fun clearLog() = update { it.copy(log = emptyList()) }

    @Synchronized
    private fun update(change: (Saved) -> Saved) {
        val next = change(saved)
        if (next == saved) return
        saved = next
        try {
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(json.encodeToString(Saved.serializer(), next))
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        } catch (_: Exception) {
            // Keeping the state in memory is the best we can do if the disk is full.
        }
    }

    private fun load(): Saved = try {
        if (file.exists()) json.decodeFromString(Saved.serializer(), file.readText()) else Saved()
    } catch (_: Exception) {
        Saved()
    }

    private companion object {
        const val MAX_LOG = 300
    }
}
