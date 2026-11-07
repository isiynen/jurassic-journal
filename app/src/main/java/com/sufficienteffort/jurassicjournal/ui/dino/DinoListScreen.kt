package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilityListItem
import com.sufficienteffort.jurassicjournal.data.game.repository.ActionCategory
import com.sufficienteffort.jurassicjournal.data.game.repository.AbilityFilterState
import com.sufficienteffort.jurassicjournal.data.user.entity.Profile
import com.sufficienteffort.jurassicjournal.data.user.entity.Team
import com.sufficienteffort.jurassicjournal.ui.profile.ProfileBarViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoListScreen(
    onDinoClick: (Long) -> Unit,
    onAbilityClick: (slug: String, threatened: Boolean) -> Unit,
    onManageProfiles: () -> Unit,
    onManageTeams: () -> Unit,
    onTeamClick: (Long) -> Unit,
    viewModel: DinoListViewModel = hiltViewModel(),
    profileBarViewModel: ProfileBarViewModel = hiltViewModel(),
) {
    val listItems by viewModel.listItems.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val dinoCount = remember(listItems) { listItems.count { it is DinoListItem.Item } }
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val abilityFilters by viewModel.abilityFilters.collectAsStateWithLifecycle()
    val abilityItems by viewModel.abilityItems.collectAsStateWithLifecycle()
    val abilityOptions by viewModel.abilityOptions.collectAsStateWithLifecycle()
    val scopeDinoCount by viewModel.scopeDinoCount.collectAsStateWithLifecycle()
    val abilityCount = remember(abilityItems) { abilityItems.count { it is AbilityListItem.Item } }
    val abilityGridState = rememberLazyGridState()
    val showModeHint by viewModel.showModeHint.collectAsStateWithLifecycle()
    var showModeSheet by remember { mutableStateOf(false) }
    val abilityMode = mode != ListMode.DINOS
    val newCount by viewModel.newCount.collectAsStateWithLifecycle()
    val barState by profileBarViewModel.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val currentProfileId by rememberUpdatedState(barState.activeProfileId)
    var scrolledForProfileId by remember { mutableLongStateOf(-1L) }

    // Jump to the top once per profile switch, after the new profile's list has arrived.
    LaunchedEffect(listItems) {
        if (currentProfileId != scrolledForProfileId) {
            scrolledForProfileId = currentProfileId
            gridState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    ProfileDropdown(
                        activeProfileId = barState.activeProfileId,
                        profiles = barState.profiles,
                        onSelect = profileBarViewModel::setActiveProfile,
                        onManage = onManageProfiles,
                    )
                },
                title = {
                    Text(
                        "Jurassic Journal",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                actions = {
                    TeamDropdown(
                        teams = barState.teams,
                        onTeamClick = onTeamClick,
                        onManage = onManageTeams,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
    ) { innerPadding ->
        if (showModeSheet) {
            ModePickerSheet(
                current = mode,
                scopeDinoCount = scopeDinoCount,
                onSelect = { viewModel.setMode(it); showModeSheet = false },
                onDismiss = { showModeSheet = false },
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fixed: search bar + reset button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                SearchBar(
                    query = if (abilityMode) abilityFilters.query else filters.query,
                    onQueryChange = if (abilityMode) viewModel::onAbilityQueryChange else viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = if (abilityMode) "Search abilities…" else "Search dinos or moves…",
                    leadingIcon = {
                        // Icon + caret reads as a dropdown, hinting that it switches the search type.
                        Row(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.dismissModeHint()
                                    showModeSheet = true
                                }
                                .padding(start = 8.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ModeIcon(mode, tint = MaterialTheme.colorScheme.primary)
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Change search type",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    TextButton(
                        onClick = {
                            if (abilityMode) {
                                viewModel.resetAbilityFilters()
                                coroutineScope.launch { abilityGridState.scrollToItem(0) }
                            } else {
                                viewModel.resetFilters()
                                coroutineScope.launch { gridState.scrollToItem(0) }
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text("Reset")
                    }
                    Text(
                        text = if (abilityMode) {
                            if (abilityCount == 1) "1 ability" else "$abilityCount abilities"
                        } else {
                            if (dinoCount == 1) "1 dino" else "$dinoCount dinos"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }

            if (showModeHint) ModeSwitchTip(onDismiss = viewModel::dismissModeHint)

            if (abilityMode) {
                AbilityResults(
                    mode = mode,
                    items = abilityItems,
                    filters = abilityFilters,
                    options = abilityOptions,
                    dinoFilters = filters,
                    scopeDinoCount = scopeDinoCount,
                    gridState = abilityGridState,
                    viewModel = viewModel,
                    onAbilityClick = onAbilityClick,
                )
            } else {
                // Scrollable: chips + dino grid together
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(DINO_GRID_COLUMNS),
                        contentPadding = PaddingValues(start = 8.dp, end = 20.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        // Chip filter rows scroll with the list
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Column {
                                RarityFilterRow(
                                    selected = filters.rarities,
                                    onToggle = viewModel::onRarityToggle,
                                    onClear = viewModel::onRarityClear,
                                )
                                ClassFilterRow(
                                    selected = filters.dinoClasses,
                                    onToggle = viewModel::onClassToggle,
                                    onClear = viewModel::onClassClear,
                                )
                                LocationFilterRow(
                                    selected = filters.locations,
                                    onToggle = viewModel::onLocationToggle,
                                    onClear = viewModel::onLocationClear,
                                )
                                StatSortRow(
                                    selected = filters.sortMode,
                                    onSelect = viewModel::onSortMode,
                                )
                                ResistanceSortRow(
                                    selected = filters.resistanceSort,
                                    onSelect = viewModel::onResistanceSort,
                                )
                                if (newCount > 0) {
                                    NewFilterRow(
                                        newCount = newCount,
                                        selected = filters.newOnly,
                                        onToggle = { viewModel.onNewOnlyFilter(!filters.newOnly) },
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                        }

                        if (listItems.isEmpty()) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    Text(
                                        "No dinosaurs found",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    )
                                }
                            }
                        } else {
                            items(
                                items = listItems,
                                key = { item ->
                                    when (item) {
                                        is DinoListItem.Header -> "header_${item.label}"
                                        is DinoListItem.Item   -> item.result.dino.id
                                    }
                                },
                                span = { item ->
                                    if (item is DinoListItem.Header) GridItemSpan(maxLineSpan)
                                    else GridItemSpan(1)
                                },
                            ) { item ->
                                when (item) {
                                    is DinoListItem.Header -> SortGroupHeader(item.label)
                                    is DinoListItem.Item   -> DinoGridCell(
                                        dino = item.result.dino,
                                        matchedMoves = item.result.matchedMoves,
                                        isNew = item.result.isNew,
                                        onClick = { onDinoClick(item.result.dino.id) },
                                    )
                                }
                            }
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

// ── Ability results ───────────────────────────────────────────────────────────

@Composable
private fun AbilityResults(
    mode: ListMode,
    items: List<AbilityListItem>,
    filters: AbilityFilterState,
    options: AbilityOptions,
    dinoFilters: FilterState,
    scopeDinoCount: Int,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    viewModel: DinoListViewModel,
    onAbilityClick: (slug: String, threatened: Boolean) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(DINO_GRID_COLUMNS),
            contentPadding = PaddingValues(start = 8.dp, end = 20.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    if (mode == ListMode.ABILITIES_SCOPED) {
                        ScopeBanner(
                            dinoCount = scopeDinoCount,
                            summary = dinoFilters.scopeSummary(),
                            onEditFilters = { viewModel.setMode(ListMode.DINOS) },
                            onClearScope = { viewModel.setMode(ListMode.ABILITIES_ALL) },
                        )
                    }
                    TriggerPriorityRow(
                        filters.triggers, filters.priorities,
                        viewModel::onTriggerToggle, viewModel::onPriorityToggle,
                        viewModel::onTriggerPriorityClear,
                    )
                    TargetFilterRow(options.targets, filters.targets, viewModel::onTargetToggle, viewModel::onTargetClear)
                    listOf(ActionCategory.BASIC, ActionCategory.BYPASS).let { cats ->
                        ActionFilterRow(
                            cats, options.actions, filters.actions,
                            viewModel::onActionToggle, { viewModel.onActionsClear(cats) },
                        )
                    }
                    listOf(
                        ActionCategory.POSITIVE, ActionCategory.NULLIFY,
                        ActionCategory.NEGATIVE, ActionCategory.CLEANSE,
                    ).forEach { category ->
                        ActionFilterRow(
                            listOf(category), options.actions, filters.actions,
                            viewModel::onActionToggle, { viewModel.onActionsClear(listOf(category)) },
                        )
                    }
                    AbilitySortRow(filters.sortMode, viewModel::onAbilitySort)
                    Spacer(Modifier.height(4.dp))
                }
            }

            if (items.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            "No abilities found",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        )
                    }
                }
            } else {
                items(
                    items = items,
                    key = { item ->
                        when (item) {
                            is AbilityListItem.Header -> "header_${item.label}"
                            is AbilityListItem.Item -> item.entry.key
                        }
                    },
                    span = { item ->
                        if (item is AbilityListItem.Header) GridItemSpan(maxLineSpan) else GridItemSpan(1)
                    },
                ) { item ->
                    when (item) {
                        is AbilityListItem.Header -> SortGroupHeader(item.label)
                        is AbilityListItem.Item -> AbilityGridCell(
                            entry = item.entry,
                            onClick = { onAbilityClick(item.entry.slug, item.entry.threatened) },
                        )
                    }
                }
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

// ── Top-bar dropdowns ─────────────────────────────────────────────────────────

@Composable
private fun DropdownAnchor(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ProfileDropdown(
    activeProfileId: Long,
    profiles: List<Profile>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val activeName = profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Profile"
    Box {
        DropdownAnchor(activeName) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Text(profile.name, fontWeight = if (profile.id == activeProfileId) FontWeight.Bold else null)
                    },
                    onClick = { onSelect(profile.id); expanded = false },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Manage Profiles") },
                onClick = { onManage(); expanded = false },
            )
        }
    }
}

@Composable
private fun TeamDropdown(
    teams: List<Team>,
    onTeamClick: (Long) -> Unit,
    onManage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        DropdownAnchor("Teams") { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (teams.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No teams yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    onClick = {},
                    enabled = false,
                )
            } else {
                teams.forEach { team ->
                    DropdownMenuItem(
                        text = { Text(team.name) },
                        onClick = { onTeamClick(team.id); expanded = false },
                    )
                }
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Manage Teams") },
                onClick = { onManage(); expanded = false },
            )
        }
    }
}

@Composable
private fun SortGroupHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
