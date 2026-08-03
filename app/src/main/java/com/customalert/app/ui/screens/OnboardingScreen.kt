package com.customalert.app.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.customalert.app.util.PermissionUtils

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
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
    ) { granted ->
        notifPermission = granted || PermissionUtils.hasPostNotificationsPermission(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome to CustomAlert", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Play your own sounds for notifications from any app - for the whole app or only when the text matches a rule.",
            style = MaterialTheme.typography.bodyLarge
        )

        SetupCard(
            title = "1. Notification access",
            body = "Required so CustomAlert can see notifications and decide which sound to play. Enable CustomAlert in the list.",
            done = listenerEnabled,
            actionLabel = if (listenerEnabled) "Enabled" else "Open settings",
            onAction = { PermissionUtils.openNotificationListenerSettings(context) },
            enabled = !listenerEnabled
        )

        SetupCard(
            title = "2. Post notifications",
            body = "Needed for the silent monitoring indicator and mirrored alerts.",
            done = notifPermission,
            actionLabel = if (notifPermission) "Granted" else "Allow",
            onAction = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            enabled = !notifPermission
        )

        SetupCard(
            title = "3. Unrestricted battery",
            body = "Stops the system from pausing CustomAlert while the screen is off. Recommended for reliable sounds.",
            done = batteryOk,
            actionLabel = if (batteryOk) "Unrestricted" else "Allow",
            onAction = { PermissionUtils.requestIgnoreBatteryOptimizations(activity) },
            enabled = !batteryOk
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onFinished,
            enabled = listenerEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Continue")
        }

        if (!listenerEnabled) {
            Text(
                "Notification access is required to continue.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (!batteryOk) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue without battery exemption")
            }
        }
    }
}

@Composable
private fun SetupCard(
    title: String,
    body: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (done) "Status: ready" else "Status: action needed",
                style = MaterialTheme.typography.labelLarge,
                color = if (done) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            OutlinedButton(onClick = onAction, enabled = enabled) {
                Text(actionLabel)
            }
        }
    }
}
