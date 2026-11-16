package io.github.pikatchu2k3.oneplusdebloat.shizuku

import androidx.annotation.Keep
import io.github.pikatchu2k3.oneplusdebloat.IShellService
import kotlin.system.exitProcess

/**
 * Started by Shizuku in its own process, running as the shell user (or root).
 * It only runs the commands the app sends; all decisions stay in the app.
 */
@Keep
class ShellService @Keep constructor() : IShellService.Stub() {

    override fun destroy() {
        exitProcess(0)
    }

    override fun exec(command: String): String = try {
        val process = ProcessBuilder("sh", "-c", command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val code = process.waitFor()
        "$code\n$output"
    } catch (e: Exception) {
        "-1\n${e.javaClass.simpleName}: ${e.message}"
    }
}
