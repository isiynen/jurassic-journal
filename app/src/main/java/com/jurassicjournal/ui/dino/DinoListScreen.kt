package com.jurassicjournal.ui.dino

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import com.jurassicjournal.R
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.repository.DinoSearchResult
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.Rarity
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
    val results by viewModel.results.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val newCount by viewModel.newCount.collectAsState()
    val barState by profileBarViewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val currentProfileId by rememberUpdatedState(barState.activeProfileId)
    var scrolledForProfileId by remember { mutableLongStateOf(-1L) }
    var showSupportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(results) {
        if (currentProfileId != scrolledForProfileId) {
            scrolledForProfileId = currentProfileId
            listState.scrollToItem(0)
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
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    Text("No dinosaurs found", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.dino.id }) { result ->
                        DinoCard(
                            dino = result.dino,
                            matchedMoves = result.matchedMoves,
                            isNew = result.isNew,
                            onClick = { onDinoClick(result.dino.id) },
                        )
                    }
                }
            }
        }
    }

    if (showSupportDialog) {
        SupportDialog(onDismiss = { showSupportDialog = false })
    }
}

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
                                androidx.compose.ui.text.font.FontWeight.Bold else null)
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
        contentPadding = PaddingValues(horizontal = 16.dp),
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("All classes") })
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
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
                        .data("file:///android_asset/dinosaurs/${dino.imagePath}")
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
                        Text(dino.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
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
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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

@Composable
fun RarityChip(rarity: Rarity) {
    val color = rarityColor(rarity)
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
    ) {
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
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.15f),
    ) {
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
