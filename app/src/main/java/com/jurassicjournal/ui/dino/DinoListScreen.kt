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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.repository.DinoSearchResult
import com.jurassicjournal.data.model.DinoClass
import com.jurassicjournal.data.model.Rarity
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
    viewModel: DinoListViewModel = hiltViewModel(),
) {
    val results by viewModel.results.collectAsState()
    val filters by viewModel.filters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jurassic Journal", style = MaterialTheme.typography.titleLarge) },
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

            Spacer(Modifier.height(4.dp))

            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No dinosaurs found", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(results, key = { it.dino.id }) { result ->
                        DinoCard(
                            dino = result.dino,
                            matchedMoves = result.matchedMoves,
                            onClick = { onDinoClick(result.dino.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
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
private fun RarityFilterRow(selected: Rarity?, onSelect: (Rarity?) -> Unit) {
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
private fun ClassFilterRow(selected: DinoClass?, onSelect: (DinoClass?) -> Unit) {
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
private fun DinoCard(dino: Dino, matchedMoves: List<String> = emptyList(), onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
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
                Text(dino.name, style = MaterialTheme.typography.titleMedium)
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
    }
}

@Composable
private fun RarityChip(rarity: Rarity) {
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
private fun ClassChip(dinoClass: DinoClass) {
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
