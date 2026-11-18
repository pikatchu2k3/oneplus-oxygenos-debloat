package io.github.pikatchu2k3.oneplusdebloat.data

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import io.github.pikatchu2k3.oneplusdebloat.core.DeviceOwner

/**
 * Receiver that lets the app become device owner (`dpm set-device-owner`). It asks for no policies;
 * the only thing the app does as device owner is hiding and showing apps the user picked.
 */
class OwnerReceiver : DeviceAdminReceiver()

/** Device owner rights through the device policy API. Every change is read back by the engine. */
class AndroidDeviceOwner(context: Context) : DeviceOwner {
    private val context = context.applicationContext
    private val dpm: DevicePolicyManager? = this.context.getSystemService(DevicePolicyManager::class.java)
    val admin = ComponentName(this.context, OwnerReceiver::class.java)

    /** As passed to `dpm set-device-owner`. */
    val component: String get() = admin.flattenToShortString()

    override val isOwner: Boolean
        get() = try {
            dpm?.isDeviceOwnerApp(context.packageName) == true
        } catch (_: Exception) {
            false
        }

    override fun setHidden(pkg: String, hidden: Boolean): String? = try {
        val d = dpm ?: return "DevicePolicyManager missing"
        if (d.setApplicationHidden(admin, pkg, hidden)) null else "setApplicationHidden: false"
    } catch (e: Exception) {
        e.message ?: e.javaClass.simpleName
    }

    /**
     * Gives up device owner. The caller shows every hidden app first - otherwise nobody could bring
     * them back. Returns true when this app is no longer device owner.
     */
    fun release(): Boolean {
        try {
            @Suppress("DEPRECATION")
            dpm?.clearDeviceOwnerApp(context.packageName)
        } catch (_: Exception) {
        }
        return !isOwner
    }
}
