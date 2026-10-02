package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.ui.components.DinoImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Column count shared by every dino grid so the fast scrollbar math matches. */
const val DINO_GRID_COLUMNS = 4

// ── Grid name formatter (display only — does not touch DB values) ─────────────

private fun formatGridName(name: String): String = name
    .replace(" Gen 2", " Gen 2")
    .replace(" Gen 3", " Gen 3")
    .replace("Gigantspinosaurus", "Gigantspino­saurus")
    .replace("Monolophosaurus", "Monolopho­saurus")

// ── Grid cell ─────────────────────────────────────────────────────────────────

@Composable
fun DinoGridCell(
    dino: Dino,
    matchedMoves: List<String> = emptyList(),
    isNew: Boolean = false,
    isSelected: Boolean? = null,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        Box {
            DinoImage(
                imagePath = dino.imagePath,
                contentDescription = dino.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            // NEW badge — top-left
            if (isNew) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                ) {
                    Text(
                        "NEW",
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                    )
                }
            }

            // Selection indicator — bottom-right
            if (isSelected != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            // Delete button — top-right (team detail)
            if (showDeleteButton && onDelete != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from team",
                        modifier = Modifier.size(13.dp),
                        tint = Color.White,
                    )
                }
            }
        }

        Text(
            text = formatGridName(dino.name),
            style = MaterialTheme.typography.labelSmall.copy(hyphens = Hyphens.Auto),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, start = 1.dp, end = 1.dp),
        )

        if (matchedMoves.isNotEmpty()) {
            Text(
                text = matchedMoves.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, start = 1.dp, end = 1.dp),
            )
        }
    }
}

// ── Fast scrollbar ────────────────────────────────────────────────────────────

@Composable
fun DinoFastScrollbar(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    columns: Int = DINO_GRID_COLUMNS,
) {
    val coroutineScope = rememberCoroutineScope()

    val thumbFraction by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems == 0) return@derivedStateOf 1f
            val totalRows = ceil(totalItems / columns.toFloat()).toInt()
            val visibleRows = info.visibleItemsInfo.map { it.row }.distinct().size
            (visibleRows.toFloat() / totalRows.coerceAtLeast(1)).coerceIn(0.08f, 1f)
        }
    }

    val scrollFraction by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems == 0) return@derivedStateOf 0f
            val totalRows = ceil(totalItems / columns.toFloat()).toInt()
            val visibleRows = info.visibleItemsInfo.map { it.row }.distinct().size
            val maxScrollRows = (totalRows - visibleRows).coerceAtLeast(1)
            val firstRow = gridState.firstVisibleItemIndex / columns
            (firstRow.toFloat() / maxScrollRows).coerceIn(0f, 1f)
        }
    }

    var showScrollbar by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (gridState.isScrollInProgress || isDragging) {
            showScrollbar = true
        } else {
            delay(3000L)
            showScrollbar = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (showScrollbar && thumbFraction < 1f) 1f else 0f,
        animationSpec = tween(300),
        label = "scrollbar_alpha",
    )

    // rememberUpdatedState keeps gesture handler + draw lambda current without
    // recomposing the Canvas node itself — only the draw phase re-runs per frame.
    val thumbFractionRef = rememberUpdatedState(thumbFraction)
    val scrollFractionRef = rememberUpdatedState(scrollFraction)
    val draggingRef = rememberUpdatedState(isDragging)

    var dragStartY by remember { mutableFloatStateOf(0f) }
    var dragStartFraction by remember { mutableFloatStateOf(0f) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.alpha(alpha)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val curTf = thumbFractionRef.value
                            val curSf = scrollFractionRef.value
                            val trackH = size.height.toFloat()
                            val thumbH = trackH * curTf
                            val thumbTop = (trackH - thumbH) * curSf
                            isDragging = offset.y >= (thumbTop - 24f) &&
                                         offset.y <= (thumbTop + thumbH + 24f)
                            if (isDragging) {
                                dragStartY = offset.y
                                dragStartFraction = curSf
                            }
                        },
                        onDrag = { change, _ ->
                            if (isDragging) {
                                change.consume()
                                val curTf = thumbFractionRef.value
                                val trackH = size.height.toFloat()
                                val thumbH = trackH * curTf
                                val available = (trackH - thumbH).coerceAtLeast(1f)
                                val delta = change.position.y - dragStartY
                                val newFraction = (dragStartFraction + delta / available).coerceIn(0f, 1f)
                                val totalItems = gridState.layoutInfo.totalItemsCount
                                if (totalItems == 0) return@detectDragGestures
                                val totalRows = ceil(totalItems / columns.toFloat()).toInt()
                                val visibleRowsApprox = (curTf * totalRows).roundToInt().coerceAtLeast(1)
                                val maxScrollRows = (totalRows - visibleRowsApprox).coerceAtLeast(1)
                                val targetRow = (newFraction * maxScrollRows).roundToInt()
                                val targetItem = (targetRow * columns).coerceIn(0, totalItems - 1)
                                coroutineScope.launch { gridState.scrollToItem(targetItem) }
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    )
                },
        ) {
            val trackW = 6.dp.toPx()
            val x = (size.width - trackW) / 2f

            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.15f),
                topLeft = Offset(x, 0f),
                size = Size(trackW, size.height),
                cornerRadius = CornerRadius(trackW / 2f),
            )

            val tf = thumbFractionRef.value
            val sf = scrollFractionRef.value
            val thumbH = (size.height * tf).coerceAtLeast(trackW * 2f)
            val thumbTop = (size.height - thumbH) * sf
            drawRoundRect(
                color = primaryColor.copy(alpha = if (draggingRef.value) 0.90f else 0.55f),
                topLeft = Offset(x, thumbTop),
                size = Size(trackW, thumbH),
                cornerRadius = CornerRadius(trackW / 2f),
            )
        }
    }
}
