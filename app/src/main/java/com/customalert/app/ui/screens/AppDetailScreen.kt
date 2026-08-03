package com.customalert.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.customalert.app.data.AppMapping
import com.customalert.app.data.Rule
import com.customalert.app.data.SoundAsset
import com.customalert.app.ui.components.SoundPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    packageName: String,
    label: String,
    mapping: AppMapping?,
    rules: List<Rule>,
    sounds: List<SoundAsset>,
    onBack: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onDefaultSoundChange: (String?) -> Unit,
    onAddRule: () -> Unit,
    onEditRule: (String) -> Unit,
    onDeleteRule: (String) -> Unit,
    onMoveRule: (Int, Int) -> Unit
) {
    val enabled = mapping?.enabled ?: true

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddRule) {
                Icon(Icons.Default.Add, contentDescription = "Add rule")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(packageName, style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable for this app", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "When off, only global rules can still match.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }
                Text("Default sound", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Used for all notifications from this app when no rule matches.",
                    style = MaterialTheme.typography.bodySmall
                )
                SoundPickerField(
                    label = "Sound",
                    sounds = sounds,
                    selectedSoundId = mapping?.defaultSoundId,
                    allowNone = true,
                    onSoundSelected = onDefaultSoundChange
                )
                Text(
                    "Rules (higher in list = higher priority)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (rules.isEmpty()) {
                    item {
                        Text(
                            "No rules yet. Add one to match notification text.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                itemsIndexed(rules, key = { _, rule -> rule.id }) { index, rule ->
                    ListItem(
                        headlineContent = { Text(rule.name) },
                        supportingContent = {
                            Text(
                                "Contains \"${rule.pattern}\" · ${rule.matchField.name.lowercase()}"
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(
                                    onClick = { onMoveRule(index, index - 1) },
                                    enabled = index > 0
                                ) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                                }
                                IconButton(
                                    onClick = { onMoveRule(index, index + 1) },
                                    enabled = index < rules.lastIndex
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move down"
                                    )
                                }
                                IconButton(onClick = { onDeleteRule(rule.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                                }
                            }
                        },
                        modifier = Modifier.clickable { onEditRule(rule.id) }
                    )
                }
            }
        }
    }
}
