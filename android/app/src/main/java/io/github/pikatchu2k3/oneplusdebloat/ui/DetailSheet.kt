package io.github.pikatchu2k3.oneplusdebloat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pikatchu2k3.oneplusdebloat.core.ListKind
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState

/** Everything about one package: what it is, where it stands, and what can be done with it. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailSheet(row: AppRow, ui: UiState, vm: MainViewModel, onDismiss: () -> Unit, onRemove: () -> Unit) {
    val t = LocalTxt.current
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val user = vm.userIdForCommands
    val inCustom = row.pkg in ui.custom

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(row.pkg, row.label, vm, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(row.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    PackageName(row.pkg)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                StateChip(row.state)
                val scheme = MaterialTheme.colorScheme
                when {
                    row.isProtected -> Pill(t.listNever, scheme.errorContainer, scheme.onErrorContainer)
                    row.entry?.kind == ListKind.SAFE -> Pill(t.listSafe, scheme.secondaryContainer, scheme.onSecondaryContainer)
                    row.entry?.kind == ListKind.OPTIONAL -> Pill(t.listOptional, scheme.secondaryContainer, scheme.onSecondaryContainer)
                }
                if (inCustom) Pill(t.listCustom, scheme.secondaryContainer, scheme.onSecondaryContainer)
                if (row.isSystem) Pill(t.systemApp, scheme.surfaceVariant, scheme.onSurfaceVariant)
            }

            val description = row.entry?.description(t.german).orEmpty()
            Text(
                when {
                    description.isNotEmpty() -> description
                    row.pkg in vm.catalog.critical -> t.criticalDescription
                    else -> t.noDescription
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(t.stateExplanation(row.state), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // ---- actions
            if (row.isProtected && row.state == PkgState.ACTIVE) {
                Text(t.protectedHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (row.state) {
                    PkgState.ACTIVE -> if (!row.isProtected) {
                        Button(onClick = { vm.freeze(listOf(row.pkg)); onDismiss() }, enabled = ui.ready) { Text(t.freeze) }
                        OutlinedButton(onClick = { onDismiss(); onRemove() }, enabled = ui.ready) { Text(t.remove) }
                    }
                    PkgState.FROZEN -> {
                        Button(onClick = { vm.thaw(row.pkg); onDismiss() }, enabled = ui.ready) { Text(t.thaw) }
                        if (!row.isProtected) {
                            OutlinedButton(onClick = { onDismiss(); onRemove() }, enabled = ui.ready) { Text(t.remove) }
                        }
                    }
                    PkgState.REMOVED -> Button(onClick = { vm.restore(row.pkg); onDismiss() }, enabled = ui.ready) { Text(t.restore) }
                    PkgState.NOT_PRESENT -> Unit
                }
                if (row.state == PkgState.ACTIVE || row.state == PkgState.FROZEN) {
                    FilledTonalButton(onClick = { openAppSettings(context, row.pkg) }) { Text(t.systemSettings) }
                }
                if (inCustom) TextButton(onClick = { vm.removeCustom(row.pkg); onDismiss() }) { Text(t.removeFromList) }
            }
            if (row.state == PkgState.ACTIVE && !row.isProtected) {
                Text(t.freezeVsRemove, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ---- without Shizuku: how to do the same by hand
            if (!ui.ready && row.state != PkgState.NOT_PRESENT) {
                val command = when (row.state) {
                    PkgState.ACTIVE -> if (row.isProtected) null else "adb shell pm disable-user --user $user ${row.pkg}"
                    PkgState.FROZEN -> "adb shell pm enable --user $user ${row.pkg}"
                    PkgState.REMOVED -> "adb shell pm install-existing --user $user ${row.pkg}"
                    PkgState.NOT_PRESENT -> null
                }
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(t.manualTitle, fontWeight = FontWeight.SemiBold)
                    Text(t.manualBody, style = MaterialTheme.typography.bodyMedium)
                    if (command != null) {
                        Text(command, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = {
                            copyToClipboard(context, command)
                            vm.notify(Message.COPIED)
                        }) { Text(t.copyCommand) }
                    }
                }
            }
        }
    }
}
