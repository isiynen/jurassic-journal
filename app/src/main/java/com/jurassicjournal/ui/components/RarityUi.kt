package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.ui.graphics.Color
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.ui.theme.RarityApex
import com.sufficienteffort.jurassicjournal.ui.theme.RarityCommon
import com.sufficienteffort.jurassicjournal.ui.theme.RarityEpic
import com.sufficienteffort.jurassicjournal.ui.theme.RarityLegendary
import com.sufficienteffort.jurassicjournal.ui.theme.RarityOmega
import com.sufficienteffort.jurassicjournal.ui.theme.RarityRare
import com.sufficienteffort.jurassicjournal.ui.theme.RarityUnique

/**
 * Single source for rarity presentation. The calculator screens used to carry
 * their own hardcoded palettes, so the same rarity rendered different colors
 * on different screens; the theme constants (matching the in-game frames) win.
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

fun rarityLabel(rarity: Rarity): String = when (rarity) {
    Rarity.COMMON    -> "Common"
    Rarity.RARE      -> "Rare"
    Rarity.EPIC      -> "Epic"
    Rarity.LEGENDARY -> "Legendary"
    Rarity.UNIQUE    -> "Unique"
    Rarity.APEX      -> "Apex"
    Rarity.OMEGA     -> "Omega"
}
