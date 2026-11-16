package io.github.pikatchu2k3.oneplusdebloat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pikatchu2k3.oneplusdebloat.core.OpOutcome
import io.github.pikatchu2k3.oneplusdebloat.core.OpState
import io.github.pikatchu2k3.oneplusdebloat.core.OpStatus

/** The script's menu 5/6/8: block background activity of the telemetry services via app-ops. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TelemetryScreen(ui: UiState, vm: MainViewModel, onOpenHelp: () -> Unit) {
    val t = LocalTxt.current
    val sc = LocalStateColors.current
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(t.telemetryTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        InfoCard(icon = Icons.Filled.Lock, title = t.telemetryWhatTitle) {
            Text(t.telemetryWhatBody)
        }

        if (!ui.ready) {
            InfoCard(icon = Icons.Filled.Info, title = t.telemetryNeedsShellTitle, container = scheme.primaryContainer, content = scheme.onPrimaryContainer) {
                Text(t.telemetryNeedsShellBody)
                TextButton(onClick = onOpenHelp) { Text(t.howTo) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { vm.setOps(true) }, enabled = ui.ready && !ui.opsBusy) { Text(t.opsBlock) }
            OutlinedButton(onClick = { vm.setOps(false) }, enabled = ui.ready && !ui.opsBusy) { Text(t.opsReset) }
            if (ui.opsBusy) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 3.dp)
            else TextButton(onClick = vm::loadOps, enabled = ui.ready) { Text(t.refresh) }
        }

        for (pkg in vm.catalog.appOpsPackages) {
            val row = ui.rows[pkg]
            val statuses = ui.ops?.filter { it.pkg == pkg }.orEmpty()
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(pkg, row?.label ?: pkg, vm, size = 36.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(row?.label ?: pkg, fontWeight = FontWeight.Medium)
                            PackageName(pkg)
                        }
                        row?.let { StateChip(it.state) }
                    }
                    row?.entry?.description(t.german)?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                    if (statuses.isEmpty()) {
                        Text(
                            if (ui.ready) t.opsLoading else t.opsUnknown,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (s in statuses) OpPill(s)
                        }
                        statuses.firstOrNull { it.state == OpState.OPEN && it.phoneSays.isNotEmpty() }?.let {
                            Text(t.phoneSays(it.phoneSays), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        Text(t.telemetryLegend, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
    }

    ui.opsReport?.let { report ->
        val refused = report.refusedPackages
        AlertDialog(
            onDismissRequest = vm::dismissOpsReport,
            title = { Text(t.reportTitle) },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    for (line in t.opsReportLines(report)) Text(line)
                    if (refused.isNotEmpty()) {
                        Text(t.opsRefusedTitle, fontWeight = FontWeight.SemiBold)
                        Text(t.opsRefusedBody)
                        for (pkg in refused) {
                            Column {
                                Text(ui.rows[pkg]?.label ?: pkg, fontWeight = FontWeight.Medium)
                                PackageName(pkg)
                            }
                        }
                        report.results.firstOrNull { it.outcome == OpOutcome.REFUSED }?.let {
                            Text(t.phoneSays(it.phoneSays), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (refused.isNotEmpty()) {
                    TextButton(onClick = { vm.dismissOpsReport(); vm.freeze(refused) }) { Text(t.freezeInstead(refused.size)) }
                } else {
                    TextButton(onClick = vm::dismissOpsReport) { Text(t.ok) }
                }
            },
            dismissButton = {
                if (refused.isNotEmpty()) TextButton(onClick = vm::dismissOpsReport) { Text(t.notNow) }
            },
        )
    }
}

@Composable
private fun OpPill(status: OpStatus) {
    val t = LocalTxt.current
    val sc = LocalStateColors.current
    val scheme = MaterialTheme.colorScheme
    val name = t.opName(status.op)
    when (status.state) {
        OpState.BLOCKED -> Pill("$name: ${t.opBlocked}", sc.ok, sc.onOk)
        OpState.PACKAGE_FROZEN -> Pill("$name: ${t.opNotRunning}", sc.ok, sc.onOk)
        OpState.OPEN -> Pill("$name: ${t.opOpen}", sc.warn, sc.onWarn)
        OpState.NOT_PRESENT -> Pill("$name: –", scheme.surface, scheme.onSurfaceVariant)
    }
}
