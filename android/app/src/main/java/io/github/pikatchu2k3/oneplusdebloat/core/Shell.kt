package io.github.pikatchu2k3.oneplusdebloat.core

/** Result of one shell command run with adb-shell rights. */
data class ShellResult(val exitCode: Int, val output: String) {
    /** First non-blank line, for showing "the phone says: ..." to the user. */
    val firstLine: String
        get() = output.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: ""
}

/**
 * Something that can run commands with the rights of `adb shell`.
 * Shizuku is the first implementation; wireless ADB will be the second.
 */
interface ShellExecutor {
    /** True if commands can be run right now. */
    val isReady: Boolean

    /** Runs the command. Throws [ShellUnavailableException] if the backend went away. */
    suspend fun exec(command: String): ShellResult
}

class ShellUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Where a package stands for the current user. */
enum class PkgState {
    /** Not part of this phone's system image and not installed. */
    NOT_PRESENT,

    /** Removed for the user with `pm uninstall -k --user`; the system copy is still there. */
    REMOVED,
    ACTIVE,

    /** Disabled (`pm disable-user`), by this app or by anything else. */
    FROZEN,
}

/** Read-only view of the installed packages. Works without any special rights. */
interface PackageSource {
    fun stateOf(pkg: String): PkgState
}

private val PACKAGE_NAME = Regex("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z0-9_]+)+$")

/** Package names are pasted into shell commands, so nothing else may get through. */
fun isValidPackageName(name: String): Boolean = name.length <= 255 && PACKAGE_NAME.matches(name)
