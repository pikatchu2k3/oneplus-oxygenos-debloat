package io.github.pikatchu2k3.oneplusdebloat.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pikatchu2k3.oneplusdebloat.core.PkgState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(pkg: String, label: String, vm: MainViewModel, size: Dp = 40.dp) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.IO) { vm.iconOf(pkg)?.asImageBitmap() }
    }
    val shape = RoundedCornerShape(size / 4)
    val current = bitmap
    if (current != null) {
        Image(bitmap = current, contentDescription = null, modifier = Modifier.size(size).clip(shape))
    } else {
        Box(
            modifier = Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StateChip(state: PkgState, modifier: Modifier = Modifier) {
    val t = LocalTxt.current
    val sc = LocalStateColors.current
    val scheme = MaterialTheme.colorScheme
    val (text, bg, fg) = when (state) {
        PkgState.ACTIVE -> Triple(t.stateActive, sc.warn, sc.onWarn)
        PkgState.FROZEN -> Triple(t.stateFrozen, sc.ok, sc.onOk)
        PkgState.REMOVED -> Triple(t.stateRemoved, scheme.tertiaryContainer, scheme.onTertiaryContainer)
        PkgState.NOT_PRESENT -> Triple(t.stateNotPresent, scheme.surfaceVariant, scheme.onSurfaceVariant)
    }
    Pill(text, bg, fg, modifier)
}

@Composable
fun Pill(text: String, background: Color, foreground: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = foreground,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A tinted card with an icon, a title and free content; the building block of most screens. */
@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    body: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            body()
        }
    }
}

@Composable
fun PackageName(pkg: String, modifier: Modifier = Modifier) {
    Text(
        text = pkg,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

fun openAppSettings(context: Context, pkg: String) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

/** Opens another app's launcher activity; returns false if it is not installed. */
fun launchApp(context: Context, pkg: String): Boolean {
    val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
    return try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: Exception) {
        false
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("adb", text))
}
