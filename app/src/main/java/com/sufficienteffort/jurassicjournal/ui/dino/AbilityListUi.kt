package com.sufficienteffort.jurassicjournal.ui.dino

import com.sufficienteffort.jurassicjournal.ui.components.formatAbilityName
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilityAction
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilityEntry
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilitySortMode
import com.sufficienteffort.jurassicjournal.data.game.repository.ActionCategory
import com.sufficienteffort.jurassicjournal.data.model.MovePriorityType
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType
import com.sufficienteffort.jurassicjournal.data.model.displayName
import com.sufficienteffort.jurassicjournal.data.model.label
import com.sufficienteffort.jurassicjournal.ui.theme.SecureGreen
import com.sufficienteffort.jurassicjournal.ui.theme.ThreatenedRed

// ── Mode icons (hand-drawn: the core icon set has no dino/bolt glyphs) ───────────

/** Dino footprint, lightning bolt, or bolt over footprint, drawn on a 24-unit grid. */
@Composable
fun ModeIcon(mode: ListMode, tint: Color, modifier: Modifier = Modifier, iconSize: androidx.compose.ui.unit.Dp = 24.dp) {
    Canvas(modifier.size(iconSize)) {
        when (mode) {
            ListMode.DINOS -> drawFootprint(tint)
            ListMode.ABILITIES_ALL -> drawBolt(tint)
            ListMode.ABILITIES_SCOPED -> {
                drawFootprint(tint.copy(alpha = 0.45f))
                drawBolt(tint, scale = 0.8f)
            }
        }
    }
}

/** Theropod track: three clawed toes splaying from a heel pad, the middle toe longest. */
private fun DrawScope.drawFootprint(color: Color) {
    val u = size.minDimension / 24f
    // (base x, base y, angle from straight ahead in degrees, length, width)
    data class Toe(val x: Float, val y: Float, val deg: Float, val len: Float, val width: Float)
    listOf(
        Toe(7.4f, 15.5f, -28f, 10f, 3.6f),
        Toe(12f, 14.5f, 0f, 12.5f, 4.0f),
        Toe(16.6f, 15.5f, 28f, 10f, 3.6f),
    ).forEach { t ->
        val a = Math.toRadians(t.deg.toDouble())
        val dx = Math.sin(a).toFloat()
        val dy = -Math.cos(a).toFloat()
        val px = -dy
        val py = dx
        val half = t.width / 2
        val shoulder = t.len * 0.72f
        val path = Path().apply {
            moveTo((t.x + px * half) * u, (t.y + py * half) * u)
            lineTo((t.x + px * half + dx * shoulder) * u, (t.y + py * half + dy * shoulder) * u)
            lineTo((t.x + dx * t.len) * u, (t.y + dy * t.len) * u)
            lineTo((t.x - px * half + dx * shoulder) * u, (t.y - py * half + dy * shoulder) * u)
            lineTo((t.x - px * half) * u, (t.y - py * half) * u)
            close()
        }
        drawPath(path, color)
        drawCircle(color, radius = half * u, center = Offset(t.x * u, t.y * u))
    }
    // Heel pad joining the toes
    drawOval(
        color,
        topLeft = Offset(7.2f * u, 15f * u),
        size = Size(9.6f * u, 7.4f * u),
    )
}

private fun DrawScope.drawBolt(color: Color, scale: Float = 1f) {
    val u = size.minDimension / 24f * scale
    val off = size.minDimension * (1f - scale) / 2f
    fun p(x: Float, y: Float) = Offset(off + x * u, off + y * u)
    val path = Path().apply {
        moveTo(p(13.5f, 1.5f).x, p(13.5f, 1.5f).y)
        lineTo(p(4.5f, 13.5f).x, p(4.5f, 13.5f).y)
        lineTo(p(11f, 13.5f).x, p(11f, 13.5f).y)
        lineTo(p(10f, 22.5f).x, p(10f, 22.5f).y)
        lineTo(p(19.5f, 10f).x, p(19.5f, 10f).y)
        lineTo(p(13f, 10f).x, p(13f, 10f).y)
        close()
    }
    drawPath(path, color)
}

// ── Mode picker bottom sheet ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModePickerSheet(
    current: ListMode,
    scopeDinoCount: Int,
    onSelect: (ListMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Browse", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ModeOption(
                mode = ListMode.DINOS,
                title = "Dinos",
                subtitle = "Search and sort the dinosaur list",
                selected = current == ListMode.DINOS,
                onClick = { onSelect(ListMode.DINOS) },
            )
            ModeOption(
                mode = ListMode.ABILITIES_ALL,
                title = "All abilities",
                subtitle = "Search every ability in the game",
                selected = current == ListMode.ABILITIES_ALL,
                onClick = { onSelect(ListMode.ABILITIES_ALL) },
            )
            ModeOption(
                mode = ListMode.ABILITIES_SCOPED,
                title = "Abilities of filtered dinos",
                subtitle = "Only abilities owned by the " +
                    (if (scopeDinoCount == 1) "1 dino" else "$scopeDinoCount dinos") +
                    " your dino filters return",
                selected = current == ListMode.ABILITIES_SCOPED,
                onClick = { onSelect(ListMode.ABILITIES_SCOPED) },
            )
        }
    }
}

@Composable
private fun ModeOption(mode: ListMode, title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) accent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) BorderStroke(1.5.dp, accent) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                ModeIcon(mode, tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant, iconSize = 26.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) Icon(Icons.Default.Check, contentDescription = "Selected", tint = accent)
        }
    }
}

// ── One-time tip ──────────────────────────────────────────────────────────────

/** Dismissible tip shown under the search bar until the user has seen it once. */
@Composable
fun ModeSwitchTip(onDismiss: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Tip: tap the icon in the search bar to switch between searching dinos and abilities.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Got it") }
        }
    }
}

// ── Scope banner ──────────────────────────────────────────────────────────────

/** Human-readable summary of the dino filters that define the ability scope. */
fun FilterState.scopeSummary(): String {
    val parts = buildList {
        if (query.isNotBlank()) add("“${query.trim()}”")
        addAll(rarities.sortedBy { it.ordinal }.map { it.label() })
        addAll(dinoClasses.map { it.label() })
        addAll(locations.map { it.displayName() })
        if (newOnly) add("New")
    }
    return if (parts.isEmpty()) "no filters" else parts.joinToString(" · ")
}

@Composable
fun ScopeBanner(
    dinoCount: Int,
    summary: String,
    onEditFilters: () -> Unit,
    onClearScope: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 4.dp)
            .clickable(onClick = onEditFilters),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ModeIcon(
                ListMode.ABILITIES_SCOPED,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                iconSize = 18.dp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Limited to ${if (dinoCount == 1) "1 dino" else "$dinoCount dinos"} · $summary",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onClearScope, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Search all abilities",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ── Chip rows ─────────────────────────────────────────────────────────────────

fun MoveTriggerType.abilityLabel(): String = when (this) {
    MoveTriggerType.SELECTABLE -> "Regular"
    else -> label()
}

private fun MovePriorityType.abilityLabel(): String = when (this) {
    MovePriorityType.PRIORITY -> "Priority"
    MovePriorityType.NORMAL -> "No Priority"
    MovePriorityType.LAST -> "Act Last"
}

/**
 * Trigger chips (OR) then priority chips (OR) in one row to save height. "All" clears both.
 * Each group is alphabetised on its own.
 */
@Composable
fun TriggerPriorityRow(
    triggers: Set<MoveTriggerType>,
    priorities: Set<MovePriorityType>,
    onTriggerToggle: (MoveTriggerType) -> Unit,
    onPriorityToggle: (MovePriorityType) -> Unit,
    onClear: () -> Unit,
) {
    val triggerOptions = remember { MoveTriggerType.entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.abilityLabel() }) }
    val priorityOptions = remember { MovePriorityType.entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.abilityLabel() }) }
    ChipRow(compact = true) {
        item { FilterChip(selected = triggers.isEmpty() && priorities.isEmpty(), onClick = onClear, label = { Text("All") }) }
        items(triggerOptions, key = { "t_${it.name}" }) { t ->
            FilterChip(selected = t in triggers, onClick = { onTriggerToggle(t) }, label = { Text(t.abilityLabel()) })
        }
        items(priorityOptions, key = { "p_${it.name}" }) { p ->
            FilterChip(selected = p in priorities, onClick = { onPriorityToggle(p) }, label = { Text(p.abilityLabel()) })
        }
    }
}

@Composable
fun TargetFilterRow(options: List<String>, selected: Set<String>, onToggle: (String) -> Unit, onClear: () -> Unit) {
    ChipRow(compact = true) {
        item { FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text("All") }) }
        items(options) { t ->
            FilterChip(selected = t in selected, onClick = { onToggle(t) }, label = { Text(t) })
        }
    }
}

@Composable
private fun categoryColor(category: ActionCategory): Color = when (category) {
    ActionCategory.BASIC -> MaterialTheme.colorScheme.primary
    ActionCategory.BYPASS -> MaterialTheme.colorScheme.tertiary
    ActionCategory.POSITIVE -> SecureGreen
    ActionCategory.NULLIFY -> Color(0xFFAB47BC)
    ActionCategory.NEGATIVE -> ThreatenedRed
    ActionCategory.CLEANSE -> Color(0xFF26A69A)
}

/**
 * One scrolling row for one or more action categories: "All" first, then each category's chips
 * alphabetised as its own run. Only options that exist in the data are shown.
 */
@Composable
fun ActionFilterRow(
    categories: List<ActionCategory>,
    available: Set<AbilityAction>,
    selected: Set<AbilityAction>,
    onToggle: (AbilityAction) -> Unit,
    onClear: () -> Unit,
) {
    val groups = remember(categories, available) {
        categories.map { c -> c to AbilityAction.inCategory(c).filter { it in available } }
            .filter { it.second.isNotEmpty() }
    }
    if (groups.isEmpty()) return
    val noneSelected = selected.none { it.category in categories }
    ChipRow(compact = true) {
        item(key = "all") { FilterChip(selected = noneSelected, onClick = onClear, label = { Text("All") }) }
        groups.forEach { (category, actions) ->
            items(actions, key = { it.name }) { action ->
                TintedChip(action.label, action in selected, categoryColor(category)) { onToggle(action) }
            }
        }
    }
}

@Composable
fun AbilitySortRow(selected: AbilitySortMode?, onSelect: (AbilitySortMode?) -> Unit) {
    val sortModes = remember { AbilitySortMode.entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label }) }
    ChipRow(compact = true) {
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Reset") }) }
        items(sortModes) { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(if (selected == mode) null else mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

// ── Grid cell ─────────────────────────────────────────────────────────────────

@Composable
fun AbilityGridCell(entry: AbilityEntry, onClick: () -> Unit) {
    val overlays = remember(entry.overlayIconsJson) { parseOverlays(entry.overlayIconsJson) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(2.dp),
    ) {
        BoxWithIcon(entry, overlays)
        Text(
            text = formatAbilityName(entry.name),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp, start = 1.dp, end = 1.dp),
        )
        if (entry.threatened) {
            Text(
                text = "Threatened",
                style = MaterialTheme.typography.labelSmall,
                color = ThreatenedRed,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BoxWithIcon(entry: AbilityEntry, overlays: List<IconOverlay>) {
    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxWidth()) {
        AbilityIcon(
            mainPath = entry.mainIconPath,
            overlays = overlays,
            size = maxWidth,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)),
        )
    }
}
