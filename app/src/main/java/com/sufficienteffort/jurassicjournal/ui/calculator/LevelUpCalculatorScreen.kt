package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sufficienteffort.jurassicjournal.data.model.maxDna
import com.sufficienteffort.jurassicjournal.ui.components.CoinsOnHandRow
import com.sufficienteffort.jurassicjournal.ui.components.DinoHeaderCard
import com.sufficienteffort.jurassicjournal.ui.components.DnaOnHandRow
import com.sufficienteffort.jurassicjournal.ui.components.HintText
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.LabeledStepper
import com.sufficienteffort.jurassicjournal.ui.components.ResultRow
import com.sufficienteffort.jurassicjournal.ui.components.SectionDivider
import com.sufficienteffort.jurassicjournal.ui.components.SectionHeader
import com.sufficienteffort.jurassicjournal.util.StatCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelUpCalculatorScreen(
    onBack: () -> Unit,
    viewModel: LevelUpCalculatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.dino?.let { "${it.name} — Level-Up Costs" } ?: "Level-Up Costs",
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
        val unlockLevel = uiState.minLevel - 1

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            item { DinoHeaderCard(dino = dino) }

            item { SectionHeader("Level Range") }
            item {
                LevelRangeCard(
                    currentLevel         = uiState.currentLevel,
                    targetLevel          = uiState.targetLevel,
                    unlockLevel          = unlockLevel,
                    onCurrentLevelChange = viewModel::setCurrentLevel,
                    onTargetLevelChange  = viewModel::setTargetLevel,
                )
            }
            item {
                HintText(
                    "Tap a level number to type it directly. To set Current to Unlock, type $unlockLevel — " +
                    "one below level ${uiState.minLevel}, where this rarity starts. Unlock includes the creation DNA cost."
                )
            }

            item { SectionHeader("Your Inventory") }
            item { HintText("Tap any value to edit. Coins are shared across all calculators.") }
            item { CoinsOnHandRow(value = uiState.coinsOnHand, onValueChange = viewModel::setCoinsOnHand) }
            item {
                DnaOnHandRow(
                    value         = uiState.dnaOnHand,
                    maxDna        = dino.rarity.maxDna(),
                    onValueChange = viewModel::setDnaOnHand,
                )
            }

            uiState.result?.let { result ->
                val currentLabel = if (uiState.currentLevel == unlockLevel) "Unlock" else "Lv ${uiState.currentLevel}"
                item { SectionHeader("Estimated Cost ($currentLabel → Lv ${uiState.targetLevel})") }
                item { LevelUpResultCard(result = result) }
            }
        }
    }
}

@Composable
private fun LevelRangeCard(
    currentLevel: Int,
    targetLevel: Int,
    unlockLevel: Int,
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
                min           = unlockLevel,
                max           = StatCalculator.MAX_LEVEL - 1,
                dialogTitle   = "Enter Current Level",
                valueText     = { if (it == unlockLevel) "Unlock" else it.toString() },
                valueStyle    = if (currentLevel == unlockLevel) MaterialTheme.typography.titleSmall
                                else MaterialTheme.typography.titleLarge,
                onValueChange = onCurrentLevelChange,
                modifier      = Modifier.weight(1f),
            )
            LevelArrow()
            LabeledStepper(
                label         = "Target",
                value         = targetLevel,
                min           = currentLevel + 1,
                max           = StatCalculator.MAX_LEVEL,
                dialogTitle   = "Enter Target Level",
                onValueChange = onTargetLevelChange,
                modifier      = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LevelUpResultCard(result: LevelUpResult) {
    JJCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        elevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            ResultRow("DNA still needed", "%,d DNA".format(result.dnaStillNeeded))
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
