package io.github.pikatchu2k3.oneplusdebloat.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class ListKind { SAFE, OPTIONAL, NEVER, CUSTOM }

@Serializable
data class CatalogEntry(
    val id: String,
    /** "safe", "optional" or "never" */
    val list: String,
    /** Short human name, used when the package is not installed and has no label. */
    val name: String = "",
    val de: String = "",
    val en: String = "",
) {
    val kind: ListKind
        get() = when (list) {
            "safe" -> ListKind.SAFE
            "optional" -> ListKind.OPTIONAL
            else -> ListKind.NEVER
        }

    fun description(german: Boolean): String = if (german && de.isNotEmpty()) de else en.ifEmpty { de }
}

@Serializable
data class Catalog(
    val version: Int = 1,
    /** System base: launcher, SystemUI, phone, settings ... never frozen or removed. */
    val critical: List<String> = emptyList(),
    val appOpsPackages: List<String> = emptyList(),
    val appOps: List<String> = emptyList(),
    val packages: List<CatalogEntry> = emptyList(),
) {
    fun ofKind(kind: ListKind): List<CatalogEntry> = packages.filter { it.kind == kind }

    /** Packages the app refuses to freeze or remove. */
    val protectedPackages: Set<String>
        get() = critical.toSet() + ofKind(ListKind.NEVER).map { it.id }

    fun find(pkg: String): CatalogEntry? = packages.firstOrNull { it.id == pkg }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): Catalog = json.decodeFromString(serializer(), text)
    }
}

/** A user-made list, also the import/export format. */
@Serializable
data class CustomList(
    val format: String = FORMAT,
    val packages: List<String> = emptyList(),
) {
    companion object {
        const val FORMAT = "oneplus-cleanup-custom-list-v1"
        private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

        fun encode(packages: Collection<String>): String =
            json.encodeToString(serializer(), CustomList(packages = packages.sorted()))

        /**
         * Accepts our own JSON export or a plain text file with one package name per line
         * (lines starting with # are comments). Invalid names are dropped.
         */
        fun decode(text: String): List<String> {
            val trimmed = text.trim()
            val names = if (trimmed.startsWith("{")) {
                json.decodeFromString(serializer(), trimmed).packages
            } else {
                trimmed.lineSequence().map { it.substringBefore('#').trim() }.toList()
            }
            return names.filter { isValidPackageName(it) }.distinct()
        }
    }
}
