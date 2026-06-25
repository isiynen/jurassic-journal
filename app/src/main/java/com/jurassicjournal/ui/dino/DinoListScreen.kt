package com.jurassicjournal.ui.dino

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jurassicjournal.R
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.repository.DinoSearchResult
import com.jurassicjournal.data.game.repository.displayName
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.Rarity
import com.jurassicjournal.data.model.ResistanceType
import com.jurassicjournal.data.model.SpawnLocation
import com.jurassicjournal.data.model.displayName
import com.jurassicjournal.data.update.dinoImageModel
import com.jurassicjournal.ui.profile.ProfileBarViewModel
import com.jurassicjournal.ui.theme.ClassCunning
import com.jurassicjournal.ui.theme.ClassFierce
import com.jurassicjournal.ui.theme.ClassResilient
import com.jurassicjournal.ui.theme.ClassWildCard
import com.jurassicjournal.ui.theme.RarityApex
import com.jurassicjournal.ui.theme.RarityCommon
import com.jurassicjournal.ui.theme.RarityEpic
import com.jurassicjournal.ui.theme.RarityLegendary
import com.jurassicjournal.ui.theme.RarityOmega
import com.jurassicjournal.ui.theme.RarityRare
import com.jurassicjournal.ui.theme.RarityUnique
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DinoListScreen(
    onDinoClick: (Long) -> Unit,
    onManageProfiles: () -> Unit,
    onManageTeams: () -> Unit,
    onTeamClick: (Long) -> Unit,
    viewModel: DinoListViewModel = hiltViewModel(),
    profileBarViewModel: ProfileBarViewModel = hiltViewModel(),
) {
    val listItems by viewModel.listItems.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val newCount by viewModel.newCount.collectAsState()
    val barState by profileBarViewModel.state.collectAsState()
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    val currentProfileId by rememberUpdatedState(barState.activeProfileId)
    var scrolledForProfileId by remember { mutableLongStateOf(-1L) }
    var showSupportDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showSupportDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_coffee),
                            contentDescription = "Support the developer",
                        )
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Fixed: search bar + reset button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchBar(
                    query = filters.query,
                    onQueryChange = viewModel::onQueryChange,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.resetFilters()
                        coroutineScope.launch { gridState.scrollToItem(0) }
                    },
                ) {
                    Text("Reset")
                }
            }

            // Scrollable: chips + dino grid together
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(4),
                    contentPadding = PaddingValues(start = 8.dp, end = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Chip filter rows scroll with the list
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            RarityFilterRow(
                                selected = filters.rarity,
                                onSelect = viewModel::onRarityFilter,
                            )
                            ClassFilterRow(
                                selected = filters.dinoClass,
                                onSelect = viewModel::onClassFilter,
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
                    columns = 4,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(14.dp)
                        .padding(vertical = 8.dp),
                )
            }
        }
    }

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }
}

// ── Grid name formatter (display only — does not touch DB values) ─────────────

private fun formatGridName(name: String): String = name
    .replace(" Gen 2", " Gen 2")
    .replace(" Gen 3", " Gen 3")
    .replace("Gigantspinosaurus", "Gigantspino­saurus")
    .replace("Monolophosaurus", "Monolopho­saurus")

// ── Grid cell ─────────────────────────────────────────────────────────────────

@Composable
fun DinoGridCell(
    dino: Dino,
    matchedMoves: List<String> = emptyList(),
    isNew: Boolean = false,
    isSelected: Boolean? = null,
    showDeleteButton: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(2.dp),
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(dinoImageModel(context, dino.imagePath))
                    .crossfade(false)
                    .build(),
                contentDescription = dino.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )

            // NEW badge — top-left
            if (isNew) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(2.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = MaterialTheme.colorScheme.tertiary,
                ) {
                    Text(
                        "NEW",
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 7.sp,
                    )
                }
            }

            // Selection indicator — bottom-right
            if (isSelected != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                        .size(18.dp)
                        .background(
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            // Delete button — top-right (team detail)
            if (showDeleteButton && onDelete != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(20.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDelete,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove from team",
                        modifier = Modifier.size(13.dp),
                        tint = Color.White,
                    )
                }
            }
        }

        Text(
            text = formatGridName(dino.name),
            style = MaterialTheme.typography.labelSmall.copy(hyphens = Hyphens.Auto),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, start = 1.dp, end = 1.dp),
        )

        if (matchedMoves.isNotEmpty()) {
            Text(
                text = matchedMoves.joinToString(" · "),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 9.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 1.dp, start = 1.dp, end = 1.dp),
            )
        }
    }
}

// ── Fast scrollbar ────────────────────────────────────────────────────────────

@Composable
fun DinoFastScrollbar(
    gridState: LazyGridState,
    columns: Int = 4,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    val thumbFraction by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems == 0) return@derivedStateOf 1f
            val totalRows = ceil(totalItems / columns.toFloat()).toInt()
            val visibleRows = info.visibleItemsInfo.map { it.row }.distinct().size
            (visibleRows.toFloat() / totalRows.coerceAtLeast(1)).coerceIn(0.08f, 1f)
        }
    }

    val scrollFraction by remember(gridState) {
        derivedStateOf {
            val info = gridState.layoutInfo
            val totalItems = info.totalItemsCount
            if (totalItems == 0) return@derivedStateOf 0f
            val totalRows = ceil(totalItems / columns.toFloat()).toInt()
            val visibleRows = info.visibleItemsInfo.map { it.row }.distinct().size
            val maxScrollRows = (totalRows - visibleRows).coerceAtLeast(1)
            val firstRow = gridState.firstVisibleItemIndex / columns
            (firstRow.toFloat() / maxScrollRows).coerceIn(0f, 1f)
        }
    }

    var showScrollbar by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(gridState.isScrollInProgress, isDragging) {
        if (gridState.isScrollInProgress || isDragging) {
            showScrollbar = true
        } else {
            delay(3000L)
            showScrollbar = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (showScrollbar && thumbFraction < 1f) 1f else 0f,
        animationSpec = tween(300),
        label = "scrollbar_alpha",
    )

    // rememberUpdatedState keeps gesture handler current without restarting pointerInput
    val thumbFractionRef = rememberUpdatedState(thumbFraction)
    val scrollFractionRef = rememberUpdatedState(scrollFraction)

    var dragStartY by remember { mutableFloatStateOf(0f) }
    var dragStartFraction by remember { mutableFloatStateOf(0f) }

    // Read in composition scope so Canvas lambda captures fresh values on recomposition
    val tf = thumbFraction
    val sf = scrollFraction
    val dragging = isDragging
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    Box(modifier = modifier.alpha(alpha)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val curTf = thumbFractionRef.value
                            val curSf = scrollFractionRef.value
                            val trackH = size.height.toFloat()
                            val thumbH = trackH * curTf
                            val thumbTop = (trackH - thumbH) * curSf
                            isDragging = offset.y >= (thumbTop - 24f) &&
                                         offset.y <= (thumbTop + thumbH + 24f)
                            if (isDragging) {
                                dragStartY = offset.y
                                dragStartFraction = curSf
                            }
                        },
                        onDrag = { change, _ ->
                            if (isDragging) {
                                change.consume()
                                val curTf = thumbFractionRef.value
                                val trackH = size.height.toFloat()
                                val thumbH = trackH * curTf
                                val available = (trackH - thumbH).coerceAtLeast(1f)
                                val delta = change.position.y - dragStartY
                                val newFraction = (dragStartFraction + delta / available).coerceIn(0f, 1f)
                                val totalItems = gridState.layoutInfo.totalItemsCount
                                if (totalItems == 0) return@detectDragGestures
                                val totalRows = ceil(totalItems / columns.toFloat()).toInt()
                                val visibleRowsApprox = (curTf * totalRows).roundToInt().coerceAtLeast(1)
                                val maxScrollRows = (totalRows - visibleRowsApprox).coerceAtLeast(1)
                                val targetRow = (newFraction * maxScrollRows).roundToInt()
                                val targetItem = (targetRow * columns).coerceIn(0, totalItems - 1)
                                coroutineScope.launch { gridState.scrollToItem(targetItem) }
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    )
                },
        ) {
            val trackW = 6.dp.toPx()
            val x = (size.width - trackW) / 2f

            drawRoundRect(
                color = onSurfaceColor.copy(alpha = 0.15f),
                topLeft = Offset(x, 0f),
                size = Size(trackW, size.height),
                cornerRadius = CornerRadius(trackW / 2f),
            )

            val thumbH = (size.height * tf).coerceAtLeast(trackW * 2f)
            val thumbTop = (size.height - thumbH) * sf
            drawRoundRect(
                color = primaryColor.copy(alpha = if (dragging) 0.90f else 0.55f),
                topLeft = Offset(x, thumbTop),
                size = Size(trackW, thumbH),
                cornerRadius = CornerRadius(trackW / 2f),
            )
        }
    }
}

// ── Dialogs / dropdowns (unchanged) ──────────────────────────────────────────

@Composable
private fun SupportDialog(onDismiss: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_coffee),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        title = { Text("Support Jurassic Journal") },
        text = {
            Text(
                "Jurassic Journal is a solo project built and maintained alongside real life. " +
                "JWA ships new dinos, moves, and balance changes constantly — every update means " +
                "re-scraping data, running the pipeline, fixing edge cases, and keeping the UI in sync.\n\n" +
                "If this app saves you time or makes your JWA experience better, a small donation " +
                "goes a long way toward justifying those hours."
            )
        },
        confirmButton = {
            TextButton(onClick = {
                uriHandler.openUri("https://ko-fi.com/jurassicjournal")
                onDismiss()
            }) {
                Text("Buy me a coffee")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe later")
            }
        },
    )
}

@Composable
private fun ProfileDropdown(
    activeProfileId: Long,
    profiles: List<com.jurassicjournal.data.user.entity.Profile>,
    onSelect: (Long) -> Unit,
    onManage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val activeName = profiles.firstOrNull { it.id == activeProfileId }?.name ?: "Profile"
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(activeName, style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            profiles.forEach { profile ->
                DropdownMenuItem(
                    text = {
                        Text(profile.name,
                            fontWeight = if (profile.id == activeProfileId)
                                FontWeight.Bold else null)
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
    teams: List<com.jurassicjournal.data.user.entity.Team>,
    onTeamClick: (Long) -> Unit,
    onManage: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Teams", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary)
            Icon(Icons.Default.ArrowDropDown, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
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

// ── Filter rows (unchanged) ───────────────────────────────────────────────────

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search dinos or moves…") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
fun RarityFilterRow(selected: Rarity?, onSelect: (Rarity?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        }
        items(Rarity.entries) { rarity ->
            FilterChip(
                selected = selected == rarity,
                onClick = { onSelect(if (selected == rarity) null else rarity) },
                label = { Text(rarity.name.lowercase().replaceFirstChar { it.uppercase() }) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = rarityColor(rarity).copy(alpha = 0.25f),
                    selectedLabelColor = rarityColor(rarity),
                ),
            )
        }
    }
}

@Composable
fun ClassFilterRow(selected: DinoClass?, onSelect: (DinoClass?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All") })
        }
        items(DinoClass.entries) { cls ->
            FilterChip(
                selected = selected == cls,
                onClick = { onSelect(if (selected == cls) null else cls) },
                label = { Text(cls.name.lowercase().replace('_', ' ').split(" ")
                    .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = classColor(cls).copy(alpha = 0.25f),
                    selectedLabelColor = classColor(cls),
                ),
            )
        }
    }
}

@Composable
fun NewFilterRow(newCount: Int, selected: Boolean, onToggle: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected,
                onClick = onToggle,
                label = { Text("New ($newCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
                    selectedLabelColor = MaterialTheme.colorScheme.tertiary,
                ),
            )
        }
    }
}

@Composable
fun LocationFilterRow(
    selected: Set<SpawnLocation>,
    onToggle: (SpawnLocation) -> Unit,
    onClear: () -> Unit,
) {
    val locations = remember {
        SpawnLocation.entries
            .filter { it != SpawnLocation.NONE && !it.name.startsWith("EVERYWHERE_") }
            .sortedBy { it.displayName() }
    }
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = onClear,
                label = { Text("All") },
            )
        }
        items(locations) { loc ->
            FilterChip(
                selected = loc in selected,
                onClick = { onToggle(loc) },
                label = { Text(loc.displayName()) },
            )
        }
    }
}

// ── Legacy DinoCard (kept for any external callers) ───────────────────────────

@Composable
fun DinoCard(
    dino: Dino,
    matchedMoves: List<String> = emptyList(),
    isNew: Boolean = false,
    isSelected: Boolean? = null,
    onSelectedChange: ((Boolean) -> Unit)? = null,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isSelected == null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = if (isSelected != null)
                    Modifier.weight(1f).clickable(onClick = onClick)
                else
                    Modifier.weight(1f),
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
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(dino.name, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false))
                        if (isNew) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                            ) {
                                Text(
                                    "NEW",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RarityChip(dino.rarity)
                        ClassChip(dino.dinoClass)
                    }
                    if (matchedMoves.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = matchedMoves.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
            if (isSelected != null) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

// ── Chips and color helpers (unchanged) ───────────────────────────────────────

@Composable
fun RarityChip(rarity: Rarity) {
    val color = rarityColor(rarity)
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Text(
            text = rarity.name.lowercase().replaceFirstChar { it.uppercase() },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Composable
fun ClassChip(dinoClass: DinoClass) {
    val color = classColor(dinoClass)
    val label = dinoClass.name.lowercase().replace('_', ' ').split(" ")
        .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    Surface(shape = CircleShape, color = color.copy(alpha = 0.15f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

fun rarityColor(rarity: Rarity): Color = when (rarity) {
    Rarity.COMMON -> RarityCommon
    Rarity.RARE -> RarityRare
    Rarity.EPIC -> RarityEpic
    Rarity.LEGENDARY -> RarityLegendary
    Rarity.UNIQUE -> RarityUnique
    Rarity.OMEGA -> RarityOmega
    Rarity.APEX -> RarityApex
}

fun classColor(dinoClass: DinoClass): Color = when (dinoClass) {
    DinoClass.CUNNING, DinoClass.CUNNING_FIERCE, DinoClass.CUNNING_RESILIENT -> ClassCunning
    DinoClass.FIERCE, DinoClass.FIERCE_RESILIENT -> ClassFierce
    DinoClass.RESILIENT -> ClassResilient
    DinoClass.WILD_CARD -> ClassWildCard
}

@Composable
fun StatSortRow(selected: StatSortMode?, onSelect: (StatSortMode?) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Reset") },
            )
        }
        item {
            FilterChip(
                selected = selected == StatSortMode.DAMAGE,
                onClick = { onSelect(if (selected == StatSortMode.DAMAGE) null else StatSortMode.DAMAGE) },
                label = { Text("Damage") },
            )
        }
        item {
            FilterChip(
                selected = selected == StatSortMode.HEALTH,
                onClick = { onSelect(if (selected == StatSortMode.HEALTH) null else StatSortMode.HEALTH) },
                label = { Text("Health") },
            )
        }
        item {
            FilterChip(
                selected = selected == StatSortMode.SPEED,
                onClick = { onSelect(if (selected == StatSortMode.SPEED) null else StatSortMode.SPEED) },
                label = { Text("Speed") },
            )
        }
        item {
            FilterChip(
                selected = selected == StatSortMode.ARMOR,
                onClick = { onSelect(if (selected == StatSortMode.ARMOR) null else StatSortMode.ARMOR) },
                label = { Text("Armor") },
            )
        }
        item {
            FilterChip(
                selected = selected == StatSortMode.CRIT,
                onClick = { onSelect(if (selected == StatSortMode.CRIT) null else StatSortMode.CRIT) },
                label = { Text("Crit") },
            )
        }
    }
}

@Composable
fun ResistanceSortRow(selected: ResistanceType?, onSelect: (ResistanceType?) -> Unit) {
    val resistances = listOf(
        ResistanceType.REDUCED_ARMOR       to "Armor Decrease",
        ResistanceType.CRIT_REDUCTION      to "Crit Reduction",
        ResistanceType.DAMAGE_DECREASE     to "Damage Decrease",
        ResistanceType.DAZE                to "Daze",
        ResistanceType.DOT                 to "DoT",
        ResistanceType.HEAL_DECREASE       to "Heal Decrease",
        ResistanceType.REND                to "Rend",
        ResistanceType.RESISTANCE_DECREASE to "Resistance Decrease",
        ResistanceType.SPEED_DECREASE      to "Speed Decrease",
        ResistanceType.STUN                to "Stun",
        ResistanceType.SWAP_PREVENTION     to "Swap Prevention",
        ResistanceType.TAUNT               to "Taunt",
        ResistanceType.VULNERABLE          to "Vulnerability",
    )
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text("Reset") },
            )
        }
        items(resistances) { (type, label) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text(label) },
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
