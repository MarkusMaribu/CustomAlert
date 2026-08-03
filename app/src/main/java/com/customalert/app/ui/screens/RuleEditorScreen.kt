package com.customalert.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.customalert.app.data.MatchField
import com.customalert.app.data.Rule
import com.customalert.app.data.SoundAsset
import com.customalert.app.ui.components.OptionPickerField
import com.customalert.app.ui.components.PickerOption
import com.customalert.app.ui.components.SoundPickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleEditorScreen(
    title: String,
    existing: Rule?,
    sounds: List<SoundAsset>,
    onBack: () -> Unit,
    onSave: (
        name: String,
        pattern: String,
        matchField: MatchField,
        soundId: String,
        enabled: Boolean
    ) -> Unit,
    onPreviewSound: (SoundAsset) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var pattern by remember(existing?.id) { mutableStateOf(existing?.pattern.orEmpty()) }
    var matchField by remember(existing?.id) {
        mutableStateOf(existing?.matchField ?: MatchField.BOTH)
    }
    var soundId by remember(existing?.id) {
        mutableStateOf(existing?.soundId ?: sounds.firstOrNull()?.id.orEmpty())
    }
    var enabled by remember(existing?.id) { mutableStateOf(existing?.enabled ?: true) }

    val matchOptions = remember {
        listOf(
            PickerOption(MatchField.BOTH.name, "Title or text"),
            PickerOption(MatchField.TITLE.name, "Title only"),
            PickerOption(MatchField.TEXT.name, "Text only")
        )
    }

    val canSave = pattern.isNotBlank() && soundId.isNotBlank()

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Rule name (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("Contains text") },
                supportingText = { Text("Case-insensitive. Example: Banana") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OptionPickerField(
                label = "Match field",
                options = matchOptions,
                selectedId = matchField.name,
                fallbackLabel = "Title or text",
                onSelected = { id ->
                    matchField = MatchField.valueOf(id ?: MatchField.BOTH.name)
                },
                sheetTitle = "Match field"
            )

            SoundPickerField(
                label = "Sound",
                sounds = sounds,
                selectedSoundId = soundId.takeIf { it.isNotBlank() },
                allowNone = false,
                onSoundSelected = { id ->
                    if (id != null) soundId = id
                },
                onPreviewSound = onPreviewSound
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Enabled")
                Switch(checked = enabled, onCheckedChange = { enabled = it })
            }

            Button(
                onClick = { onSave(name, pattern, matchField, soundId, enabled) },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save rule")
            }
        }
    }
}
