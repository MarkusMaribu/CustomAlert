package com.customalert.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.customalert.app.util.PermissionUtils

@Composable
fun HomeScreen(
    monitoringEnabled: Boolean,
    onMonitoringChange: (Boolean) -> Unit,
    onOpenApps: () -> Unit,
    onOpenGlobalRules: () -> Unit,
    onOpenSounds: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var listenerEnabled by remember {
        mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = PermissionUtils.isNotificationListenerEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("CustomAlert", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Custom sounds for any app's notifications.",
            style = MaterialTheme.typography.bodyLarge
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Monitoring", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (!listenerEnabled) "Notification access is off â€” open Settings"
                            else if (monitoringEnabled) "Listening for matching notifications"
                            else "Paused",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Switch(
                        checked = monitoringEnabled && listenerEnabled,
                        onCheckedChange = onMonitoringChange,
                        enabled = listenerEnabled
                    )
                }
            }
        }

        NavCard(Icons.Default.Apps, "Apps", "Per-app default sounds and rules", onOpenApps)
        NavCard(
            Icons.AutoMirrored.Filled.List,
            "Global rules",
            "Rules that apply to every app",
            onOpenGlobalRules
        )
        NavCard(Icons.Default.MusicNote, "Sounds", "Built-in tones and your imports", onOpenSounds)
        NavCard(Icons.Default.Settings, "Settings", "Replace behavior, permissions, battery", onOpenSettings)
    }
}

@Composable
private fun NavCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
