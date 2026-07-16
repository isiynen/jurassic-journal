package com.sufficienteffort.jurassicjournal.ui.calculator

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sufficienteffort.jurassicjournal.data.update.dinoImageModel
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.sufficienteffort.jurassicjournal.ui.components.RepeatingButton
import com.sufficienteffort.jurassicjournal.ui.components.SectionHeader
import com.sufficienteffort.jurassicjournal.ui.components.HintText
import com.sufficienteffort.jurassicjournal.ui.components.ResultRow
import com.sufficienteffort.jurassicjournal.ui.components.SectionDivider
import com.sufficienteffort.jurassicjournal.ui.components.NumberInputDialog
import com.sufficienteffort.jurassicjournal.ui.components.LongInputDialog
import com.sufficienteffort.jurassicjournal.ui.components.rarityLabel
import com.sufficienteffort.jurassicjournal.ui.components.rarityColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HybridCalculatorScreen(
    onBack: () -> Unit,
    onDinoClick: (Long) -> Unit = {},
    viewModel: HybridCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.hybrid != null) "${uiState.hybrid!!.name} — Calculator"
                               else "Calculator",
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

        val hybrid = uiState.hybrid ?: return@Scaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            // ── Hybrid header ────────────────────────────────────────────────
            item {
                HybridHeaderCard(hybrid = hybrid, onClick = { onDinoClick(hybrid.id) })
            }

            // ── Mode ─────────────────────────────────────────────────────────
            item { SectionHeader("Mode") }
            item {
                ModeCard(
                    isCreate       = uiState.isCreate,
                    onIsCreateChange = viewModel::setIsCreate,
                )
            }

            // ── Level range ──────────────────────────────────────────────────
            item { SectionHeader("Level Range") }
            item {
                LevelRangeCard(
                    isCreate          = uiState.isCreate,
                    currentLevel      = uiState.currentLevel,
                    targetLevel       = uiState.targetLevel,
                    minLevel          = hybrid.rarity.minLevel(),
                    onCurrentLevelChange = viewModel::setCurrentLevel,
                    onTargetLevelChange  = viewModel::setTargetLevel,
                )
            }
            item { HintText("Tap a level number to enter directly.") }

            // ── Inventory ────────────────────────────────────────────────────
            item { SectionHeader("Your Inventory") }
            item { HintText("Tap any value to edit. Coins are shared across all calculators.") }
            item {
                CoinsOnHandRow(
                    value         = uiState.coinsOnHand,
                    onValueChange = viewModel::setCoinsOnHand,
                )
            }
            item {
                DnaProgressRow(
                    label         = "Hybrid DNA already accumulated",
                    value         = uiState.currentHybridDna,
                    onValueChange = viewModel::setCurrentHybridDna,
                )
            }
            if (uiState.ingredients.isNotEmpty()) {
                items(uiState.ingredients.size) { index ->
                    IngredientDnaRow(
                        input      = uiState.ingredients[index],
                        index      = index,
                        onDnaChange = viewModel::setIngredientDna,
                    )
                }
            }

            // ── Target cost breakdown ────────────────────────────────────────
            uiState.result?.let { result ->
                val costHeaderPrefix = if (uiState.isCreate) "Estimated Cost to Create" else "Estimated Cost to Level Up"
                item { SectionHeader("$costHeaderPrefix (Lv ${uiState.currentLevel} → ${uiState.targetLevel})") }
                item { ResultSummaryCard(result = result) }
                if (result.ingredientCosts.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ingredient Breakdown",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    items(result.ingredientCosts.size) { index ->
                        IngredientCostCard(cost = result.ingredientCosts[index])
                    }
                }
            }

            // ── Max reachable level ──────────────────────────────────────────
            uiState.maxReachableLevel?.let { maxLevel ->
                item { SectionHeader("How Far Can You Go?") }
                item {
                    if (maxLevel < uiState.currentLevel) {
                        CannotCreateCard()
                    } else {
                        MaxReachableLevelCard(
                            currentLevel = uiState.currentLevel,
                            maxLevel     = maxLevel,
                        )
                    }
                }
            }
        }
    }
}

// ── Hybrid header card ────────────────────────────────────────────────────────

@Composable
private fun HybridHeaderCard(hybrid: Dino, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dinoImageModel(LocalContext.current, hybrid.imagePath))
                    .crossfade(false)
                    .build(),
                contentDescription = hybrid.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(hybrid.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = rarityLabel(hybrid.rarity),
                    style = MaterialTheme.typography.labelSmall,
                    color = rarityColor(hybrid.rarity),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

// ── Mode card ─────────────────────────────────────────────────────────────────

@Composable
private fun ModeCard(
    isCreate: Boolean,
    onIsCreateChange: (Boolean) -> Unit,
) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ModeCheckbox(label = "Create", checked = isCreate, onSelect = { onIsCreateChange(true) })
            ModeCheckbox(label = "Level Up", checked = !isCreate, onSelect = { onIsCreateChange(false) })
        }
    }
}

@Composable
private fun ModeCheckbox(label: String, checked: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.clickable(onClick = onSelect),
    ) {
        androidx.compose.material3.Checkbox(checked = checked, onCheckedChange = { onSelect() })
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

// ── Level range card ──────────────────────────────────────────────────────────

@Composable
private fun LevelRangeCard(
    isCreate: Boolean,
    currentLevel: Int,
    targetLevel: Int,
    minLevel: Int,
    onCurrentLevelChange: (Int) -> Unit,
    onTargetLevelChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LevelStepper(
                label       = "Current",
                value       = currentLevel,
                min         = minLevel,
                max         = 34,
                enabled     = !isCreate,
                onValueChange = onCurrentLevelChange,
                modifier    = Modifier.weight(1f),
            )
            Text(
                "→",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            LevelStepper(
                label       = "Target",
                value       = targetLevel,
                min         = if (isCreate) currentLevel else currentLevel + 1,
                max         = 35,
                onValueChange = onTargetLevelChange,
                modifier    = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LevelStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NumberInputDialog(
            title     = "Enter $label Level",
            current   = value,
            min       = min,
            max       = max,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatingButton(
                onClick  = { if (value > min) onValueChange(value - 1) },
                enabled  = enabled && value > min,
                modifier = Modifier.size(32.dp),
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }
            Text(
                text     = value.toString(),
                style    = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color    = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.clickable(enabled = enabled) { showDialog = true }.padding(horizontal = 8.dp),
                textAlign = TextAlign.Center,
            )
            RepeatingButton(
                onClick  = { if (value < max) onValueChange(value + 1) },
                enabled  = enabled && value < max,
                modifier = Modifier.size(32.dp),
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

// ── Inventory rows ────────────────────────────────────────────────────────────

@Composable
private fun CoinsOnHandRow(
    value: Long,
    onValueChange: (Long) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        LongInputDialog(
            title     = "Coins on Hand",
            current   = value,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text("Coins on Hand", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "%,d coins".format(value),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = Color(0xFFFFD700),
            )
        }
    }
}

@Composable
private fun DnaProgressRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title     = label,
            current   = value,
            min       = 0,
            max       = 9_999_999,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "%,d DNA".format(value),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun IngredientDnaRow(
    input: IngredientInput,
    index: Int,
    onDnaChange: (Int, Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title     = "${input.dino.name} DNA on hand",
            current   = input.dnaOnHand,
            min       = 0,
            max       = 9_999_999,
            onConfirm = { onDnaChange(index, it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }

    val costPerFuse = HybridCalculatorViewModel.fuseCostForRarity(input.dino.rarity)

    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dinoImageModel(LocalContext.current, input.dino.imagePath))
                    .crossfade(false)
                    .build(),
                contentDescription = input.dino.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(input.dino.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "$costPerFuse DNA per fuse",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
            Text(
                "%,d DNA".format(input.dnaOnHand),
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Result cards ──────────────────────────────────────────────────────────────

@Composable
private fun ResultSummaryCard(result: CalcResult) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            ResultRow("Hybrid DNA still needed", "%,d DNA".format(result.hybridDnaStillNeeded))
            SectionDivider()
            ResultRow("Estimated fuses (avg 20 DNA each)", "%,d".format(result.fusesNeeded))
            SectionDivider()
            ResultRow("Coins needed", "%,d".format(result.coinsNeeded))
            if (result.coinDeficit > 0) {
                SectionDivider()
                ResultRow(
                    label      = "Coin deficit",
                    value      = "−%,d".format(result.coinDeficit),
                    valueColor = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CannotCreateCard() {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Cannot Create Yet",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Not enough resources to create this hybrid — add your inventory above",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun MaxReachableLevelCard(currentLevel: Int, maxLevel: Int) {
    val levelsGained = maxLevel - currentLevel
    val containerColor = when {
        maxLevel >= 35      -> Color(0xFF1B5E20).copy(alpha = 0.15f)
        levelsGained > 0    -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else                -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }
    val levelColor = when {
        maxLevel >= 35   -> Color(0xFF4CAF50)
        levelsGained > 0 -> MaterialTheme.colorScheme.primary
        else             -> MaterialTheme.colorScheme.error
    }
    val subtitle = when {
        maxLevel >= 35   -> "You have everything needed for max level!"
        levelsGained > 0 -> "+$levelsGained level${if (levelsGained > 1) "s" else ""} with your current inventory"
        else             -> "Not enough resources to advance — add your inventory above"
    }

    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Max Reachable Level",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                maxLevel.toString(),
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color      = levelColor,
            )
        }
    }
}

@Composable
private fun IngredientCostCard(cost: IngredientCost) {
    val hasDeficit = cost.dnaDeficit > 0
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 3.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(10.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dinoImageModel(LocalContext.current, cost.dino.imagePath))
                    .crossfade(false)
                    .build(),
                contentDescription = cost.dino.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cost.dino.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Need: %,d | Have: %,d".format(cost.totalDnaNeeded, cost.dnaOnHand),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                if (hasDeficit) {
                    Text(
                        "-%,d".format(cost.dnaDeficit),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        "deficit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    )
                } else {
                    Text(
                        "Ready",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
