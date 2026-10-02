package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.sufficienteffort.jurassicjournal.data.game.repository.DinoMoveDetail
import com.sufficienteffort.jurassicjournal.data.game.repository.MoveVariant
import com.sufficienteffort.jurassicjournal.data.game.repository.ParsedTarget
import com.sufficienteffort.jurassicjournal.data.model.MovePriorityType
import com.sufficienteffort.jurassicjournal.data.model.MoveTriggerType
import com.sufficienteffort.jurassicjournal.data.model.MoveUnlockType
import com.sufficienteffort.jurassicjournal.data.model.label
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.abilityIconRequest
import com.sufficienteffort.jurassicjournal.ui.theme.BadgeLastBg
import com.sufficienteffort.jurassicjournal.ui.theme.BadgePriorityBg
import com.sufficienteffort.jurassicjournal.ui.theme.BadgeUnlockBg
import com.sufficienteffort.jurassicjournal.ui.theme.BadgeUnlockFg
import com.sufficienteffort.jurassicjournal.ui.theme.SecureGreen
import com.sufficienteffort.jurassicjournal.ui.theme.ThreatenedRed
import org.json.JSONArray
import kotlin.math.roundToInt

private val ATTACK_MULT_REGEX = Regex("""(?i)attack\s+([\d.]+)x""")

// ── Icon overlay model ────────────────────────────────────────────────────────

private enum class OverlayPosition(val alignment: Alignment) {
    TOP_LEFT(Alignment.TopStart),
    TOP_RIGHT(Alignment.TopEnd),
    BOTTOM_LEFT(Alignment.BottomStart),
    BOTTOM_RIGHT(Alignment.BottomEnd);

    companion object {
        fun fromKey(key: String): OverlayPosition? = when (key) {
            "top_left"     -> TOP_LEFT
            "top_right"    -> TOP_RIGHT
            "bottom_left"  -> BOTTOM_LEFT
            "bottom_right" -> BOTTOM_RIGHT
            else           -> null
        }
    }
}

private data class IconOverlay(val rawPath: String, val position: OverlayPosition)

private fun parseOverlays(json: String?): List<IconOverlay> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val rawPath = obj.optString("path").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val pos = OverlayPosition.fromKey(obj.optString("position")) ?: return@mapNotNull null
            IconOverlay(rawPath, pos)
        }
    } catch (_: Exception) { emptyList() }
}

// ── Moves Panel ───────────────────────────────────────────────────────────────

@Composable
internal fun MovesPanel(
    movesByTrigger: Map<MoveTriggerType, List<DinoMoveDetail>>,
    computedAttack: Int,
    reactiveMoveLocked: Boolean = false,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        movesByTrigger.forEach { (trigger, moves) ->
            if (moves.isEmpty()) return@forEach
            if (trigger != MoveTriggerType.SELECTABLE) {
                Text(
                    trigger.label(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                if (trigger == MoveTriggerType.REACTIVE && reactiveMoveLocked) {
                    Text(
                        "Enable E5 to unlock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            val isDimmed = trigger == MoveTriggerType.REACTIVE && reactiveMoveLocked
            moves.forEach { detail ->
                MoveCard(detail, computedAttack, isDimmed)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MoveCard(detail: DinoMoveDetail, computedAttack: Int, isDimmed: Boolean = false) {
    val overlays = remember(detail.move.overlayIconsJson) {
        parseOverlays(detail.move.overlayIconsJson)
    }
    val threatened = detail.threatened

    JJCard(
        modifier = Modifier.fillMaxWidth().alpha(if (isDimmed) 0.5f else 1f),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        elevation = 1.dp,
        cornerRadius = 10.dp,
    ) {
        Column(Modifier.padding(12.dp)) {

            // Icon (with overlays) + name + priority badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AbilityIcon(mainPath = detail.move.mainIconPath, overlays = overlays)
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            detail.move.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (detail.dinoMove.unlockType == MoveUnlockType.LEVEL) {
                            val lv = detail.dinoMove.unlockValue ?: "?"
                            MoveBadge("LV$lv", BadgeUnlockBg, BadgeUnlockFg)
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        PriorityBadge(detail.secure.priority)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (threatened != null) {
                VariantBlock("Secure",     detail.secure, SecureGreen,   computedAttack)
                Spacer(Modifier.height(8.dp))
                VariantBlock("Threatened", threatened,    ThreatenedRed, computedAttack)
            } else {
                TargetsList(detail.secure.targets, computedAttack)
                CooldownDelayRow(detail.secure.cooldown, detail.secure.delay)
            }
        }
    }
}

// ── Ability icon with corner overlays ────────────────────────────────────────

@Composable
private fun AbilityIcon(mainPath: String?, overlays: List<IconOverlay>) {
    Box(modifier = Modifier.size(48.dp)) {
        if (mainPath != null) {
            AsyncImage(
                model = abilityIconRequest(mainPath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        overlays.forEach { overlay ->
            AsyncImage(
                model = abilityIconRequest(overlay.rawPath),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .align(overlay.position.alignment),
            )
        }
    }
}

// ── Variant block (Secure / Threatened) ───────────────────────────────────────

@Composable
private fun VariantBlock(
    label: String,
    variant: MoveVariant,
    labelColor: Color,
    computedAttack: Int,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor,
            )
            PriorityBadge(variant.priority)
        }
        Spacer(Modifier.height(4.dp))
        TargetsList(variant.targets, computedAttack)
        CooldownDelayRow(variant.cooldown, variant.delay)
    }
}

// ── Effects list ─────────────────────────────────────────────────────────────

@Composable
private fun TargetsList(targets: List<ParsedTarget>, computedAttack: Int) {
    targets.forEach { target ->
        Text(
            "Target: ${target.target}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
        target.effects.forEach { effect ->
            Text(
                "  • ${enrichEffect(effect, computedAttack)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Appends "(XXXX dmg)" when the effect contains an attack multiplier like "Attack 1.5X". */
internal fun enrichEffect(effect: String, computedAttack: Int): String {
    val match = ATTACK_MULT_REGEX.find(effect) ?: return effect
    val multiplier = match.groupValues[1].toFloatOrNull() ?: return effect
    val damage = (multiplier * computedAttack).roundToInt()
    return "${effect.trimEnd()} ($damage dmg)"
}

// ── Cooldown / delay footer ───────────────────────────────────────────────────

@Composable
private fun CooldownDelayRow(cooldown: Int, delay: Int) {
    if (cooldown <= 0 && delay <= 0) return
    Row(
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cooldown > 0) FooterLabel("Cooldown: $cooldown")
        if (delay > 0) FooterLabel("Delay: $delay")
    }
}

@Composable
private fun FooterLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
    )
}

// ── Badges ────────────────────────────────────────────────────────────────────

@Composable
private fun PriorityBadge(priority: MovePriorityType) {
    when (priority) {
        MovePriorityType.PRIORITY -> MoveBadge("Priority", BadgePriorityBg, Color.White)
        MovePriorityType.LAST     -> MoveBadge("Act Last", BadgeLastBg, Color.White)
        MovePriorityType.NORMAL   -> Unit
    }
}

@Composable
private fun MoveBadge(label: String, background: Color, foreground: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = foreground)
    }
}
