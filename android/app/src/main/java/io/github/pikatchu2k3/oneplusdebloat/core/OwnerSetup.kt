package io.github.pikatchu2k3.oneplusdebloat.core

/**
 * Setting this app up as device owner with `dpm set-device-owner` (through Shizuku or adb; both run
 * as the shell user). Android only allows it while there are no accounts on the phone and no second
 * user or profile (OxygenOS "App cloner" / parallel apps count as one). The app never removes
 * anything to get there; it shows what stands in the way.
 */
object OwnerSetup {

    enum class Result {
        OK,

        /** Accounts on the phone (Google, OnePlus, ...). They can be added again afterwards. */
        ACCOUNTS,

        /** Second user, work profile or cloned apps. */
        USERS,

        /** Another app is device owner already (e.g. App Ops Shizuku). Only one per phone. */
        OTHER_OWNER,
        OTHER,
    }

    fun command(userId: Int, component: String) = "dpm set-device-owner --user $userId $component"

    /** Sorts the answer of `dpm set-device-owner`; the wording differs between Android versions. */
    fun classify(output: String): Result {
        val t = output.lowercase()
        return when {
            t.contains("success") && !t.contains("exception") -> Result.OK
            t.contains("account") -> Result.ACCOUNTS
            t.contains("several users") || t.contains("multiple users") || t.contains("more than one user") ||
                t.contains("profile") -> Result.USERS
            t.contains("already") && (t.contains("owner") || t.contains("provisioned")) -> Result.OTHER_OWNER
            else -> Result.OTHER
        }
    }
}
