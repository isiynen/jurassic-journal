package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.game.repository.IngredientNode
import com.sufficienteffort.jurassicjournal.data.model.label
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.ui.components.BadgeChip
import com.sufficienteffort.jurassicjournal.ui.components.DinoThumbnail
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.rarityColor

// ── Hybrid ingredient tree ────────────────────────────────────────────────────

@Composable
internal fun IngredientsSection(
    ingredientTree: List<IngredientNode>,
    ingredientMinLevel: Int,
    onDinoClick: (Long) -> Unit,
) {
    JJCard {
        Column(Modifier.padding(12.dp)) {
            ingredientTree.forEachIndexed { idx, node ->
                if (idx > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                IngredientNodeRow(node = node, depth = 0, minLevel = ingredientMinLevel, onDinoClick = onDinoClick)
            }
        }
    }
}

@Composable
private fun IngredientNodeRow(
    node: IngredientNode,
    depth: Int,
    minLevel: Int,
    onDinoClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(start = (depth * 20).dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onDinoClick(node.dino.id) }
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DinoThumbnail(
                imagePath = node.dino.imagePath,
                contentDescription = node.dino.name,
                size = 48.dp,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    node.dino.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BadgeChip(label = node.dino.rarity.label(), color = rarityColor(node.dino.rarity))
                    BadgeChip(label = "Lv. $minLevel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
            if (node.children.isNotEmpty()) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                )
            }
        }
        if (node.children.isNotEmpty()) {
            Text(
                "Requires:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
            )
            node.children.forEach { child ->
                IngredientNodeRow(node = child, depth = depth + 1, minLevel = node.dino.rarity.minLevel() - 1, onDinoClick = onDinoClick)
            }
        }
    }
}

// ── Used in Hybrids Section ───────────────────────────────────────────────────

@Composable
internal fun HybridsUsingSection(
    hybrids: List<Dino>,
    onDinoClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        hybrids.forEach { hybrid ->
            JJCard(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                elevation = 1.dp,
                cornerRadius = 10.dp,
                onClick = { onDinoClick(hybrid.id) },
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DinoThumbnail(imagePath = hybrid.imagePath, contentDescription = hybrid.name, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        hybrid.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}
