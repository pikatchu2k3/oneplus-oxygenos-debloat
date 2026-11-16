package io.github.pikatchu2k3.oneplusdebloat;

// Runs inside the process Shizuku starts for us (uid shell or root).
interface IShellService {
    // Reserved transaction code: Shizuku calls this before it kills the service process.
    void destroy() = 16777114;

    // Runs "sh -c <command>" and returns "<exit code>\n<stdout and stderr>".
    String exec(String command) = 1;
}
