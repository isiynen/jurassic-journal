package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.label
import com.sufficienteffort.jurassicjournal.ui.theme.ClassCunning
import com.sufficienteffort.jurassicjournal.ui.theme.ClassFierce
import com.sufficienteffort.jurassicjournal.ui.theme.ClassResilient
import com.sufficienteffort.jurassicjournal.ui.theme.ClassWildCard
import com.sufficienteffort.jurassicjournal.ui.theme.RarityApex
import com.sufficienteffort.jurassicjournal.ui.theme.RarityCommon
import com.sufficienteffort.jurassicjournal.ui.theme.RarityEpic
import com.sufficienteffort.jurassicjournal.ui.theme.RarityLegendary
import com.sufficienteffort.jurassicjournal.ui.theme.RarityOmega
import com.sufficienteffort.jurassicjournal.ui.theme.RarityRare
import com.sufficienteffort.jurassicjournal.ui.theme.RarityUnique

/**
 * Single source for rarity/class presentation. The theme constants match the
 * in-game frames; every screen must go through these, never a local palette.
 */
fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON    -> RarityCommon
    Rarity.RARE      -> RarityRare
    Rarity.EPIC      -> RarityEpic
    Rarity.LEGENDARY -> RarityLegendary
    Rarity.UNIQUE    -> RarityUnique
    Rarity.OMEGA     -> RarityOmega
    Rarity.APEX      -> RarityApex
}

fun rarityLabel(rarity: Rarity): String = rarity.label()

fun classColor(dinoClass: DinoClass): Color = when (dinoClass) {
    DinoClass.CUNNING, DinoClass.CUNNING_FIERCE, DinoClass.CUNNING_RESILIENT -> ClassCunning
    DinoClass.FIERCE, DinoClass.FIERCE_RESILIENT -> ClassFierce
    DinoClass.RESILIENT -> ClassResilient
    DinoClass.WILD_CARD -> ClassWildCard
}

/** Small tinted pill: translucent [color] background, [color] text. */
@Composable
fun BadgeChip(label: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}
