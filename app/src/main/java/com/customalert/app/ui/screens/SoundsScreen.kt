package com.customalert.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.customalert.app.data.SoundAsset
import com.customalert.app.data.SoundKind
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundsScreen(
    sounds: List<SoundAsset>,
    onBack: () -> Unit,
    onImport: (Uri, (String) -> Unit) -> Unit,
    onPreview: (SoundAsset) -> Unit,
    onDelete: (String) -> Unit
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onImport(uri) { message ->
                scope.launch { snackbar.showSnackbar(message) }
            }
        }
    }

    val builtins = sounds.filter { it.kind == SoundKind.BUILTIN }
    val customs = sounds.filter { it.kind == SoundKind.CUSTOM }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Sounds") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { picker.launch(arrayOf("audio/*")) }
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Import sound")
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    "Built-in",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Text(
                    "Included sounds provided by Universfield.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp)
                )
            }
            items(builtins, key = { it.id }) { sound ->
                SoundRow(sound, onPreview = { onPreview(sound) }, onDelete = null)
            }
            item {
                Text(
                    "Imported from device",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (customs.isEmpty()) {
                item {
                    Text(
                        "No custom sounds yet. Tap the upload button to import from storage.",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(customs, key = { it.id }) { sound ->
                SoundRow(
                    sound,
                    onPreview = { onPreview(sound) },
                    onDelete = { onDelete(sound.id) }
                )
            }
        }
    }
}

@Composable
private fun SoundRow(
    sound: SoundAsset,
    onPreview: () -> Unit,
    onDelete: (() -> Unit)?
) {
    ListItem(
        headlineContent = { Text(sound.displayName) },
        supportingContent = {
            Text(if (sound.kind == SoundKind.BUILTIN) "Built-in" else "Custom")
        },
        trailingContent = {
            Row {
                IconButton(onClick = onPreview) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Preview")
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    )
}
