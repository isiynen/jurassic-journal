package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sufficienteffort.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.sufficienteffort.jurassicjournal.data.game.entity.OmegaTrainingConfig
import com.sufficienteffort.jurassicjournal.data.model.BoostStat
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.OmegaStat
import com.sufficienteffort.jurassicjournal.data.model.ProgressionSystem
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation
import com.sufficienteffort.jurassicjournal.data.model.defaultLevel
import com.sufficienteffort.jurassicjournal.data.model.displayName
import com.sufficienteffort.jurassicjournal.data.model.label
import com.sufficienteffort.jurassicjournal.data.model.maxDna
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.data.user.entity.Team
import com.sufficienteffort.jurassicjournal.ui.components.BadgeChip
import com.sufficienteffort.jurassicjournal.ui.components.DinoImage
import com.sufficienteffort.jurassicjournal.ui.components.DnaOnHandRow
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.NumberStepper
import com.sufficienteffort.jurassicjournal.ui.components.classColor
import com.sufficienteffort.jurassicjournal.ui.components.rarityColor
import com.sufficienteffort.jurassicjournal.ui.team.DinoTeamViewModel
import com.sufficienteffort.jurassicjournal.ui.theme.SuccessGreen
import com.sufficienteffort.jurassicjournal.util.StatCalculator
import kotlin.math.roundToInt

private const val ENHANCEMENT_UNLOCK_LEVEL = 30
private const val REACTIVE_MOVE_TIER = 5

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoDetailScreen(
    onBack: () -> Unit,
    onDinoClick: (Long) -> Unit = {},
    onCalculate: (Long) -> Unit = {},
    onLevelUpCalculate: (Long, Int) -> Unit = { _, _ -> },
    onSanctuaryCalculate: (Long) -> Unit = {},
    onEnhancementEstimate: (Long, Int) -> Unit = { _, _ -> },
    showTeamSelector: Boolean = true,
    viewModel: DinoDetailViewModel = hiltViewModel(),
    teamViewModel: DinoTeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val teamState by teamViewModel.state.collectAsStateWithLifecycle()
    val pendingUncheck by viewModel.pendingEnhancementUncheck.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFullResetDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCatalogueDialog by remember { mutableStateOf(false) }
    var showSanctuaryUnsavedDialog by remember { mutableStateOf(false) }

    // Intercept hardware/gesture back when there are unsaved changes
    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showExitDialog = true
    }

    if (showFullResetDialog) {
        ConfirmDialog(
            title = "Reset to Defaults",
            text = "This will clear the level and all boosts for this dino, returning it to its base state. Are you sure?",
            confirmLabel = "Reset",
            onConfirm = { showFullResetDialog = false; viewModel.fullReset() },
            onDismiss = { showFullResetDialog = false },
        )
    }

    if (showCatalogueDialog) {
        val dinoName = uiState.detail?.dino?.name ?: "This dino"
        ConfirmDialog(
            title = "Mark as Catalogued?",
            text = "\"$dinoName\" will lose its NEW badge, drop out of the New filter, " +
                "and return to its normal place in the list. " +
                "You can't undo this for the current profile.",
            confirmLabel = "Confirm",
            onConfirm = { showCatalogueDialog = false; viewModel.clearNewStatus() },
            onDismiss = { showCatalogueDialog = false },
        )
    }

    pendingUncheck?.let { pending ->
        ConfirmDialog(
            title = "Remove Boosts?",
            text = "Disabling E${pending.tier} will remove ${pending.boostsTrimmed} boost(s) " +
                "to stay within the new limit.",
            confirmLabel = "OK",
            onConfirm = viewModel::confirmEnhancementUncheck,
            onDismiss = viewModel::cancelEnhancementUncheck,
        )
    }

    if (showExitDialog) {
        UnsavedChangesDialog(
            text = "You have unsaved changes. Would you like to save before leaving?",
            onCancel = { showExitDialog = false },
            onDiscard = { showExitDialog = false; onBack() },
            onSave = { showExitDialog = false; viewModel.save(); onBack() },
        )
    }

    if (showSanctuaryUnsavedDialog) {
        val dinoId = uiState.detail?.dino?.id
        val proceed = { if (dinoId != null) onSanctuaryCalculate(dinoId) }
        UnsavedChangesDialog(
            text = "You have unsaved changes. Would you like to save before planning sanctuary interactions?",
            onCancel = { showSanctuaryUnsavedDialog = false },
            onDiscard = { showSanctuaryUnsavedDialog = false; proceed() },
            onSave = { showSanctuaryUnsavedDialog = false; viewModel.save(); proceed() },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.saveEvents.collect {
            snackbarHostState.showSnackbar("Saved", duration = SnackbarDuration.Short)
        }
    }

    // Whether there's any customisation worth resetting (saved or unsaved)
    val defaultLevel = uiState.detail?.dino?.rarity?.defaultLevel() ?: 26
    val hasAnyCustomization = uiState.level != defaultLevel || uiState.boosts != BoostState()
        || uiState.hasUnsavedChanges
        || uiState.omegaPoints.values.any { it > 0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.dino?.name ?: "", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState.hasUnsavedChanges) showExitDialog = true else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (hasAnyCustomization) {
                        IconButton(onClick = {
                            // Unsaved edits: revert to last saved. Already saved: offer a full reset.
                            if (uiState.hasUnsavedChanges) viewModel.reset() else showFullResetDialog = true
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset")
                        }
                    }
                    if (uiState.hasUnsavedChanges) {
                        IconButton(onClick = { viewModel.save() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Loading…", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        val detail = uiState.detail ?: return@Scaffold
        val computed = uiState.computed ?: return@Scaffold
        val dino = detail.dino

        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            item {
                DinoImage(
                    imagePath = dino.imagePath,
                    contentDescription = dino.name,
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            item {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BadgeChip(label = dino.rarity.label(), color = rarityColor(dino.rarity))
                    BadgeChip(label = dino.dinoClass.label(), color = classColor(dino.dinoClass))
                    if (dino.isHybrid) BadgeChip("Hybrid", MaterialTheme.colorScheme.tertiary)
                }
            }

            if (dino.description.isNotBlank()) {
                item {
                    Text(
                        dino.description,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (uiState.isNew) {
                item {
                    ActionButton("Mark Catalogued") { showCatalogueDialog = true }
                }
            }

            if (dino.isHybrid || detail.hybridsUsing.isNotEmpty() || uiState.enhancementItems.isNotEmpty()) {
                item {
                    DnaOnHandRow(
                        value = uiState.dnaOnHand,
                        maxDna = dino.rarity.maxDna(),
                        onValueChange = viewModel::setDnaOnHand,
                    )
                }
            }

            item {
                DetailSectionHeader("Stats")
                StatsPanel(
                    uiState = uiState,
                    computed = computed,
                    onLevelChange = viewModel::setLevel,
                    onBoostChange = viewModel::setBoost,
                    onToggleEnhancement = viewModel::toggleEnhancement,
                )
                if (dino.progressionSystem == ProgressionSystem.TRAINING_POINT) {
                    Spacer(Modifier.height(8.dp))
                    OmegaTrainingCard(
                        uiState = uiState,
                        omegaConfigs = detail.omegaTrainingConfigs,
                        onOmegaPointsChange = viewModel::setOmegaPoints,
                    )
                }
            }

            if (uiState.enhancementItems.isNotEmpty()) {
                item {
                    val highestUnlocked = uiState.enhancementItems
                        .filter { it.isUnlocked }.maxOfOrNull { it.tier } ?: 0
                    ActionButton("Estimate Enhancement Costs", topPadding = 4.dp) {
                        onEnhancementEstimate(dino.id, highestUnlocked)
                    }
                }
            }

            if (detail.resistances.isNotEmpty()) {
                item {
                    DetailSectionHeader("Resistances")
                    ResistancesPanel(detail.resistances.map { it.resistType to it.percentage })
                }
            }

            if (detail.movesByTrigger.isNotEmpty()) {
                item {
                    val reactiveMoveLocked = uiState.enhancementItems.isNotEmpty() &&
                        uiState.enhancementItems.none { it.tier == REACTIVE_MOVE_TIER && it.isUnlocked }
                    DetailSectionHeader("Moves")
                    MovesPanel(detail.movesByTrigger, computed.attack, reactiveMoveLocked)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (!dino.isHybrid) {
                item {
                    ActionButton("Level-Up Costs") { onLevelUpCalculate(dino.id, uiState.level) }
                }
            }

            if (detail.ingredientTree.isNotEmpty()) {
                item {
                    DetailSectionHeader("How to Create")
                    IngredientsSection(
                        ingredientTree = detail.ingredientTree,
                        ingredientMinLevel = dino.rarity.minLevel() - 1,
                        onDinoClick = onDinoClick,
                    )
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Calculate Creation / Level-Up Costs") { onCalculate(dino.id) }
                }
            }

            if (detail.hybridsUsing.isNotEmpty()) {
                item {
                    DetailSectionHeader("Used in Hybrids")
                    HybridsUsingSection(hybrids = detail.hybridsUsing, onDinoClick = onDinoClick)
                    Spacer(Modifier.height(8.dp))
                }
            }

            detail.sanctuaryPoints?.let { sp ->
                item {
                    DetailSectionHeader("Sanctuary")
                    SanctuarySpEstimate(sp, uiState.level, uiState.boosts)
                    Spacer(Modifier.height(8.dp))
                    ActionButton("Plan Sanctuary Interactions") {
                        if (uiState.hasUnsavedChanges) showSanctuaryUnsavedDialog = true
                        else onSanctuaryCalculate(dino.id)
                    }
                }
            }

            if (showTeamSelector && teamState.availableTeams.isNotEmpty()) {
                item {
                    DetailSectionHeader("Teams")
                    TeamsCard(
                        teams = teamState.availableTeams,
                        memberTeamIds = teamState.memberTeamIds,
                        onToggle = { teamId, isMember ->
                            if (isMember) teamViewModel.removeFromTeam(teamId)
                            else teamViewModel.addToTeam(teamId)
                        },
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (detail.spawnLocations.isNotEmpty()) {
                item {
                    DetailSectionHeader("Location Found")
                    LocationFoundSection(detail.spawnLocations)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────────

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun UnsavedChangesDialog(
    text: String,
    onCancel: () -> Unit,
    onDiscard: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Unsaved Changes") },
        text = { Text(text) },
        confirmButton = {
            Row {
                TextButton(onClick = onCancel) { Text("Cancel") }
                TextButton(onClick = onDiscard) { Text("Discard") }
                TextButton(onClick = onSave) { Text("Save") }
            }
        },
        dismissButton = null,
    )
}

// ── Shared helpers ────────────────────────────────────────────────────────────

/** Full-width outlined button used for every "go to calculator" action on this screen. */
@Composable
private fun ActionButton(label: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = topPadding),
    ) {
        Text(label)
    }
    Spacer(Modifier.height(4.dp))
}

@Composable
internal fun DetailSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun CardDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun TapHint() {
    Text(
        "(Press the number to manually enter.)",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
    )
}

/** "used / max" caption that turns red at the cap. */
@Composable
private fun CapacityLabel(used: Int, max: Int) {
    Text(
        "$used / $max",
        style = MaterialTheme.typography.labelMedium,
        color = if (used >= max) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    )
}

// ── Stats Panel ───────────────────────────────────────────────────────────────

@Composable
private fun StatsPanel(
    uiState: DinoDetailUiState,
    computed: ComputedStats,
    onLevelChange: (Int) -> Unit,
    onBoostChange: (BoostStat, Int) -> Unit,
    onToggleEnhancement: (EnhancementUiItem) -> Unit,
) {
    val level = uiState.level
    val boosts = uiState.boosts
    val maxTotal = uiState.maxTotalBoosts
    val minLevel = uiState.detail?.dino?.rarity?.minLevel() ?: StatCalculator.MIN_LEVEL

    JJCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Level", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp))
                Slider(
                    value = level.toFloat(),
                    onValueChange = { onLevelChange(it.roundToInt()) },
                    valueRange = minLevel.toFloat()..StatCalculator.MAX_LEVEL.toFloat(),
                    steps = StatCalculator.MAX_LEVEL - 1 - minLevel,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "$level",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(32.dp),
                    textAlign = TextAlign.End,
                )
            }

            Spacer(Modifier.height(8.dp))
            CardDivider()
            Spacer(Modifier.height(12.dp))

            StatRow("❤ Health", computed.health.toString())
            StatRow("⚔ Damage", computed.attack.toString())
            StatRow("⚡ Speed",  computed.speed.toString())
            StatRow("🛡 Armor",  "${computed.armor.toInt()}%")
            StatRow("🎯 Crit Chance",      "${computed.critChance.toInt()}%")
            StatRow("💥 Crit Multiplier", "${computed.critMultiplier.toInt()}%")

            Spacer(Modifier.height(16.dp))
            CardDivider()
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Boosts", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                CapacityLabel(boosts.total, maxTotal)
            }
            TapHint()
            Spacer(Modifier.height(8.dp))

            BoostStat.entries.forEach { stat ->
                StepperRow(
                    label = stat.label,
                    value = boosts[stat],
                    max = boosts.maxFor(stat, maxTotal),
                    onChange = { onBoostChange(stat, it) },
                    labelWidth = 40.dp,
                ) {
                    Spacer(Modifier.weight(1f))
                    Text("/ ${boosts.maxFor(stat, maxTotal)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }

            if (uiState.enhancementItems.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                CardDivider()
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Enhancements", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    if (level < ENHANCEMENT_UNLOCK_LEVEL) {
                        Text(
                            "Available at level $ENHANCEMENT_UNLOCK_LEVEL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    uiState.enhancementItems.forEach { item ->
                        EnhancementItemBox(item, onToggleEnhancement, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancementItemBox(
    item: EnhancementUiItem,
    onToggle: (EnhancementUiItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (item.isAvailable) 1f else 0.38f
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "E${item.tier}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            textAlign = TextAlign.Center,
        )
        Checkbox(
            checked = item.isUnlocked,
            onCheckedChange = { if (item.isAvailable) onToggle(item) },
            enabled = item.isAvailable,
        )
        Text(
            item.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Omega Training Card (shown below StatsPanel for Omega dinos) ──────────────

@Composable
private fun OmegaTrainingCard(
    uiState: DinoDetailUiState,
    omegaConfigs: List<OmegaTrainingConfig>,
    onOmegaPointsChange: (String, Int) -> Unit,
) {
    val points = uiState.omegaPoints
    val totalAvail = StatCalculator.maxOmegaTrainingPoints(uiState.level)
    val totalUsed = points.values.sum()

    // Known stats in enum order first; anything unknown from a newer DB trails, keyed as-is.
    val ordered = omegaConfigs.sortedBy { OmegaStat.fromKey(it.stat)?.ordinal ?: Int.MAX_VALUE }

    JJCard {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Training Points", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                CapacityLabel(totalUsed, totalAvail)
            }
            TapHint()
            Spacer(Modifier.height(8.dp))

            ordered.forEach { cfg ->
                val stat = OmegaStat.fromKey(cfg.stat)
                val label = stat?.label ?: cfg.stat.uppercase()
                val allocated = points[cfg.stat] ?: 0
                val remaining = totalAvail - totalUsed
                val maxForStat = minOf(cfg.pointCap, allocated + remaining)
                val bonus = allocated * cfg.gainPerPoint
                val bonusLabel = if (stat?.isFlat != false) "+$bonus" else "+$bonus%"
                StepperRow(
                    label = label,
                    value = allocated,
                    max = maxForStat,
                    onChange = { onOmegaPointsChange(cfg.stat, it) },
                    labelWidth = 48.dp,
                ) {
                    Text(
                        bonusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                    )
                    Text(
                        "/ ${cfg.pointCap}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

/** Label + stepper + caller-supplied trailing content (boost cap, omega bonus). */
@Composable
private fun StepperRow(
    label: String,
    value: Int,
    max: Int,
    onChange: (Int) -> Unit,
    labelWidth: androidx.compose.ui.unit.Dp,
    trailing: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(labelWidth))
        NumberStepper(
            value = value,
            min = 0,
            max = max,
            onValueChange = onChange,
            dialogTitle = label,
            valueStyle = MaterialTheme.typography.titleSmall,
            valueModifier = Modifier.width(28.dp),
        )
        trailing()
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

// ── Resistances Panel ─────────────────────────────────────────────────────────

@Composable
private fun ResistancesPanel(resistances: List<Pair<ResistanceType, Int>>) {
    JJCard {
        Column(Modifier.padding(12.dp)) {
            resistances.forEach { (type, pct) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(type.displayName(), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$pct%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            pct >= 100 -> SuccessGreen
                            pct >= 50  -> MaterialTheme.colorScheme.primary
                            else       -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

// ── Sanctuary Points Card ─────────────────────────────────────────────────────

@Composable
private fun SanctuarySpEstimate(sp: DinoSanctuaryPoint, level: Int, boosts: BoostState) {
    val estimated = StatCalculator.calculateSp(sp.spSad, level, boosts)

    JJCard {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "LV$level (your dino)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            )
            Text(
                "$estimated SP",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

// ── Teams Card ────────────────────────────────────────────────────────────────

@Composable
private fun TeamsCard(
    teams: List<Team>,
    memberTeamIds: Set<Long>,
    onToggle: (teamId: Long, isMember: Boolean) -> Unit,
) {
    JJCard {
        Column(Modifier.padding(vertical = 4.dp)) {
            teams.forEachIndexed { idx, team ->
                val isMember = team.id in memberTeamIds
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(team.id, isMember) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(team.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    if (isMember) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                "On team",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Text(
                            "Add",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
                if (idx < teams.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

// ── Location Found Section ────────────────────────────────────────────────────

@Composable
private fun LocationFoundSection(locations: List<SpawnLocation>) {
    val rows = locations.map { it.displayName() }.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pair.forEach { label ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
