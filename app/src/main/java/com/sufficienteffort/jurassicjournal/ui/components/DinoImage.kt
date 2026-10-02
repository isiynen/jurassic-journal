package com.sufficienteffort.jurassicjournal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.sufficienteffort.jurassicjournal.data.update.abilityIconModel
import com.sufficienteffort.jurassicjournal.data.update.dinoImageModel

/** Coil request for a dino portrait resolved through downloaded → bundled → GitHub. */
@Composable
fun dinoImageRequest(imagePath: String): ImageRequest {
    val context = LocalContext.current
    return remember(imagePath) {
        ImageRequest.Builder(context)
            .data(dinoImageModel(context, imagePath))
            .crossfade(false)
            .build()
    }
}

/** Coil request for an ability icon (main or overlay) by its DB path. */
@Composable
fun abilityIconRequest(rawPath: String): ImageRequest {
    val context = LocalContext.current
    return remember(rawPath) {
        ImageRequest.Builder(context)
            .data(abilityIconModel(context, rawPath))
            .crossfade(false)
            .build()
    }
}

/** Dino portrait with no framing; caller sizes it. */
@Composable
fun DinoImage(
    imagePath: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    AsyncImage(
        model = dinoImageRequest(imagePath),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}

/** Square dino thumbnail: rounded, on a surfaceVariant backdrop. */
@Composable
fun DinoThumbnail(
    imagePath: String,
    contentDescription: String?,
    size: Dp,
    cornerRadius: Dp = 6.dp,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier,
) {
    DinoImage(
        imagePath = imagePath,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}
