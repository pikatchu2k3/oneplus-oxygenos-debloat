package io.github.pikatchu2k3.oneplusdebloat.shizuku

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import io.github.pikatchu2k3.oneplusdebloat.BuildConfig
import io.github.pikatchu2k3.oneplusdebloat.IShellService
import io.github.pikatchu2k3.oneplusdebloat.core.ShellExecutor
import io.github.pikatchu2k3.oneplusdebloat.core.ShellResult
import io.github.pikatchu2k3.oneplusdebloat.core.ShellUnavailableException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ShizukuState {
    /** The Shizuku app is not installed. */
    NOT_INSTALLED,

    /** Installed, but its service is not running (it has to be started after every reboot). */
    NOT_RUNNING,

    /** Running, but this app has not been allowed yet. */
    NEEDS_PERMISSION,

    /** Allowed; the helper service is being started. */
    CONNECTING,
    READY,
}

/** Talks to Shizuku and runs shell commands through a small user service. */
class ShizukuBackend(context: Context) : ShellExecutor {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(ShizukuState.NOT_INSTALLED)
    val state: StateFlow<ShizukuState> = _state

    @Volatile
    private var service: IShellService? = null
    private var bound = false
    private var started = false

    private val serviceArgs by lazy {
        Shizuku.UserServiceArgs(ComponentName(appContext.packageName, ShellService::class.java.name))
            .daemon(false)
            .processNameSuffix("shell")
            .debuggable(BuildConfig.DEBUG)
            .version(BuildConfig.VERSION_CODE)
    }

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = if (binder != null && binder.pingBinder()) IShellService.Stub.asInterface(binder) else null
            refresh()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
            refresh()
        }
    }

    private val binderReceived = Shizuku.OnBinderReceivedListener { refresh() }
    private val binderDead = Shizuku.OnBinderDeadListener {
        service = null
        bound = false
        refresh()
    }
    private val permissionResult = Shizuku.OnRequestPermissionResultListener { _, _ -> refresh() }

    override val isReady: Boolean get() = service != null

    /** Call once the UI is up. Safe to call repeatedly. */
    fun start() {
        if (started) {
            refresh()
            return
        }
        started = true
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceived)
            Shizuku.addBinderDeadListener(binderDead)
            Shizuku.addRequestPermissionResultListener(permissionResult)
        } catch (_: Throwable) {
            // Shizuku classes are always there; failing here only means no callbacks.
        }
        refresh()
    }

    fun stop() {
        if (!started) return
        started = false
        try {
            Shizuku.removeBinderReceivedListener(binderReceived)
            Shizuku.removeBinderDeadListener(binderDead)
            Shizuku.removeRequestPermissionResultListener(permissionResult)
            if (bound) Shizuku.unbindUserService(serviceArgs, connection, true)
        } catch (_: Throwable) {
        }
        bound = false
        service = null
    }

    fun requestPermission() {
        try {
            if (Shizuku.pingBinder() && !Shizuku.isPreV11()) Shizuku.requestPermission(REQUEST_CODE)
        } catch (_: Throwable) {
            refresh()
        }
    }

    /** Works out where we stand and binds the helper service when that becomes possible. */
    fun refresh() {
        val next = try {
            when {
                !Shizuku.pingBinder() ->
                    if (isShizukuInstalled()) ShizukuState.NOT_RUNNING else ShizukuState.NOT_INSTALLED
                Shizuku.isPreV11() -> ShizukuState.NOT_RUNNING
                Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED ->
                    ShizukuState.NEEDS_PERMISSION
                service != null -> ShizukuState.READY
                else -> {
                    bind()
                    ShizukuState.CONNECTING
                }
            }
        } catch (_: Throwable) {
            ShizukuState.NOT_RUNNING
        }
        _state.value = next
    }

    private fun bind() {
        if (bound) return
        bound = true
        try {
            Shizuku.bindUserService(serviceArgs, connection)
        } catch (_: Throwable) {
            bound = false
        }
    }

    private fun isShizukuInstalled(): Boolean = try {
        appContext.packageManager.getApplicationInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    override suspend fun exec(command: String): ShellResult = withContext(Dispatchers.IO) {
        val svc = service ?: throw ShellUnavailableException("Shizuku is not connected")
        val raw = try {
            svc.exec(command)
        } catch (e: Exception) {
            service = null
            bound = false
            refresh()
            throw ShellUnavailableException("Shizuku connection lost", e)
        }
        val code = raw.substringBefore('\n').trim().toIntOrNull() ?: -1
        ShellResult(code, raw.substringAfter('\n', ""))
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
        private const val REQUEST_CODE = 4711
    }
}
