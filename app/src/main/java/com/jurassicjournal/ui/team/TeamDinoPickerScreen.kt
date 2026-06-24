package com.jurassicjournal.ui.team

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jurassicjournal.ui.dino.ClassFilterRow
import com.jurassicjournal.ui.dino.DinoFastScrollbar
import com.jurassicjournal.ui.dino.DinoGridCell
import com.jurassicjournal.ui.dino.NewFilterRow
import com.jurassicjournal.ui.dino.RarityFilterRow
import com.jurassicjournal.ui.dino.SearchBar
import com.jurassicjournal.ui.profile.ProfileBarViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamDinoPickerScreen(
    onBack: () -> Unit,
    onDinoClick: (Long) -> Unit,
    viewModel: TeamDinoPickerViewModel = hiltViewModel(),
    profileBarViewModel: ProfileBarViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val newCount by viewModel.newCount.collectAsState()
    val stagedIds by viewModel.stagedIds.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    val teamName by viewModel.teamName.collectAsState()
    val barState by profileBarViewModel.state.collectAsState()
    val gridState = rememberLazyGridState()

    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.saveCompleted.collect { onBack() }
    }

    BackHandler(enabled = hasChanges) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have unsaved membership changes. What would you like to do?") },
            confirmButton = {
                androidx.compose.foundation.layout.Row {
                    TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
                    TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("Discard") }
                    TextButton(onClick = { showDiscardDialog = false; viewModel.save() }) { Text("Save") }
                }
            },
            dismissButton = null,
        )
    }

    val profileName = barState.profiles
        .firstOrNull { it.id == barState.activeProfileId }?.name ?: "Profile"

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { if (hasChanges) showDiscardDialog = true else onBack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "$profileName · Add/Remove Dinos · $teamName",
                            style = MaterialTheme.typography.titleSmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.save() }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (hasChanges) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SearchBar(
                query = filters.query,
                onQueryChange = viewModel::onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            RarityFilterRow(
                selected = filters.rarity,
                onSelect = viewModel::onRarityFilter,
            )
            ClassFilterRow(
                selected = filters.dinoClass,
                onSelect = viewModel::onClassFilter,
            )
            if (newCount > 0) {
                NewFilterRow(
                    newCount = newCount,
                    selected = filters.newOnly,
                    onToggle = { viewModel.onNewOnlyFilter(!filters.newOnly) },
                )
            }
            Spacer(Modifier.height(4.dp))

            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No dinosaurs found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(4),
                        contentPadding = PaddingValues(
                            start = 8.dp, end = 20.dp, top = 8.dp, bottom = 8.dp,
                        ),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.dino.id }) { result ->
                            DinoGridCell(
                                dino = result.dino,
                                matchedMoves = result.matchedMoves,
                                isNew = result.isNew,
                                isSelected = result.dino.id in stagedIds,
                                onClick = { viewModel.toggle(result.dino.id) },
                            )
                        }
                    }
                    DinoFastScrollbar(
                        gridState = gridState,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .fillMaxHeight()
                            .width(14.dp)
                            .padding(vertical = 8.dp),
                    )
                }
            }
        }
    }
}
