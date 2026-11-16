package io.github.pikatchu2k3.oneplusdebloat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pikatchu2k3.oneplusdebloat.core.Outcome

enum class Tab { HOME, APPS, TELEMETRY, HELP }

@Composable
fun CleanupApp(vm: MainViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val t = LocalTxt.current
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var listTab by rememberSaveable { mutableIntStateOf(0) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmRemove by remember { mutableStateOf<List<String>?>(null) }
    // A replaceable core app (dialer, keyboard ...) and whether it is to be removed instead of frozen.
    var caution by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    val snackbar = remember { SnackbarHostState() }

    // Coming back from Shizuku or the system settings: states may have changed.
    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }
    LaunchedEffect(ui.message) {
        val message = ui.message ?: return@LaunchedEffect
        vm.consumeMessage()
        snackbar.showSnackbar(t.message(message))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == Tab.HOME, onClick = { tab = Tab.HOME },
                    icon = { Icon(Icons.Filled.Home, null) }, label = { Text(t.tabHome) },
                )
                NavigationBarItem(
                    selected = tab == Tab.APPS, onClick = { tab = Tab.APPS },
                    icon = { Icon(Icons.Filled.List, null) }, label = { Text(t.tabApps) },
                )
                NavigationBarItem(
                    selected = tab == Tab.TELEMETRY, onClick = { tab = Tab.TELEMETRY },
                    icon = { Icon(Icons.Filled.Lock, null) }, label = { Text(t.tabTelemetry) },
                )
                NavigationBarItem(
                    selected = tab == Tab.HELP, onClick = { tab = Tab.HELP },
                    icon = { Icon(Icons.Filled.Info, null) }, label = { Text(t.tabHelp) },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                Tab.HOME -> HomeScreen(
                    ui = ui, vm = vm,
                    onOpenList = { index -> listTab = index; tab = Tab.APPS },
                    onOpenHelp = { tab = Tab.HELP },
                    onDetail = { detail = it },
                )
                Tab.APPS -> AppsScreen(
                    ui = ui, vm = vm, listTab = listTab,
                    onListTab = { listTab = it },
                    onDetail = { detail = it },
                )
                Tab.TELEMETRY -> TelemetryScreen(ui = ui, vm = vm, onOpenHelp = { tab = Tab.HELP })
                Tab.HELP -> HelpScreen(ui = ui, vm = vm)
            }
        }
    }

    ui.progress?.let { ProgressDialog(it) }

    ui.report?.let { report ->
        ReportDialog(
            report = report,
            ui = ui,
            onDismiss = vm::dismissReport,
            onRemoveIgnored = {
                confirmRemove = report.ignored.map { it.pkg }
                vm.dismissReport()
            },
        )
    }

    confirmRemove?.let { pkgs ->
        RemoveConfirmDialog(
            pkgs = pkgs, ui = ui,
            onConfirm = { confirmRemove = null; vm.remove(pkgs) },
            onDismiss = { confirmRemove = null },
        )
    }

    detail?.let { pkg ->
        val row = ui.rows[pkg]
        if (row == null) detail = null
        else DetailSheet(
            row = row, ui = ui, vm = vm,
            onDismiss = { detail = null },
            onFreeze = { if (row.isCaution) caution = pkg to false else vm.freeze(listOf(pkg)) },
            onRemove = { if (row.isCaution) caution = pkg to true else confirmRemove = listOf(pkg) },
        )
    }

    caution?.let { (pkg, removing) ->
        AlertDialog(
            onDismissRequest = { caution = null },
            title = { Text(t.cautionTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ui.rows[pkg]?.label ?: pkg, fontWeight = FontWeight.SemiBold)
                    PackageName(pkg)
                    ui.rows[pkg]?.entry?.description(t.german)?.takeIf { it.isNotEmpty() }?.let { Text(it) }
                    Text(t.cautionBody)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    caution = null
                    if (removing) confirmRemove = listOf(pkg) else vm.freeze(listOf(pkg))
                }) { Text(t.cautionConfirm) }
            },
            dismissButton = { TextButton(onClick = { caution = null }) { Text(t.cancel) } },
        )
    }
}

@Composable
private fun ProgressDialog(progress: Progress) {
    val t = LocalTxt.current
    AlertDialog(
        onDismissRequest = { },
        confirmButton = { },
        title = { Text(t.progressTitle(progress.kind)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { if (progress.total == 0) 0f else progress.done.toFloat() / progress.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${progress.done + 1} / ${progress.total}", style = MaterialTheme.typography.labelLarge)
                PackageName(progress.current)
                Text(t.progressHint, style = MaterialTheme.typography.bodySmall)
            }
        },
    )
}

@Composable
private fun ReportDialog(report: Report, ui: UiState, onDismiss: () -> Unit, onRemoveIgnored: () -> Unit) {
    val t = LocalTxt.current
    val ignored = report.ignored
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.reportTitle) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (line in t.reportLines(report)) Text(line)
                if (ignored.isNotEmpty()) {
                    Text(t.reportIgnoredTitle(ignored.size), fontWeight = FontWeight.SemiBold)
                    Text(t.reportIgnoredBody, style = MaterialTheme.typography.bodyMedium)
                    for (r in ignored) FailureLine(ui.rows[r.pkg]?.label ?: r.pkg, r.pkg, r.phoneSays)
                }
                if (report.failed.isNotEmpty()) {
                    Text(t.reportFailedTitle(report.failed.size), fontWeight = FontWeight.SemiBold)
                    for (r in report.failed) {
                        FailureLine(ui.rows[r.pkg]?.label ?: r.pkg, r.pkg, r.phoneSays.ifEmpty { t.outcome(r.outcome) })
                    }
                }
                if (report.count(Outcome.DEFAULT_APP) > 0) {
                    Text(t.reportDefaultApp(report.count(Outcome.DEFAULT_APP)), style = MaterialTheme.typography.bodyMedium)
                }
                if (report.count(Outcome.PROTECTED) > 0) {
                    Text(t.reportProtected(report.count(Outcome.PROTECTED)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            if (ignored.isNotEmpty()) TextButton(onClick = onRemoveIgnored) { Text(t.removeInstead(ignored.size)) }
            else TextButton(onClick = onDismiss) { Text(t.ok) }
        },
        dismissButton = {
            if (ignored.isNotEmpty()) TextButton(onClick = onDismiss) { Text(t.notNow) }
        },
    )
}

@Composable
private fun FailureLine(label: String, pkg: String, phoneSays: String) {
    val t = LocalTxt.current
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        PackageName(pkg)
        if (phoneSays.isNotEmpty()) {
            Text(t.phoneSays(phoneSays), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun RemoveConfirmDialog(pkgs: List<String>, ui: UiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val t = LocalTxt.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.removeTitle(pkgs.size)) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(t.removeBody)
                for (pkg in pkgs) {
                    Column {
                        Text(ui.rows[pkg]?.label ?: pkg, fontWeight = FontWeight.Medium)
                        PackageName(pkg)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(t.removeConfirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t.cancel) } },
    )
}
