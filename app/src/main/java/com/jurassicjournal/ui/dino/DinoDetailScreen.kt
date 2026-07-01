package com.jurassicjournal.ui.dino

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jurassicjournal.data.update.abilityIconModel
import com.jurassicjournal.data.update.dinoImageModel
import com.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.jurassicjournal.data.game.entity.OmegaTrainingConfig
import com.jurassicjournal.data.game.repository.DinoMoveDetail
import com.jurassicjournal.data.game.repository.IngredientNode
import com.jurassicjournal.data.game.repository.MoveVariant
import com.jurassicjournal.data.game.repository.ParsedTarget
import com.jurassicjournal.data.game.repository.displayName
import com.jurassicjournal.data.model.MovePriorityType
import com.jurassicjournal.data.model.MoveTriggerType
import com.jurassicjournal.data.model.MoveUnlockType
import com.jurassicjournal.data.model.ProgressionSystem
import com.jurassicjournal.data.model.ResistanceType
import com.jurassicjournal.data.model.SpawnLocation
import com.jurassicjournal.data.model.minLevel
import com.jurassicjournal.data.user.entity.Team
import com.jurassicjournal.ui.team.DinoTeamViewModel
import com.jurassicjournal.util.StatCalculator
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.json.JSONArray
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ATTACK_MULT_REGEX = Regex("""(?i)attack\s+([\d.]+)x""")

// ── Icon overlay model ────────────────────────────────────────────────────────

private enum class OverlayPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
private data class IconOverlay(val rawPath: String, val position: OverlayPosition)

private fun parseOverlays(json: String?): List<IconOverlay> {
    if (json.isNullOrEmpty()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).mapNotNull { i ->
            val obj = arr.getJSONObject(i)
            val rawPath = obj.optString("path").takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val pos = when (obj.optString("position")) {
                "top_left"     -> OverlayPosition.TOP_LEFT
                "top_right"    -> OverlayPosition.TOP_RIGHT
                "bottom_left"  -> OverlayPosition.BOTTOM_LEFT
                "bottom_right" -> OverlayPosition.BOTTOM_RIGHT
                else           -> return@mapNotNull null
            }
            IconOverlay(rawPath, pos)
        }
    } catch (_: Exception) { emptyList() }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoDetailScreen(
    onBack: () -> Unit,
    onDinoClick: (Long) -> Unit = {},
    onCalculate: (Long) -> Unit = {},
    onSanctuaryCalculate: (Long) -> Unit = {},
    onEnhancementEstimate: (Long, Int) -> Unit = { _, _ -> },
    showTeamSelector: Boolean = true,
    viewModel: DinoDetailViewModel = hiltViewModel(),
    teamViewModel: DinoTeamViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val teamState by teamViewModel.state.collectAsState()
    val pendingUncheck by viewModel.pendingEnhancementUncheck.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showFullResetDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showCatalogueDialog by remember { mutableStateOf(false) }

    // Intercept hardware/gesture back when there are unsaved changes
    BackHandler(enabled = uiState.hasUnsavedChanges) {
        showExitDialog = true
    }

    // Full reset confirmation
    if (showFullResetDialog) {
        AlertDialog(
            onDismissRequest = { showFullResetDialog = false },
            title = { Text("Reset to Defaults") },
            text = { Text("This will clear the level and all boosts for this dino, returning it to its base state. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    showFullResetDialog = false
                    viewModel.fullReset()
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showFullResetDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Mark Catalogued confirmation
    if (showCatalogueDialog) {
        val dinoName = uiState.detail?.dino?.name ?: "This dino"
        AlertDialog(
            onDismissRequest = { showCatalogueDialog = false },
            title = { Text("Mark as Catalogued?") },
            text = {
                Text(
                    "\"$dinoName\" will lose its NEW badge, drop out of the New filter, " +
                    "and return to its normal place in the list. " +
                    "You can't undo this for the current profile."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCatalogueDialog = false
                    viewModel.clearNewStatus()
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showCatalogueDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Enhancement uncheck warning
    pendingUncheck?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelEnhancementUncheck,
            title = { Text("Remove Boosts?") },
            text = {
                Text(
                    "Disabling E${pending.tier} will remove ${pending.boostsTrimmed} boost(s) " +
                    "to stay within the new limit."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmEnhancementUncheck) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelEnhancementUncheck) { Text("Cancel") }
            },
        )
    }

    // Unsaved-changes exit guard
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved changes. Would you like to save before leaving?") },
            confirmButton = {
                Row {
                    TextButton(onClick = { showExitDialog = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        showExitDialog = false
                        onBack()
                    }) { Text("Discard") }
                    TextButton(onClick = {
                        showExitDialog = false
                        viewModel.save()
                        onBack()
                    }) { Text("Save") }
                }
            },
            dismissButton = null,
        )
    }

    LaunchedEffect(Unit) {
        viewModel.saveEvents.collect {
            snackbarHostState.showSnackbar("Saved", duration = SnackbarDuration.Short)
        }
    }

    // Whether there's any customisation worth resetting (saved or unsaved)
    val hasAnyCustomization = uiState.level != 26 || uiState.boosts != BoostState()
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
                            if (uiState.hasUnsavedChanges) {
                                // Revert to last saved state
                                viewModel.reset()
                            } else {
                                // Already at saved state — offer full reset
                                showFullResetDialog = true
                            }
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

        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {

            item {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(dinoImageModel(LocalContext.current, detail.dino.imagePath))
                        .crossfade(false).build(),
                    contentDescription = detail.dino.name,
                    contentScale = ContentScale.Fit,
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
                    BadgeChip(
                        label = detail.dino.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = rarityColor(detail.dino.rarity),
                    )
                    val classLabel = detail.dino.dinoClass.name.lowercase().replace('_', ' ')
                        .split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    BadgeChip(label = classLabel, color = classColor(detail.dino.dinoClass))
                    if (detail.dino.isHybrid) BadgeChip("Hybrid", MaterialTheme.colorScheme.tertiary)
                }
            }

            if (detail.dino.description.isNotBlank()) {
                item {
                    Text(
                        detail.dino.description,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }

            if (uiState.isNew) {
                item {
                    OutlinedButton(
                        onClick = { showCatalogueDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Mark Catalogued")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (detail.dino.isHybrid || detail.hybridsUsing.isNotEmpty() || uiState.enhancementItems.isNotEmpty()) {
                item {
                    DnaOnHandCard(
                        dnaOnHand = uiState.dnaOnHand,
                        onValueChange = viewModel::setDnaOnHand,
                    )
                }
            }

            item {
                SectionHeader("Stats")
                StatsPanel(
                    uiState = uiState,
                    computed = computed,
                    onLevelChange = viewModel::setLevel,
                    onHealthBoostChange = viewModel::setHealthBoosts,
                    onAttackBoostChange = viewModel::setAttackBoosts,
                    onSpeedBoostChange = viewModel::setSpeedBoosts,
                    onToggleEnhancement = viewModel::toggleEnhancement,
                )
                if (detail.dino.progressionSystem == ProgressionSystem.TRAINING_POINT) {
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
                    OutlinedButton(
                        onClick = { onEnhancementEstimate(detail.dino.id, highestUnlocked) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp),
                    ) {
                        Text("Estimate Enhancement Costs")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (detail.resistances.isNotEmpty()) {
                item {
                    SectionHeader("Resistances")
                    ResistancesPanel(detail.resistances.map { it.resistType to it.percentage })
                }
            }

            if (detail.movesByTrigger.isNotEmpty()) {
                item {
                    val reactiveMoveLocked = uiState.enhancementItems.isNotEmpty() &&
                        uiState.enhancementItems.none { it.tier == 5 && it.isUnlocked }
                    SectionHeader("Moves")
                    MovesPanel(detail.movesByTrigger, computed.attack, reactiveMoveLocked)
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (detail.ingredientTree.isNotEmpty()) {
                item {
                    SectionHeader("How to Create")
                    IngredientsSection(
                        ingredientTree = detail.ingredientTree,
                        onDinoClick = onDinoClick,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onCalculate(detail.dino.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Calculate Creation / Level-Up Costs")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (detail.hybridsUsing.isNotEmpty()) {
                item {
                    SectionHeader("Used in Hybrids")
                    HybridsUsingSection(
                        hybrids = detail.hybridsUsing,
                        onDinoClick = onDinoClick,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            detail.sanctuaryPoints?.let { sp ->
                item {
                    SectionHeader("Sanctuary")
                    SanctuarySpEstimate(sp, uiState.level, uiState.boosts)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSanctuaryCalculate(detail.dino.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("Plan Sanctuary Interactions")
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (showTeamSelector && teamState.availableTeams.isNotEmpty()) {
                item {
                    SectionHeader("Teams")
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
                    SectionHeader("Location Found")
                    LocationFoundSection(detail.spawnLocations)
                    Spacer(Modifier.height(8.dp))
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Stats Panel ───────────────────────────────────────────────────────────────

@Composable
private fun StatsPanel(
    uiState: DinoDetailUiState,
    computed: ComputedStats,
    onLevelChange: (Int) -> Unit,
    onHealthBoostChange: (Int) -> Unit,
    onAttackBoostChange: (Int) -> Unit,
    onSpeedBoostChange: (Int) -> Unit,
    onToggleEnhancement: (EnhancementUiItem) -> Unit,
) {
    val level = uiState.level
    val boosts = uiState.boosts
    val maxTotal = uiState.maxTotalBoosts
    val minLevel = uiState.detail?.dino?.rarity?.minLevel() ?: 1

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Level", style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(52.dp))
                Slider(
                    value = level.toFloat(),
                    onValueChange = { onLevelChange(it.toInt()) },
                    valueRange = minLevel.toFloat()..35f,
                    steps = 34 - minLevel,
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
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            StatRow("❤ Health", computed.health.toString())
            StatRow("⚔ Damage", computed.attack.toString())
            StatRow("⚡ Speed",  computed.speed.toString())
            StatRow("🛡 Armor",  "${computed.armor.toInt()}%")
            StatRow("🎯 Crit",   "${computed.critChance.toInt()}%")

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Boosts", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(
                    "${boosts.total} / $maxTotal",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (boosts.total >= maxTotal) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Text(
                "(Press the number to manually enter.)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(8.dp))

            val maxH = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT, maxTotal - boosts.attack - boosts.speed)
            val maxA = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT, maxTotal - boosts.health - boosts.speed)
            val maxS = minOf(StatCalculator.MAX_BOOST_TIERS_PER_STAT, maxTotal - boosts.health - boosts.attack)

            BoostRow("HP",  boosts.health, maxH, onHealthBoostChange)
            BoostRow("ATK", boosts.attack, maxA, onAttackBoostChange)
            BoostRow("SPD", boosts.speed,  maxS, onSpeedBoostChange)

            if (uiState.enhancementItems.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Enhancements", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    if (level < 30) {
                        Text(
                            "Available at level 30",
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

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Training Points", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(
                    "$totalUsed / $totalAvail",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (totalUsed >= totalAvail) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                )
            }
            Text(
                "(Press the number to manually enter.)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            )
            Spacer(Modifier.height(8.dp))

            val statLabels = mapOf(
                "health"          to "HP",
                "attack"          to "ATK",
                "speed"           to "SPD",
                "armor"           to "ARM",
                "crit_chance"     to "CRIT %",
                "crit_multiplier" to "CRIT DMG",
            )

            omegaConfigs.forEach { cfg ->
                val label = statLabels[cfg.stat] ?: cfg.stat.uppercase()
                val allocated = points[cfg.stat] ?: 0
                val remaining = totalAvail - totalUsed
                val maxForStat = minOf(cfg.pointCap, allocated + remaining)
                val bonusLabel = when (cfg.stat) {
                    "health", "attack", "speed" -> "+${allocated * cfg.gainPerPoint}"
                    else -> "+${allocated * cfg.gainPerPoint}%"
                }
                OmegaPointRow(
                    label    = label,
                    value    = allocated,
                    max      = maxForStat,
                    cap      = cfg.pointCap,
                    bonus    = bonusLabel,
                    onChange = { onOmegaPointsChange(cfg.stat, it) },
                )
            }
        }
    }
}

@Composable
private fun OmegaPointRow(
    label: String,
    value: Int,
    max: Int,
    cap: Int,
    bonus: String,
    onChange: (Int) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NumberInputDialog(
            title = label,
            current = value,
            min = 0,
            max = max,
            onConfirm = { onChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(48.dp))
        RepeatingButton(onClick = { if (value > 0) onChange(value - 1) }, enabled = value > 0, modifier = Modifier.size(32.dp)) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(28.dp)
                .clickable { showDialog = true },
            textAlign = TextAlign.Center,
        )
        RepeatingButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max, modifier = Modifier.size(32.dp)) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            bonus,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        Text(
            "/ $cap",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
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

@Composable
private fun BoostRow(label: String, value: Int, max: Int, onChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        NumberInputDialog(
            title = label,
            current = value,
            min = 0,
            max = max,
            onConfirm = { onChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(40.dp))
        RepeatingButton(onClick = { if (value > 0) onChange(value - 1) }, enabled = value > 0, modifier = Modifier.size(32.dp)) {
            Text("−", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .width(28.dp)
                .clickable { showDialog = true },
            textAlign = TextAlign.Center,
        )
        RepeatingButton(onClick = { if (value < max) onChange(value + 1) }, enabled = value < max, modifier = Modifier.size(32.dp)) {
            Text("+", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.weight(1f))
        Text("/ $max", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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

// ── Resistances Panel ─────────────────────────────────────────────────────────

@Composable
private fun ResistancesPanel(resistances: List<Pair<ResistanceType, Int>>) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            resistances.forEach { (type, pct) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(resistanceLabel(type), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "$pct%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            pct >= 100 -> Color(0xFF4CAF50)
                            pct >= 50  -> MaterialTheme.colorScheme.primary
                            else       -> MaterialTheme.colorScheme.onSurface
                        },
                    )
                }
            }
        }
    }
}

private fun resistanceLabel(type: ResistanceType): String = when (type) {
    ResistanceType.CRIT_REDUCTION      -> "Crit Reduction"
    ResistanceType.DOT                 -> "DoT"
    ResistanceType.DAMAGE_DECREASE     -> "Damage Decrease"
    ResistanceType.REND                -> "Rend"
    ResistanceType.REDUCED_ARMOR       -> "Armor Decrease"
    ResistanceType.SPEED_DECREASE      -> "Speed Decrease"
    ResistanceType.STUN                -> "Stun"
    ResistanceType.SWAP_PREVENTION     -> "Swap Prevention"
    ResistanceType.TAUNT               -> "Taunt"
    ResistanceType.VULNERABLE          -> "Vulnerability"
    ResistanceType.RESISTANCE_DECREASE -> "Resistance Decrease"
    ResistanceType.HEAL_DECREASE       -> "Heal Decrease"
    ResistanceType.DAZE                -> "Daze"
}

// ── Moves Panel ───────────────────────────────────────────────────────────────

@Composable
private fun MovesPanel(
    movesByTrigger: Map<MoveTriggerType, List<DinoMoveDetail>>,
    computedAttack: Int,
    reactiveMoveLocked: Boolean = false,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        movesByTrigger.forEach { (trigger, moves) ->
            if (moves.isEmpty()) return@forEach
            if (trigger != MoveTriggerType.SELECTABLE) {
                Text(
                    triggerLabel(trigger),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                if (trigger == MoveTriggerType.REACTIVE && reactiveMoveLocked) {
                    Text(
                        "Enable E5 to unlock",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            val isDimmed = trigger == MoveTriggerType.REACTIVE && reactiveMoveLocked
            moves.forEach { detail ->
                MoveCard(detail, computedAttack, isDimmed)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MoveCard(detail: DinoMoveDetail, computedAttack: Int, isDimmed: Boolean = false) {
    val overlays = remember(detail.move.overlayIconsJson) {
        parseOverlays(detail.move.overlayIconsJson)
    }
    val hasThreatenedVariant = detail.threatened != null

    Card(
        modifier = Modifier.fillMaxWidth().alpha(if (isDimmed) 0.5f else 1f),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(1.dp),
    ) {
        Column(Modifier.padding(12.dp)) {

            // Icon (with overlays) + name + priority badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                AbilityIcon(
                    mainPath = detail.move.mainIconPath,
                    overlays = overlays,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            detail.move.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (detail.dinoMove.unlockType == MoveUnlockType.LEVEL) {
                            val lv = detail.dinoMove.unlockValue ?: "?"
                            MoveBadge("LV$lv", BadgeStyle.UNLOCK)
                        }
                    }
                    val secPriority = detail.secure.priority
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        if (secPriority == MovePriorityType.PRIORITY) MoveBadge("Priority", BadgeStyle.PRIORITY)
                        if (secPriority == MovePriorityType.LAST)     MoveBadge("Act Last", BadgeStyle.LAST)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (hasThreatenedVariant) {
                VariantBlock("Secure",     detail.secure,     Color(0xFF2E7D32), computedAttack)
                Spacer(Modifier.height(8.dp))
                VariantBlock("Threatened", detail.threatened!!, Color(0xFFC62828), computedAttack)
            } else {
                TargetsList(detail.secure.targets, computedAttack)
                CooldownDelayRow(detail.secure.cooldown, detail.secure.delay)
            }
        }
    }
}

// ── Ability icon with corner overlays ────────────────────────────────────────

@Composable
private fun AbilityIcon(mainPath: String?, overlays: List<IconOverlay>) {
    Box(modifier = Modifier.size(48.dp)) {
        if (mainPath != null) {
            val ctx = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(abilityIconModel(ctx, mainPath))
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
            )
        }
        val ctx = LocalContext.current
        overlays.forEach { overlay ->
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(abilityIconModel(ctx, overlay.rawPath))
                    .crossfade(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .align(
                        when (overlay.position) {
                            OverlayPosition.TOP_LEFT     -> Alignment.TopStart
                            OverlayPosition.TOP_RIGHT    -> Alignment.TopEnd
                            OverlayPosition.BOTTOM_LEFT  -> Alignment.BottomStart
                            OverlayPosition.BOTTOM_RIGHT -> Alignment.BottomEnd
                        }
                    ),
            )
        }
    }
}

// ── Variant block (Secure / Threatened) ───────────────────────────────────────

@Composable
private fun VariantBlock(
    label: String,
    variant: MoveVariant,
    labelColor: Color,
    computedAttack: Int,
) {
    Column {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = labelColor,
            )
            if (variant.priority == MovePriorityType.PRIORITY) MoveBadge("Priority", BadgeStyle.PRIORITY)
            if (variant.priority == MovePriorityType.LAST)     MoveBadge("Act Last", BadgeStyle.LAST)
        }
        Spacer(Modifier.height(4.dp))
        TargetsList(variant.targets, computedAttack)
        CooldownDelayRow(variant.cooldown, variant.delay)
    }
}

// ── Effects list ─────────────────────────────────────────────────────────────

@Composable
private fun TargetsList(targets: List<ParsedTarget>, computedAttack: Int) {
    targets.forEach { target ->
        Text(
            "Target: ${target.target}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp),
        )
        target.effects.forEach { effect ->
            Text(
                "  • ${enrichEffect(effect, computedAttack)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

/** Appends "(XXXX dmg)" when the effect contains an attack multiplier like "Attack 1.5X". */
private fun enrichEffect(effect: String, computedAttack: Int): String {
    val match = ATTACK_MULT_REGEX.find(effect) ?: return effect
    val multiplier = match.groupValues[1].toFloatOrNull() ?: return effect
    val damage = (multiplier * computedAttack).roundToInt()
    return "${effect.trimEnd()} ($damage dmg)"
}

// ── Cooldown / delay footer ───────────────────────────────────────────────────

@Composable
private fun CooldownDelayRow(cooldown: Int, delay: Int) {
    if (cooldown <= 0 && delay <= 0) return
    Row(
        modifier = Modifier.padding(top = 6.dp, start = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (cooldown > 0) {
            Text(
                "Cooldown: $cooldown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
        if (delay > 0) {
            Text(
                "Delay: $delay",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )
        }
    }
}

// ── Numeric input dialog ──────────────────────────────────────────────────────

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

// ── Badge ─────────────────────────────────────────────────────────────────────

private enum class BadgeStyle { PRIORITY, LAST, COOLDOWN, DELAY, UNLOCK }

@Composable
private fun MoveBadge(label: String, style: BadgeStyle) {
    val (bg, fg) = when (style) {
        BadgeStyle.PRIORITY -> Color(0xFF1565C0) to Color.White
        BadgeStyle.LAST     -> Color(0xFF616161) to Color.White
        BadgeStyle.COOLDOWN -> Color(0xFF4E342E) to Color(0xFFFFCCBC)
        BadgeStyle.DELAY    -> Color(0xFF311B92) to Color(0xFFE8EAF6)
        BadgeStyle.UNLOCK   -> Color(0xFF1B5E20) to Color(0xFFC8E6C9)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun BadgeChip(label: String, color: Color) {
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private fun triggerLabel(trigger: MoveTriggerType): String = when (trigger) {
    MoveTriggerType.SELECTABLE -> "Moves"
    MoveTriggerType.ON_SWAP_IN -> "Swap-In"
    MoveTriggerType.ON_COUNTER -> "Counter"
    MoveTriggerType.ON_ESCAPE  -> "On Escape"
    MoveTriggerType.REACTIVE   -> "Reactive"
}

// ── Hybrid ingredient tree ────────────────────────────────────────────────────

@Composable
private fun IngredientsSection(
    ingredientTree: List<IngredientNode>,
    onDinoClick: (Long) -> Unit,
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            ingredientTree.forEachIndexed { idx, node ->
                if (idx > 0) HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                IngredientNodeRow(node = node, depth = 0, onDinoClick = onDinoClick)
            }
        }
    }
}

@Composable
private fun IngredientNodeRow(
    node: IngredientNode,
    depth: Int,
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
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(dinoImageModel(LocalContext.current, node.dino.imagePath))
                    .crossfade(false)
                    .build(),
                contentDescription = node.dino.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    node.dino.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                BadgeChip(
                    label = node.dino.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                    color = rarityColor(node.dino.rarity),
                )
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
                IngredientNodeRow(node = child, depth = depth + 1, onDinoClick = onDinoClick)
            }
        }
    }
}

// ── Sanctuary Points Card ─────────────────────────────────────────────────────

@Composable
private fun SanctuaryCard(sp: DinoSanctuaryPoint) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            SanctuaryPointRow("LV30 Boost:Max",  sp.spMaxBoost)
            Spacer(Modifier.height(4.dp))
            SanctuaryPointRow("LV15 Boost:None", sp.spBaseline)
        }
    }
}

@Composable
private fun SanctuaryPointRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
        Text(
            "$value SP",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun SanctuarySpEstimate(sp: DinoSanctuaryPoint, level: Int, boosts: BoostState) {
    val estimated = StatCalculator.calculateSp(sp.spSad, level, boosts.health, boosts.attack, boosts.speed)

    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
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

// ── DNA on Hand Card ──────────────────────────────────────────────────────────

@Composable
private fun DnaOnHandCard(dnaOnHand: Int, onValueChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title = "DNA on Hand",
            current = dnaOnHand,
            min = 0,
            max = 9_999_999,
            onConfirm = { onValueChange(it); showDialog = false },
            onDismiss = { showDialog = false },
        )
    }
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clickable { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DNA on Hand", style = MaterialTheme.typography.labelLarge)
            Text(
                "%,d DNA".format(dnaOnHand),
                style = MaterialTheme.typography.titleSmall,
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
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
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

// ── Used in Hybrids Section ───────────────────────────────────────────────────

@Composable
private fun HybridsUsingSection(
    hybrids: List<com.jurassicjournal.data.game.entity.Dino>,
    onDinoClick: (Long) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        hybrids.forEach { hybrid ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
                    .clickable { onDinoClick(hybrid.id) },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(1.dp),
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current)
                            .data(dinoImageModel(LocalContext.current, hybrid.imagePath))
                            .crossfade(false)
                            .build(),
                        contentDescription = hybrid.name,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    )
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

// ── Location Found Section ────────────────────────────────────────────────────

@Composable
private fun LocationFoundSection(locations: List<SpawnLocation>) {
    val labels = locations.map { it.displayName() }
    val rows = labels.chunked(2)
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        rows.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pair.forEachIndexed { idx, label ->
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
                if (pair.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
