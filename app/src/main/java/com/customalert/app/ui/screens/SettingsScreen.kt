package com.customalert.app.ui.screens

import android.app.Activity
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.customalert.app.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferReplace: Boolean,
    monitoringEnabled: Boolean,
    onPreferReplaceChange: (Boolean) -> Unit,
    onMonitoringChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var listenerEnabled by remember {
        mutableStateOf(PermissionUtils.isNotificationListenerEnabled(context))
    }
    var batteryOk by remember {
        mutableStateOf(PermissionUtils.isIgnoringBatteryOptimizations(context))
    }
    var notifPermission by remember {
        mutableStateOf(PermissionUtils.hasPostNotificationsPermission(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerEnabled = PermissionUtils.isNotificationListenerEnabled(context)
                batteryOk = PermissionUtils.isIgnoringBatteryOptimizations(context)
                notifPermission = PermissionUtils.hasPostNotificationsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        notifPermission = PermissionUtils.hasPostNotificationsPermission(context)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ToggleRow(
                        title = "Monitoring",
                        subtitle = "Silent foreground service keeps CustomAlert awake",
                        checked = monitoringEnabled,
                        onCheckedChange = onMonitoringChange
                    )
                    ToggleRow(
                        title = "Prefer replace when possible",
                        subtitle = "Cancel and mirror safe notifications, then play your sound. Falls back to playing alongside the original.",
                        checked = preferReplace,
                        onCheckedChange = onPreferReplaceChange
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Permissions", style = MaterialTheme.typography.titleMedium)
                    StatusLine("Notification access", listenerEnabled)
                    OutlinedButton(
                        onClick = { PermissionUtils.openNotificationListenerSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open notification access settings")
                    }

                    StatusLine("Post notifications", notifPermission)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notifPermission) {
                        OutlinedButton(
                            onClick = {
                                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Allow notifications")
                        }
                    }

                    StatusLine("Battery unrestricted", batteryOk)
                    OutlinedButton(
                        onClick = {
                            if (!batteryOk) {
                                PermissionUtils.requestIgnoreBatteryOptimizations(activity)
                            } else {
                                PermissionUtils.openBatterySettings(context)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (batteryOk) "Battery settings" else "Allow unrestricted battery")
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Smoke-test checklist", style = MaterialTheme.typography.titleMedium)
                    Text("1. Enable notification access and monitoring.")
                    Text("2. Set a default sound for Messages (or any app) and send yourself a notification.")
                    Text("3. Add a rule containing a unique word and confirm only those notifications use that sound.")
                    Text("4. Turn the screen off, wait 1â€“2 minutes, trigger another notification â€” sound should still play.")
                    Text("5. Confirm the monitoring notification is silent (no sound/vibration).")
                    Text("6. Import a custom .ogg/.mp3 from storage and preview it.")
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusLine(label: String, ok: Boolean) {
    Text(
        "$label: ${if (ok) "ready" else "action needed"}",
        color = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.labelLarge
    )
}
