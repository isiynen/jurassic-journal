package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sufficienteffort.jurassicjournal.data.model.maxDna
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.ui.components.CoinsOnHandRow
import com.sufficienteffort.jurassicjournal.ui.components.DinoHeaderCard
import com.sufficienteffort.jurassicjournal.ui.components.DinoThumbnail
import com.sufficienteffort.jurassicjournal.ui.components.DnaOnHandRow
import com.sufficienteffort.jurassicjournal.ui.components.HintText
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.LabeledStepper
import com.sufficienteffort.jurassicjournal.ui.components.NumberInputDialog
import com.sufficienteffort.jurassicjournal.ui.components.ResultRow
import com.sufficienteffort.jurassicjournal.ui.components.SectionDivider
import com.sufficienteffort.jurassicjournal.ui.components.SectionHeader
import com.sufficienteffort.jurassicjournal.ui.theme.CoinGold
import com.sufficienteffort.jurassicjournal.ui.theme.SuccessGreen
import com.sufficienteffort.jurassicjournal.ui.theme.SuccessGreenDark
import com.sufficienteffort.jurassicjournal.util.CalcResult
import com.sufficienteffort.jurassicjournal.util.HybridCostCalculator
import com.sufficienteffort.jurassicjournal.util.IngredientCost
import com.sufficienteffort.jurassicjournal.util.IngredientInput
import com.sufficienteffort.jurassicjournal.util.StatCalculator

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
                        text = uiState.hybrid?.let { "${it.name} — Calculator" } ?: "Calculator",
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
        // Ingredients arrive depth-sorted; index is the position the ViewModel expects back.
        val (direct, sub) = uiState.ingredients.withIndex().partition { it.value.depth == 0 }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { DinoHeaderCard(dino = hybrid, onClick = { onDinoClick(hybrid.id) }) }

            item { SectionHeader("Mode") }
            item { ModeCard(isCreate = uiState.isCreate, onIsCreateChange = viewModel::setIsCreate) }

            item { SectionHeader("Level Range") }
            item {
                LevelRangeCard(
                    isCreate             = uiState.isCreate,
                    currentLevel         = uiState.currentLevel,
                    targetLevel          = uiState.targetLevel,
                    minLevel             = hybrid.rarity.minLevel(),
                    onCurrentLevelChange = viewModel::setCurrentLevel,
                    onTargetLevelChange  = viewModel::setTargetLevel,
                )
            }
            item { HintText("Tap a level number to enter directly.") }

            item { SectionHeader("Your Inventory") }
            item { HintText("Tap any value to edit. Coins are shared across all calculators.") }
            item { CoinsOnHandRow(value = uiState.coinsOnHand, onValueChange = viewModel::setCoinsOnHand) }
            item {
                DnaOnHandRow(
                    label         = "Hybrid DNA already accumulated",
                    value         = uiState.currentHybridDna,
                    maxDna        = hybrid.rarity.maxDna(),
                    onValueChange = viewModel::setCurrentHybridDna,
                )
            }
            if (direct.isNotEmpty()) {
                if (sub.isNotEmpty()) item { IngredientSubHeader("Direct Ingredients") }
                items(direct, key = { "in_${it.value.dino.id}" }) { (index, input) ->
                    IngredientDnaRow(input = input, onDnaChange = { viewModel.setIngredientDna(index, it) })
                }
                if (sub.isNotEmpty()) {
                    item { IngredientSubHeader("Sub-Ingredients") }
                    items(sub, key = { "in_${it.value.dino.id}" }) { (index, input) ->
                        IngredientDnaRow(input = input, onDnaChange = { viewModel.setIngredientDna(index, it) })
                    }
                }
            }

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
                    val (directCosts, subCosts) = result.ingredientCosts.partition { it.depth == 0 }
                    if (subCosts.isNotEmpty()) item { IngredientSubHeader("Direct Ingredients") }
                    items(directCosts) { IngredientCostCard(cost = it) }
                    if (subCosts.isNotEmpty()) {
                        item { IngredientSubHeader("Sub-Ingredients") }
                        items(subCosts) { IngredientCostCard(cost = it) }
                    }
                }
            }

            uiState.maxReachableLevel?.let { maxLevel ->
                item { SectionHeader("How Far Can You Go?") }
                item {
                    if (maxLevel < uiState.currentLevel) {
                        CannotCreateCard()
                    } else {
                        MaxReachableLevelCard(currentLevel = uiState.currentLevel, maxLevel = maxLevel)
                    }
                }
            }
        }
    }
}

// ── Mode card ─────────────────────────────────────────────────────────────────

@Composable
private fun ModeCard(isCreate: Boolean, onIsCreateChange: (Boolean) -> Unit) {
    JJCard {
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
internal fun ModeCheckbox(label: String, checked: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.clickable(onClick = onSelect),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onSelect() })
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
    JJCard {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabeledStepper(
                label         = "Current",
                value         = currentLevel,
                min           = minLevel,
                max           = StatCalculator.MAX_LEVEL - 1,
                enabled       = !isCreate,
                dialogTitle   = "Enter Current Level",
                onValueChange = onCurrentLevelChange,
                modifier      = Modifier.weight(1f),
            )
            LevelArrow()
            LabeledStepper(
                label         = "Target",
                value         = targetLevel,
                min           = if (isCreate) currentLevel else currentLevel + 1,
                max           = StatCalculator.MAX_LEVEL,
                dialogTitle   = "Enter Target Level",
                onValueChange = onTargetLevelChange,
                modifier      = Modifier.weight(1f),
            )
        }
    }
}

/** The "→" between a Current and Target stepper. */
@Composable
internal fun LevelArrow() {
    Text(
        "→",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        modifier = Modifier.padding(horizontal = 8.dp),
    )
}

// ── Ingredient rows ───────────────────────────────────────────────────────────

/** Left indent grows with ingredient depth. */
private fun Modifier.ingredientMargin(depth: Int, vertical: androidx.compose.ui.unit.Dp) =
    padding(start = (16 + depth * 16).dp, end = 16.dp, top = vertical, bottom = vertical).fillMaxWidth()

private fun displayName(dinoName: String, depth: Int, parentName: String?): String =
    if (depth > 0 && parentName != null) "$parentName → $dinoName" else dinoName

@Composable
private fun IngredientDnaRow(input: IngredientInput, onDnaChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title     = "${input.dino.name} DNA on hand",
            current   = input.dnaOnHand,
            min       = 0,
            max       = input.dino.rarity.maxDna(),
            onConfirm = { onDnaChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }

    val costPerFuse = HybridCostCalculator.fuseDnaCost(input.dino.rarity, input.parentRarity)

    JJCard(modifier = Modifier.ingredientMargin(input.depth, 4.dp), onClick = { showDialog = true }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DinoThumbnail(imagePath = input.dino.imagePath, contentDescription = input.dino.name, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName(input.dino.name, input.depth, input.parentDinoName),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
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

@Composable
private fun IngredientSubHeader(title: String) {
    Text(
        text     = title,
        style    = MaterialTheme.typography.labelMedium,
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun IngredientCostCard(cost: IngredientCost) {
    JJCard(modifier = Modifier.ingredientMargin(cost.depth, 3.dp), elevation = 1.dp, cornerRadius = 10.dp) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DinoThumbnail(imagePath = cost.dino.imagePath, contentDescription = cost.dino.name, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    displayName(cost.dino.name, cost.depth, cost.parentDinoName),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Need: %,d | Have: %,d".format(cost.totalDnaNeeded, cost.dnaOnHand),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
                if (cost.fusesNeededToProduce > 0) {
                    Text(
                        "Fuse %,d× (%,d coins)".format(cost.fusesNeededToProduce, cost.fuseCoinCost),
                        style = MaterialTheme.typography.labelSmall,
                        color = CoinGold.copy(alpha = 0.85f),
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                if (cost.dnaDeficit > 0) {
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

// ── Result cards ──────────────────────────────────────────────────────────────

@Composable
private fun ResultSummaryCard(result: CalcResult) {
    JJCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        elevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            ResultRow("Hybrid DNA still needed", "%,d DNA".format(result.hybridDnaStillNeeded))
            SectionDivider()
            ResultRow("Estimated fuses (avg ${HybridCostCalculator.DNA_PER_FUSE} DNA each)", "%,d".format(result.fusesNeeded))
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
    JJCard(
        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
        elevation = 0.dp,
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
    val atMax = maxLevel >= StatCalculator.MAX_LEVEL
    val containerColor = when {
        atMax            -> SuccessGreenDark.copy(alpha = 0.15f)
        levelsGained > 0 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else             -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
    }
    val levelColor = when {
        atMax            -> SuccessGreen
        levelsGained > 0 -> MaterialTheme.colorScheme.primary
        else             -> MaterialTheme.colorScheme.error
    }
    val subtitle = when {
        atMax            -> "You have everything needed for max level!"
        levelsGained > 0 -> "+$levelsGained level${if (levelsGained > 1) "s" else ""} with your current inventory"
        else             -> "Not enough resources to advance — add your inventory above"
    }

    JJCard(containerColor = containerColor, elevation = 0.dp) {
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
