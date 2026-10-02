package com.sufficienteffort.jurassicjournal.ui.enhancement

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
import com.sufficienteffort.jurassicjournal.ui.calculator.LevelArrow
import com.sufficienteffort.jurassicjournal.ui.calculator.ModeCheckbox
import com.sufficienteffort.jurassicjournal.ui.components.DinoHeaderCard
import com.sufficienteffort.jurassicjournal.ui.components.JJCard
import com.sufficienteffort.jurassicjournal.ui.components.LabeledStepper
import com.sufficienteffort.jurassicjournal.ui.components.ResultRow
import com.sufficienteffort.jurassicjournal.ui.components.SectionDivider
import com.sufficienteffort.jurassicjournal.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancementEstimatorScreen(
    onBack: () -> Unit,
    viewModel: EnhancementEstimatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.dino?.let { "${it.name} — Enhancements" } ?: "Enhancement Estimator",
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
            item { DinoHeaderCard(dino) }

            item { SectionHeader("Enhancement Range") }
            item {
                EnhancementRangeCard(
                    isApex          = uiState.isApex,
                    current         = uiState.current,
                    target          = uiState.target,
                    onIsApexChange  = viewModel::setIsApex,
                    onCurrentChange = viewModel::setCurrent,
                    onTargetChange  = viewModel::setTarget,
                )
            }

            item { SectionHeader("Estimated Cost (E${uiState.current} → E${uiState.target})") }
            item { CostCard(uiState.result) }
        }
    }
}

// ── Enhancement range card ────────────────────────────────────────────────────

@Composable
private fun EnhancementRangeCard(
    isApex: Boolean,
    current: Int,
    target: Int,
    onIsApexChange: (Boolean) -> Unit,
    onCurrentChange: (Int) -> Unit,
    onTargetChange: (Int) -> Unit,
) {
    JJCard {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ModeCheckbox(label = "Unique", checked = !isApex, onSelect = { onIsApexChange(false) })
                ModeCheckbox(label = "Apex",   checked = isApex,  onSelect = { onIsApexChange(true) })
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabeledStepper(
                    label         = "Current",
                    value         = current,
                    min           = 0,
                    max           = MAX_ENHANCEMENT_TIER - 1,
                    dialogTitle   = "Enter Current Enhancement",
                    valueText     = { "E$it" },
                    onValueChange = onCurrentChange,
                    modifier      = Modifier.weight(1f),
                )
                LevelArrow()
                LabeledStepper(
                    label         = "Target",
                    value         = target,
                    min           = current + 1,
                    max           = MAX_ENHANCEMENT_TIER,
                    dialogTitle   = "Enter Target Enhancement",
                    valueText     = { "E$it" },
                    onValueChange = onTargetChange,
                    modifier      = Modifier.weight(1f),
                )
            }
        }
    }
}

// ── Cost card ─────────────────────────────────────────────────────────────────

@Composable
private fun CostCard(result: EnhancementCostResult) {
    JJCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        elevation = 0.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            if (result.bronze > 0) { ResultRow("Bronze Catalyst", "%,d".format(result.bronze)); SectionDivider() }
            if (result.silver > 0) { ResultRow("Silver Catalyst", "%,d".format(result.silver)); SectionDivider() }
            if (result.gold   > 0) { ResultRow("Gold Catalyst",   "%,d".format(result.gold));   SectionDivider() }
            ResultRow("Coins", "%,d".format(result.coins))
            SectionDivider()
            ResultRow("DNA", "%,d".format(result.dna))
        }
    }
}
