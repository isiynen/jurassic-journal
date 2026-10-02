package com.sufficienteffort.jurassicjournal.ui.dino

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.sufficienteffort.jurassicjournal.data.model.DinoClass
import com.sufficienteffort.jurassicjournal.data.model.Rarity
import com.sufficienteffort.jurassicjournal.data.model.ResistanceType
import com.sufficienteffort.jurassicjournal.data.model.SpawnLocation
import com.sufficienteffort.jurassicjournal.data.model.displayName
import com.sufficienteffort.jurassicjournal.data.model.label
import com.sufficienteffort.jurassicjournal.ui.components.classColor
import com.sufficienteffort.jurassicjournal.ui.components.rarityColor

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

/** Horizontally scrolling chip strip with the list's standard padding. */
@Composable
private fun ChipRow(content: LazyListScope.() -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(start = 8.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** Tinted multi-select chip: translucent [color] container when selected. */
@Composable
private fun TintedChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color.copy(alpha = 0.25f),
            selectedLabelColor = color,
        ),
    )
}

@Composable
fun RarityFilterRow(
    selected: Set<Rarity>,
    onToggle: (Rarity) -> Unit,
    onClear: () -> Unit,
) {
    ChipRow {
        item { FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text("All") }) }
        items(Rarity.entries) { rarity ->
            TintedChip(rarity.label(), rarity in selected, rarityColor(rarity)) { onToggle(rarity) }
        }
    }
}

@Composable
fun ClassFilterRow(
    selected: Set<DinoClass>,
    onToggle: (DinoClass) -> Unit,
    onClear: () -> Unit,
) {
    ChipRow {
        item { FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text("All") }) }
        items(DinoClass.entries) { cls ->
            TintedChip(cls.label(), cls in selected, classColor(cls)) { onToggle(cls) }
        }
    }
}

@Composable
fun NewFilterRow(newCount: Int, selected: Boolean, onToggle: () -> Unit) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    ChipRow {
        item { TintedChip("New ($newCount)", selected, tertiary, onToggle) }
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
    ChipRow {
        item { FilterChip(selected = selected.isEmpty(), onClick = onClear, label = { Text("All") }) }
        items(locations) { loc ->
            FilterChip(
                selected = loc in selected,
                onClick = { onToggle(loc) },
                label = { Text(loc.displayName()) },
            )
        }
    }
}

@Composable
fun StatSortRow(selected: StatSortMode?, onSelect: (StatSortMode?) -> Unit) {
    ChipRow {
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Reset") }) }
        items(StatSortMode.entries) { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelect(if (selected == mode) null else mode) },
                label = { Text(mode.label) },
            )
        }
    }
}

@Composable
fun ResistanceSortRow(selected: ResistanceType?, onSelect: (ResistanceType?) -> Unit) {
    val resistances = remember { ResistanceType.entries.sortedBy { it.displayName() } }
    ChipRow {
        item { FilterChip(selected = selected == null, onClick = { onSelect(null) }, label = { Text("Reset") }) }
        items(resistances) { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(if (selected == type) null else type) },
                label = { Text(type.displayName()) },
            )
        }
    }
}
