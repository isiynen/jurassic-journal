package com.jurassicjournal.ui.enhancement

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.update.dinoImageModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancementEstimatorScreen(
    onBack: () -> Unit,
    viewModel: EnhancementEstimatorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState.dino != null) "${uiState.dino!!.name} — Enhancements"
                               else "Enhancement Estimator",
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
                    isApex         = uiState.isApex,
                    current        = uiState.current,
                    target         = uiState.target,
                    onIsApexChange = viewModel::setIsApex,
                    onCurrentChange = viewModel::setCurrent,
                    onTargetChange = viewModel::setTarget,
                )
            }

            item { SectionHeader("Estimated Cost (E${uiState.current} → E${uiState.target})") }
            item { CostCard(uiState.result) }
        }
    }
}

// ── Dino header ───────────────────────────────────────────────────────────────

@Composable
private fun DinoHeaderCard(dino: Dino) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    dino.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
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
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                RarityCheckbox(label = "Unique", checked = !isApex, onSelect = { onIsApexChange(false) })
                RarityCheckbox(label = "Apex",   checked = isApex,  onSelect = { onIsApexChange(true) })
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                EnhancementStepper(
                    label         = "Current",
                    value         = current,
                    min           = 0,
                    max           = 4,
                    onValueChange = onCurrentChange,
                    modifier      = Modifier.weight(1f),
                )
                Text(
                    "→",
                    style    = MaterialTheme.typography.headlineMedium,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                EnhancementStepper(
                    label         = "Target",
                    value         = target,
                    min           = current + 1,
                    max           = 5,
                    onValueChange = onTargetChange,
                    modifier      = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RarityCheckbox(label: String, checked: Boolean, onSelect: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.clickable(onClick = onSelect),
    ) {
        Checkbox(checked = checked, onCheckedChange = { onSelect() })
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EnhancementStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                text       = "E$value",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(horizontal = 8.dp),
                textAlign  = TextAlign.Center,
            )
            RepeatingButton(
                onClick  = { if (value < max) onValueChange(value + 1) },
                enabled  = value < max,
                modifier = Modifier.size(32.dp),
            ) { Text("+", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

// ── Cost card ─────────────────────────────────────────────────────────────────

@Composable
private fun CostCard(result: EnhancementCostResult) {
    Card(
        modifier  = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (result.bronze > 0) { CostRow("Bronze Catalyst", "%,d".format(result.bronze)); Divider() }
            if (result.silver > 0) { CostRow("Silver Catalyst", "%,d".format(result.silver)); Divider() }
            if (result.gold   > 0) { CostRow("Gold Catalyst",   "%,d".format(result.gold));   Divider() }
            CostRow("Coins", "%,d".format(result.coins))
            Divider()
            CostRow("DNA", "%,d".format(result.dna))
        }
    }
}

@Composable
private fun CostRow(label: String, value: String) {
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
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text       = title,
        style      = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color      = MaterialTheme.colorScheme.primary,
        modifier   = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    )
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
