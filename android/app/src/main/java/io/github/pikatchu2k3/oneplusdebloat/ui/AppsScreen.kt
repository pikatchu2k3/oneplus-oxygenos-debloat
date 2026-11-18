package io.github.pikatchu2k3.oneplusdebloat.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppsScreen(
    ui: UiState,
    vm: MainViewModel,
    listTab: Int,
    onListTab: (Int) -> Unit,
    onDetail: (String) -> Unit,
) {
    val t = LocalTxt.current
    val context = LocalContext.current
    var showMissing by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }

    val ids = when (listTab) {
        0 -> vm.safeIds
        1 -> vm.optionalIds
        2 -> ui.custom
        3 -> vm.cautionIds
        else -> ui.deviceFrozen + ui.deviceRemoved
    }
    val all = ui.rowsOf(ids)
    val present = all.filter { it.state != PkgState.NOT_PRESENT }
    val shown = (if (showMissing) all else present).filter {
        query.isBlank() || it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true)
    }
    val selectedHere = ui.selected.filter { it in ids }
    val selectable = if (listTab < 3) present.filter { it.bulkSelectable } else emptyList()

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportCustom().toByteArray()) }
            } catch (_: Exception) {
                vm.notify(Message.IMPORT_FAILED)
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes().take(1_000_000).toByteArray().decodeToString() }
            } catch (_: Exception) {
                null
            }
            vm.importCustom(text ?: "")
        }
    }

    Column(Modifier.fillMaxSize()) {
        ScrollableTabRow(selectedTabIndex = listTab, edgePadding = 8.dp) {
            val titles = listOf(t.listSafe, t.listOptional, t.listCustom, t.listCaution, t.listOff)
            titles.forEachIndexed { index, title ->
                Tab(selected = listTab == index, onClick = { onListTab(index) }, text = { Text(title) })
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(t.listIntro(listTab), style = MaterialTheme.typography.bodyMedium)
                    if (listTab == 2) {
                        // Wraps on narrow screens or large font sizes instead of squeezing "Export" into a column.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Button(onClick = vm::loadPicker) {
                                Icon(Icons.Filled.Add, null)
                                Spacer(Modifier.width(6.dp))
                                Text(t.addApps)
                            }
                            TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text(t.importList, maxLines = 1) }
                            TextButton(
                                onClick = { exportLauncher.launch("oneplus-cleanup-liste.json") },
                                enabled = ui.custom.isNotEmpty(),
                            ) { Text(t.exportList, maxLines = 1) }
                        }
                    }
                    if (all.size > 6) {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, null) },
                            trailingIcon = {
                                if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, t.cancel) }
                            },
                            placeholder = { Text(t.searchHint) },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (all.size > present.size) {
                            FilterChip(
                                selected = showMissing, onClick = { showMissing = !showMissing },
                                label = { Text(t.showMissing(all.size - present.size)) },
                            )
                        }
                        if (selectable.isNotEmpty()) {
                            TextButton(onClick = {
                                if (selectedHere.size == selectable.size) vm.deselect(selectedHere)
                                else vm.selectAllActive(ids)
                            }) { Text(if (selectedHere.size == selectable.size) t.selectNone else t.selectAllActive(selectable.size)) }
                        }
                    }
                    Text(t.listSummary(present.size, all.size), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
            if (!ui.loaded) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            } else if (shown.isEmpty()) {
                item {
                    Text(
                        if (listTab == 2 && all.isEmpty()) t.customEmpty else t.listEmpty,
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(shown, key = { it.pkg }) { row ->
                AppRowItem(
                    row = row, vm = vm,
                    checked = row.pkg in ui.selected,
                    checkable = listTab < 3 && row.bulkSelectable,
                    onToggle = { vm.toggle(row.pkg) },
                    onClick = { onDetail(row.pkg) },
                )
            }
        }

        // The selection is kept across the tabs, so one tap freezes picks from several lists.
        if (ui.selected.isNotEmpty()) {
            Surface(tonalElevation = 6.dp, shadowElevation = 8.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.selectedCount(ui.selected.size), modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        TextButton(onClick = vm::clearSelection) { Text(t.selectNone) }
                        Button(onClick = { vm.freeze(ui.selected.toList()) }, enabled = ui.ready) { Text(t.freeze) }
                    }
                    if (!ui.ready) Text(t.needsShellShort, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    ui.picker?.let { apps ->
        PickerDialog(apps = apps, already = ui.custom.toSet(), vm = vm, onClose = vm::closePicker)
    }
}

@Composable
private fun AppRowItem(
    row: AppRow,
    vm: MainViewModel,
    checked: Boolean,
    checkable: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 4.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checkable) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        } else {
            Box(Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                when {
                    row.isProtected -> Icon(Icons.Filled.Lock, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    row.state == PkgState.ACTIVE && (row.isCaution || row.defaultRole != null) ->
                        Icon(Icons.Filled.Warning, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        AppIcon(row.pkg, row.label, vm)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(row.label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            PackageName(row.pkg)
        }
        Spacer(Modifier.width(8.dp))
        StateChip(row.state)
    }
}

/** State filter of the "add apps" picker. REMOVED also covers apps hidden by a device owner. */
enum class PickerFilter(val states: Set<PkgState>) {
    ALL(emptySet()),
    ACTIVE(setOf(PkgState.ACTIVE)),
    FROZEN(setOf(PkgState.FROZEN)),
    SUSPENDED(setOf(PkgState.SUSPENDED)),
    REMOVED(setOf(PkgState.REMOVED, PkgState.HIDDEN));

    fun matches(state: PkgState) = this == ALL || state in states
}

/** Origin filter of the picker: everything, only apps the user installed, or only pre-installed ones. */
enum class PickerSource {
    ALL, USER, SYSTEM;

    fun matches(isSystem: Boolean) = when (this) {
        ALL -> true
        USER -> !isSystem
        SYSTEM -> isSystem
    }
}

/** Full-screen chooser for adding any installed app to the user's own list. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PickerDialog(apps: List<PickerApp>, already: Set<String>, vm: MainViewModel, onClose: () -> Unit) {
    val t = LocalTxt.current
    var query by remember { mutableStateOf("") }
    // "All" by default: switched-off apps are nearly always system apps and should not hide behind a switch.
    var source by remember { mutableStateOf(PickerSource.ALL) }
    var filter by remember { mutableStateOf(PickerFilter.ALL) }
    var picked by remember { mutableStateOf(setOf<String>()) }
    var manual by remember { mutableStateOf("") }
    // Search applies to every chip; each chip counts exactly what tapping it shows.
    val searched = apps.filter {
        it.pkg !in already && (query.isBlank() || it.label.contains(query, true) || it.pkg.contains(query, true))
    }
    // Each row of chips counts what tapping it would show, given the other row's choice.
    val stateCounts = PickerFilter.entries.associateWith { f -> searched.count { f.matches(it.state) && source.matches(it.isSystem) } }
    val sourceCounts = PickerSource.entries.associateWith { s -> searched.count { s.matches(it.isSystem) && filter.matches(it.state) } }
    val shown = searched.filter { filter.matches(it.state) && source.matches(it.isSystem) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.padding(start = 4.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, t.cancel) }
                    Text(t.pickerTitle, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    Button(
                        onClick = { vm.addCustom(picked); onClose() },
                        enabled = picked.isNotEmpty(),
                    ) { Text(t.addCount(picked.size)) }
                }
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(t.pickerIntro, style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = query, onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        placeholder = { Text(t.searchHint) },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (s in PickerSource.entries) {
                            FilterChip(
                                selected = source == s,
                                onClick = { source = s },
                                label = { Text(t.pickerSource(s, sourceCounts.getValue(s))) },
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (f in PickerFilter.entries) {
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = { Text(t.pickerFilter(f, stateCounts.getValue(f))) },
                            )
                        }
                    }
                }
                HorizontalDivider()
                LazyColumn(Modifier.weight(1f)) {
                    items(shown, key = { it.pkg }) { app ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                picked = if (app.pkg in picked) picked - app.pkg else picked + app.pkg
                            }.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = app.pkg in picked, onCheckedChange = {
                                picked = if (app.pkg in picked) picked - app.pkg else picked + app.pkg
                            })
                            AppIcon(app.pkg, app.label, vm, size = 36.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                PackageName(app.pkg)
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (app.state != PkgState.ACTIVE) StateChip(app.state)
                                if (app.isSystem) Pill(t.systemApp, MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    item {
                        // For packages that do not show up, e.g. ones already removed for the user.
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(t.manualAddTitle, fontWeight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = manual, onValueChange = { manual = it.trim() },
                                    modifier = Modifier.weight(1f), singleLine = true,
                                    placeholder = { Text("com.example.app") },
                                )
                                FilledTonalButton(
                                    onClick = { vm.addCustom(listOf(manual)); manual = ""; onClose() },
                                    enabled = io.github.pikatchu2k3.oneplusdebloat.core.isValidPackageName(manual),
                                ) { Text(t.add) }
                            }
                        }
                    }
                }
            }
        }
    }
}
