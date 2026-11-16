package io.github.pikatchu2k3.oneplusdebloat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.pikatchu2k3.oneplusdebloat.shizuku.ShizukuBackend

/** One collapsible help topic. Lines starting with a digit and a dot are rendered as steps. */
data class HelpTopic(val title: String, val paragraphs: List<String>)

const val REPO_URL = "https://github.com/pikatchu2k3/oneplus-oxygenos-debloat"

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HelpScreen(ui: UiState, vm: MainViewModel) {
    val t = LocalTxt.current
    val context = LocalContext.current
    // Open the set-up topic by default while Shizuku is not connected yet.
    var open by rememberSaveable { mutableStateOf(if (ui.ready) -1 else 1) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(t.helpTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(t.helpIntro, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        t.helpTopics.forEachIndexed { index, topic ->
            val expanded = open == index
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { open = if (expanded) -1 else index }.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(topic.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null)
                    }
                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            for (p in topic.paragraphs) Text(p, style = MaterialTheme.typography.bodyMedium)
                            if (index == 1) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = {
                                        if (!launchApp(context, ShizukuBackend.SHIZUKU_PACKAGE)) {
                                            openUrl(context, "https://play.google.com/store/apps/details?id=${ShizukuBackend.SHIZUKU_PACKAGE}")
                                        }
                                    }) { Text(t.openShizuku) }
                                    FilledTonalButton(onClick = { openUrl(context, "https://shizuku.rikka.app/guide/setup/") }) { Text(t.shizukuGuide) }
                                }
                            }
                        }
                    }
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { openUrl(context, REPO_URL) }) { Text(t.openRepo) }
            FilledTonalButton(onClick = { openUrl(context, "$REPO_URL/issues") }) { Text(t.reportProblem) }
        }
        Text(t.disclaimer, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
