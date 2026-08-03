package com.customalert.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.customalert.app.data.SoundAsset

data class PickerOption(
    val id: String?,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundPickerField(
    label: String,
    sounds: List<SoundAsset>,
    selectedSoundId: String?,
    allowNone: Boolean = false,
    noneLabel: String = "None (rules only)",
    onSoundSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = remember(sounds, allowNone, noneLabel) {
        buildList {
            if (allowNone) add(PickerOption(null, noneLabel))
            sounds.forEach { add(PickerOption(it.id, it.displayName)) }
        }
    }

    OptionPickerField(
        label = label,
        options = options,
        selectedId = selectedSoundId,
        fallbackLabel = if (allowNone) noneLabel else "Select sound",
        onSelected = onSoundSelected,
        sheetTitle = "Choose sound",
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptionPickerField(
    label: String,
    options: List<PickerOption>,
    selectedId: String?,
    fallbackLabel: String,
    onSelected: (String?) -> Unit,
    sheetTitle: String,
    modifier: Modifier = Modifier
) {
    var showSheet by remember { mutableStateOf(false) }
    // Keep a local selection so brief null/stale parent emissions don't flicker the label.
    var localSelectedId by remember { mutableStateOf(selectedId) }
    LaunchedEffect(selectedId) {
        localSelectedId = selectedId
    }

    val selectedLabel = options.firstOrNull { it.id == localSelectedId }?.label
        ?: fallbackLabel

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true },
            shape = RoundedCornerShape(4.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            Text(
                text = sheetTitle,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            HorizontalDivider()
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(options, key = { it.id ?: "__none__" }) { option ->
                    val selected = option.id == localSelectedId
                    ListItem(
                        headlineContent = { Text(option.label) },
                        trailingContent = {
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier.clickable {
                            localSelectedId = option.id
                            onSelected(option.id)
                            showSheet = false
                        }
                    )
                }
            }
        }
    }
}
