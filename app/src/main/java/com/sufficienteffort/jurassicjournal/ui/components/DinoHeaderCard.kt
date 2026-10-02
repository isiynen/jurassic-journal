package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino

/** Thumbnail + name + rarity row at the top of every calculator screen. */
@Composable
fun DinoHeaderCard(dino: Dino, onClick: (() -> Unit)? = null) {
    JJCard(modifier = Modifier.cardMargin(vertical = 8.dp), onClick = onClick) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DinoThumbnail(
                imagePath = dino.imagePath,
                contentDescription = dino.name,
                size = 56.dp,
                cornerRadius = 8.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(dino.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = rarityLabel(dino.rarity),
                    style = MaterialTheme.typography.labelSmall,
                    color = rarityColor(dino.rarity),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
