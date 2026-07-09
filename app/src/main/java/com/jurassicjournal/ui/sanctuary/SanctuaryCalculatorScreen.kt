package com.sufficienteffort.jurassicjournal.ui.sanctuary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sufficienteffort.jurassicjournal.data.update.dinoImageModel
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.util.StatCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuaryCalculatorScreen(
    onBack: () -> Unit,
    viewModel: SanctuaryCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.dino != null) "${uiState.dino!!.name} — Sanctuary"
                               else "Sanctuary Calculator",
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        val dino = uiState.dino ?: return@Scaffold
        val minLevel = dino.rarity.minLevel()

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            // ── Dino header ──────────────────────────────────────────────────
            item {
                DinoBadgeCard(dino = dino)
            }

            // ── Dino configuration ───────────────────────────────────────────
            item { SectionHeader("Dino Configuration") }
            item {
                LevelAndBoostCard(
                    level        = uiState.level,
                    boosts       = uiState.boosts,
                    minLevel     = minLevel,
                    onLevelChange       = viewModel::setLevel,
                    onSpeedChange       = viewModel::setSpeedBoosts,
                    onAttackChange      = viewModel::setAttackBoosts,
                    onHealthChange      = viewModel::setHealthBoosts,
                )
            }
            item {
                HintText("Speed boosts have the greatest impact on sanctuary points.")
            }

            // ── SP contribution ──────────────────────────────────────────────
            item { SectionHeader("Sanctuary Points (SP) Per Interaction") }
            item {
                SpContributionCard(
                    estimatedSp = uiState.estimatedSpPerAction,
                )
            }

        }
    }
}

// ── Dino badge ────────────────────────────────────────────────────────────────

@Composable
private fun DinoBadgeCard(dino: com.sufficienteffort.jurassicjournal.data.game.entity.Dino) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dinoImageModel(LocalContext.current, dino.imagePath))
                    .crossfade(false)
                    .build(),
                contentDescription = dino.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(dino.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    rarityLabel(dino.rarity),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = rarityColor(dino.rarity),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Level + boost card ────────────────────────────────────────────────────────

@Composable
private fun LevelAndBoostCard(
    level: Int,
    boosts: SanctuaryBoostConfig,
    minLevel: Int,
    onLevelChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onAttackChange: (Int) -> Unit,
    onHealthChange: (Int) -> Unit,
) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Level row
            ConfigRow(label = "Level") {
                CompactStepper(
                    title  = "Level",
                    value  = level,
                    min    = minLevel,
                    max    = 35,
                    onValueChange = onLevelChange,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )
            // Boost rows
            ConfigRow(label = "Health boosts") {
                CompactStepper(title = "Health boosts", value = boosts.health, min = 0, max = 20, onValueChange = onHealthChange)
            }
            Spacer(Modifier.height(8.dp))
            ConfigRow(label = "Attack boosts") {
                CompactStepper(title = "Attack boosts", value = boosts.attack, min = 0, max = 20, onValueChange = onAttackChange)
            }
            Spacer(Modifier.height(8.dp))
            ConfigRow(label = "Speed boosts") {
                CompactStepper(title = "Speed boosts", value = boosts.speed, min = 0, max = 20, onValueChange = onSpeedChange)
            }
            if (boosts.total > 0) {
                Spacer(Modifier.height(10.dp))
                Text(
                    "Total boosts: ${boosts.total} / ${StatCalculator.maxTotalBoosts(level)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ConfigRow(
    label: String,
    content: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        content()
    }
}

@Composable
private fun CompactStepper(
    title: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title     = title,
            current   = value,
            min       = min,
            max       = max,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        RepeatingButton(
            onClick  = { if (value > min) onValueChange(value - 1) },
            enabled  = value > min,
            modifier = Modifier.size(32.dp),
        ) { Text("−", style = MaterialTheme.typography.titleMedium) }
        Text(
            value.toString(),
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier   = Modifier.width(36.dp).clickable { showDialog = true },
            textAlign  = TextAlign.Center,
        )
        RepeatingButton(
            onClick  = { if (value < max) onValueChange(value + 1) },
            enabled  = value < max,
            modifier = Modifier.size(32.dp),
        ) { Text("+", style = MaterialTheme.typography.titleMedium) }
    }
}

@Composable
private fun RepeatingButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val currentEnabled by rememberUpdatedState(enabled)
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier.pointerInput(Unit) {
            while (true) {
                awaitPointerEventScope { awaitFirstDown(requireUnconsumed = false) }
                if (!currentEnabled) continue
                currentOnClick()
                val job = scope.launch {
                    delay(400L)
                    while (currentEnabled) {
                        currentOnClick()
                        delay(80L)
                    }
                }
                awaitPointerEventScope { waitForUpOrCancellation() }
                job.cancel()
            }
        },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NumberInputDialog(
    title: String,
    current: Int,
    min: Int,
    max: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialText = current.toString()
    var fieldValue by remember {
        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(0, initialText.length)))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = fieldValue,
                onValueChange = { new ->
                    fieldValue = new.copy(text = new.text.filter { it.isDigit() })
                },
                label = { Text("$min – $max") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = {
                    val v = fieldValue.text.toIntOrNull()?.coerceIn(min, max) ?: current
                    onConfirm(v)
                }),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val v = fieldValue.text.toIntOrNull()?.coerceIn(min, max) ?: current
                onConfirm(v)
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

// ── SP contribution card ──────────────────────────────────────────────────────

@Composable
private fun SpContributionCard(
    estimatedSp: Int?,
) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SpResultRow(
                label = "Your estimated SP / action",
                value = estimatedSp,
            )
            Spacer(Modifier.height(8.dp))
            SpResultRow(
                label = "Daily SP (3 interactions)",
                value = estimatedSp?.let { it * 3 },
            )
        }
    }
}

@Composable
private fun SpResultRow(label: String, value: Int?) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f),
        )
        Text(
            value?.toString() ?: "—",
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = if (value != null) MaterialTheme.colorScheme.onSurface
                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color    = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun HintText(text: String) {
    Text(
        text,
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        modifier = Modifier.padding(start = 16.dp, bottom = 2.dp),
    )
}

private fun rarityLabel(rarity: Rarity): String = when (rarity) {
    Rarity.COMMON    -> "Common"
    Rarity.RARE      -> "Rare"
    Rarity.EPIC      -> "Epic"
    Rarity.LEGENDARY -> "Legendary"
    Rarity.UNIQUE    -> "Unique"
    Rarity.APEX      -> "Apex"
    Rarity.OMEGA     -> "Omega"
}

@Composable
private fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON    -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    Rarity.RARE      -> Color(0xFF4B9EFF)
    Rarity.EPIC      -> Color(0xFFAB67D8)
    Rarity.LEGENDARY -> Color(0xFFFFD700)
    Rarity.UNIQUE    -> Color(0xFF00CBA0)
    Rarity.APEX      -> Color(0xFFFF5555)
    Rarity.OMEGA     -> Color(0xFFFF8C00)
}
