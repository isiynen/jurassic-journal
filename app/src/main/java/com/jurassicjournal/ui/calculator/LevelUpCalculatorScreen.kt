package com.sufficienteffort.jurassicjournal.ui.calculator

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.sufficienteffort.jurassicjournal.data.game.entity.Dino
import com.sufficienteffort.jurassicjournal.data.update.dinoImageModel
import com.sufficienteffort.jurassicjournal.ui.components.HintText
import com.sufficienteffort.jurassicjournal.ui.components.LongInputDialog
import com.sufficienteffort.jurassicjournal.ui.components.NumberInputDialog
import com.sufficienteffort.jurassicjournal.ui.components.RepeatingButton
import com.sufficienteffort.jurassicjournal.ui.components.ResultRow
import com.sufficienteffort.jurassicjournal.ui.components.SectionDivider
import com.sufficienteffort.jurassicjournal.ui.components.SectionHeader
import com.sufficienteffort.jurassicjournal.ui.components.rarityColor
import com.sufficienteffort.jurassicjournal.ui.components.rarityLabel

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
                        text = if (uiState.dino != null) "${uiState.dino!!.name} — Level-Up Costs"
                               else "Level-Up Costs",
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
            item {
                DinoHeaderCard(dino = dino)
            }

            item { SectionHeader("Level Range") }
            item {
                LevelRangeCard(
                    currentLevel         = uiState.currentLevel,
                    targetLevel          = uiState.targetLevel,
                    unlockLevel          = uiState.minLevel - 1,
                    onCurrentLevelChange = viewModel::setCurrentLevel,
                    onTargetLevelChange  = viewModel::setTargetLevel,
                )
            }
            item { HintText("Tap a level number to enter directly. \"Unlock\" includes creation DNA cost.") }

            item { SectionHeader("Your Inventory") }
            item { HintText("Tap any value to edit. Coins are shared across all calculators.") }
            item {
                CoinsOnHandRowLu(
                    value         = uiState.coinsOnHand,
                    onValueChange = viewModel::setCoinsOnHand,
                )
            }
            item {
                DnaOnHandRowLu(
                    value         = uiState.dnaOnHand,
                    onValueChange = viewModel::setDnaOnHand,
                )
            }

            uiState.result?.let { result ->
                val currentLabel = if (uiState.currentLevel == uiState.minLevel - 1) "Unlock" else "Lv ${uiState.currentLevel}"
                item { SectionHeader("Estimated Cost ($currentLabel → Lv ${uiState.targetLevel})") }
                item { LevelUpResultCard(result = result) }
            }
        }
    }
}

@Composable
private fun DinoHeaderCard(dino: Dino) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
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
                    text       = rarityLabel(dino.rarity),
                    style      = MaterialTheme.typography.labelSmall,
                    color      = rarityColor(dino.rarity),
                    fontWeight = FontWeight.SemiBold,
                )
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
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LuLevelStepper(
                label        = "Current",
                value        = currentLevel,
                min          = unlockLevel,
                max          = 34,
                unlockLevel  = unlockLevel,
                onValueChange = onCurrentLevelChange,
                modifier     = Modifier.weight(1f),
            )
            Text(
                "→",
                style    = MaterialTheme.typography.headlineMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            LuLevelStepper(
                label        = "Target",
                value        = targetLevel,
                min          = currentLevel + 1,
                max          = 35,
                unlockLevel  = null,
                onValueChange = onTargetLevelChange,
                modifier     = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LuLevelStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    unlockLevel: Int?,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnlock = unlockLevel != null && value == unlockLevel
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
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RepeatingButton(
                onClick  = { if (value > min) onValueChange(value - 1) },
                enabled  = value > min,
                modifier = Modifier.size(32.dp),
            ) { Text("−", style = MaterialTheme.typography.titleMedium) }
            Text(
                text      = if (isUnlock) "Unlock" else value.toString(),
                style     = if (isUnlock) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color     = MaterialTheme.colorScheme.onSurface,
                modifier  = Modifier
                    .clickable { showDialog = true }
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center,
            )
            RepeatingButton(
                onClick  = { if (value < max) onValueChange(value + 1) },
                enabled  = value < max,
                modifier = Modifier.size(32.dp),
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

@Composable
private fun CoinsOnHandRowLu(value: Long, onValueChange: (Long) -> Unit) {
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
private fun DnaOnHandRowLu(value: Int, onValueChange: (Int) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    if (showDialog) {
        NumberInputDialog(
            title     = "DNA on Hand",
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
            Text("DNA on Hand", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
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
private fun LevelUpResultCard(result: LevelUpResult) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
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
