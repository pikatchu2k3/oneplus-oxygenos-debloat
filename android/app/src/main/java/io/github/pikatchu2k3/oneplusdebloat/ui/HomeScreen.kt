package io.github.pikatchu2k3.oneplusdebloat.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.pikatchu2k3.oneplusdebloat.BuildConfig
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuBackend
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuState

@Composable
fun HomeScreen(
    ui: UiState,
    vm: MainViewModel,
    onOpenList: (Int) -> Unit,
    onOpenHelp: () -> Unit,
    onDetail: (String) -> Unit,
) {
    val t = LocalTxt.current
    var confirmReset by remember { mutableStateOf(false) }
    var showLog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(t.appTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(t.appSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ConnectionCard(ui.shizuku, vm, onOpenHelp)

        // What this app has done so far, and what is still running from the safe list.
        val frozen = ui.journalFrozen.count { ui.rows[it]?.state == PkgState.FROZEN }
        val removed = ui.journalRemoved.count { ui.rows[it]?.state == PkgState.REMOVED }
        val safeActive = vm.safeIds.count { ui.rows[it]?.state == PkgState.ACTIVE }
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(frozen.toString(), t.statFrozen, Modifier.weight(1f).fillMaxHeight())
            StatCard(removed.toString(), t.statRemoved, Modifier.weight(1f).fillMaxHeight())
            StatCard(safeActive.toString(), t.statSafeActive, Modifier.weight(1f).fillMaxHeight()) { onOpenList(0) }
        }

        if (ui.drift.isNotEmpty()) {
            InfoCard(
                icon = Icons.Filled.Warning,
                title = t.driftTitle(ui.drift.size),
                container = LocalStateColors.current.warn,
                content = LocalStateColors.current.onWarn,
            ) {
                Text(t.driftBody)
                for (d in ui.drift.take(8)) {
                    Text(
                        "• " + (ui.rows[d.pkg]?.label ?: d.pkg),
                        modifier = Modifier.clickable { onDetail(d.pkg) },
                    )
                }
                if (ui.drift.size > 8) Text(t.andMore(ui.drift.size - 8))
                Button(onClick = vm::reapplyDrift, enabled = ui.ready) { Text(t.driftApply) }
                if (!ui.ready) Text(t.needsShellShort, style = MaterialTheme.typography.bodySmall)
            }
        } else if (ui.loaded && (ui.journalFrozen.isNotEmpty() || ui.journalRemoved.isNotEmpty())) {
            InfoCard(
                icon = Icons.Filled.CheckCircle,
                title = t.allGoodTitle,
                container = LocalStateColors.current.ok,
                content = LocalStateColors.current.onOk,
            ) { Text(t.allGoodBody) }
        }

        InfoCard(icon = Icons.Filled.List, title = t.startTitle) {
            Text(t.startBody)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onOpenList(0) }) { Text(t.listSafe) }
                FilledTonalButton(onClick = { onOpenList(2) }) { Text(t.listCustom) }
            }
        }

        InfoCard(icon = Icons.Filled.Refresh, title = t.resetTitle) {
            Text(t.resetBody)
            OutlinedButton(
                onClick = { confirmReset = true },
                enabled = ui.journalFrozen.isNotEmpty() || ui.journalRemoved.isNotEmpty(),
            ) { Text(t.resetButton) }
        }

        if (ui.log.isNotEmpty()) {
            InfoCard(icon = Icons.Filled.Info, title = t.logTitle) {
                val lines = if (showLog) ui.log.reversed() else ui.log.takeLast(3).reversed()
                for (line in lines) {
                    Text(line, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace)
                }
                Row {
                    TextButton(onClick = { showLog = !showLog }) { Text(if (showLog) t.showLess else t.showAll) }
                    TextButton(onClick = vm::clearLog) { Text(t.clearLog) }
                }
            }
        }

        Text(
            t.versionLine(BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(t.resetTitle) },
            text = { Text(t.resetConfirmBody(ui.journalFrozen.size, ui.journalRemoved.size)) },
            confirmButton = {
                TextButton(onClick = { confirmReset = false; vm.resetAll() }) { Text(t.resetButton) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(t.cancel) } },
        )
    }
}

@Composable
private fun ConnectionCard(state: ShizukuState, vm: MainViewModel, onOpenHelp: () -> Unit) {
    val t = LocalTxt.current
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val ready = state == ShizukuState.READY
    InfoCard(
        icon = if (ready) Icons.Filled.CheckCircle else Icons.Filled.Info,
        title = t.connectionTitle(state),
        container = if (ready) LocalStateColors.current.ok else scheme.primaryContainer,
        content = if (ready) LocalStateColors.current.onOk else scheme.onPrimaryContainer,
    ) {
        Text(t.connectionBody(state))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            when (state) {
                ShizukuState.NOT_INSTALLED -> Button(onClick = {
                    openUrl(context, "https://play.google.com/store/apps/details?id=${ShizukuBackend.SHIZUKU_PACKAGE}")
                }) { Text(t.getShizuku) }
                ShizukuState.NOT_RUNNING -> Button(onClick = {
                    launchApp(context, ShizukuBackend.SHIZUKU_PACKAGE)
                }) { Text(t.openShizuku) }
                ShizukuState.NEEDS_PERMISSION -> Button(onClick = vm::requestShizukuPermission) { Text(t.allowAccess) }
                ShizukuState.CONNECTING, ShizukuState.READY -> Unit
            }
            if (!ready) TextButton(onClick = onOpenHelp) { Text(t.howTo) }
        }
        if (!ready) Text(t.viewOnlyHint, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Center,
            )
        }
    }
}
