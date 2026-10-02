package com.sufficienteffort.jurassicjournal.ui.sanctuary

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sufficienteffort.jurassicjournal.data.model.BoostStat
import com.sufficienteffort.jurassicjournal.data.model.BoostState
import com.sufficienteffort.jurassicjournal.data.model.minLevel
import com.sufficienteffort.jurassicjournal.ui.components.DinoHeaderCard
import com.sufficienteffort.jurassicjournal.ui.components.HintText
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.NumberStepper
import com.sufficienteffort.jurassicjournal.ui.components.SectionHeader
import com.sufficienteffort.jurassicjournal.util.StatCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SanctuaryCalculatorScreen(
    onBack: () -> Unit,
    viewModel: SanctuaryCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.dino?.let { "${it.name} — Sanctuary" } ?: "Sanctuary Calculator",
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

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { DinoHeaderCard(dino = dino) }

            item { SectionHeader("Dino Configuration") }
            item {
                LevelAndBoostCard(
                    level         = uiState.level,
                    boosts        = uiState.boosts,
                    minLevel      = dino.rarity.minLevel(),
                    onLevelChange = viewModel::setLevel,
                    onBoostChange = viewModel::setBoost,
                )
            }
            item { HintText("Speed boosts have the greatest impact on sanctuary points.") }

            item { SectionHeader("Sanctuary Points (SP) Per Interaction") }
            item { SpContributionCard(estimatedSp = uiState.estimatedSpPerAction) }
        }
    }
}

// ── Level + boost card ────────────────────────────────────────────────────────

/** Display order on this screen: speed first because it moves SP the most. */
private val SANCTUARY_BOOST_ORDER = listOf(BoostStat.SPEED, BoostStat.ATTACK, BoostStat.HEALTH)

private fun BoostStat.sanctuaryLabel(): String = when (this) {
    BoostStat.HEALTH -> "Health boosts"
    BoostStat.ATTACK -> "Attack boosts"
    BoostStat.SPEED  -> "Speed boosts"
}

@Composable
private fun LevelAndBoostCard(
    level: Int,
    boosts: BoostState,
    minLevel: Int,
    onLevelChange: (Int) -> Unit,
    onBoostChange: (BoostStat, Int) -> Unit,
) {
    JJCard {
        Column(modifier = Modifier.padding(16.dp)) {
            ConfigRow(label = "Level") {
                CompactStepper(
                    title = "Level",
                    value = level,
                    min = minLevel,
                    max = StatCalculator.MAX_LEVEL,
                    onValueChange = onLevelChange,
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
            )
            SANCTUARY_BOOST_ORDER.forEachIndexed { i, stat ->
                if (i > 0) Spacer(Modifier.height(8.dp))
                ConfigRow(label = stat.sanctuaryLabel()) {
                    CompactStepper(
                        title = stat.sanctuaryLabel(),
                        value = boosts[stat],
                        min = 0,
                        max = StatCalculator.MAX_BOOST_TIERS_PER_STAT,
                        onValueChange = { onBoostChange(stat, it) },
                    )
                }
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
    NumberStepper(
        value = value,
        min = min,
        max = max,
        onValueChange = onValueChange,
        dialogTitle = title,
        valueModifier = Modifier.width(36.dp),
    )
}

// ── SP contribution card ──────────────────────────────────────────────────────

@Composable
private fun SpContributionCard(estimatedSp: Int?) {
    JJCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        elevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SpResultRow(label = "Your estimated SP / action", value = estimatedSp)
            Spacer(Modifier.height(8.dp))
            SpResultRow(label = "Daily SP (3 interactions)", value = estimatedSp?.let { it * 3 })
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
