package com.sufficienteffort.jurassicjournal.ui.ability

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sufficienteffort.jurassicjournal.ui.dino.DINO_GRID_COLUMNS
import com.sufficienteffort.jurassicjournal.ui.dino.DinoGridCell
import com.sufficienteffort.jurassicjournal.ui.dino.MoveCardContent
import com.sufficienteffort.jurassicjournal.ui.dino.abilityLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AbilityDetailScreen(
    onBack: () -> Unit,
    onDinoClick: (Long) -> Unit,
    viewModel: AbilityDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = state.detail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.move?.name ?: "Ability") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            detail == null -> Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                Text("Ability not found")
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(DINO_GRID_COLUMNS),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(
                            detail.move.trigger.abilityLabel(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        MoveCardContent(
                            move = detail.move,
                            secure = detail.normal,
                            threatened = detail.threatened,
                            computedAttack = 0,
                            threatenedFirst = state.threatenedFirst,
                        )
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Dinos with this ability (${detail.dinos.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
                if (detail.dinos.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "No dinos have this ability",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    items(detail.dinos, key = { it.id }) { dino ->
                        DinoGridCell(dino = dino, onClick = { onDinoClick(dino.id) })
                    }
                }
            }
        }
    }
}
