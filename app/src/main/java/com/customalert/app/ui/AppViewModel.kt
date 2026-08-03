package com.customalert.app.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.customalert.app.CustomAlertApp
import com.customalert.app.data.AppMapping
import com.customalert.app.data.MatchField
import com.customalert.app.data.Rule
import com.customalert.app.data.RuleScope
import com.customalert.app.data.SoundAsset
import com.customalert.app.domain.SoundResolver
import com.customalert.app.service.MonitorForegroundService
import com.customalert.app.service.SoundPlayer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class InstalledApp(
    val packageName: String,
    val label: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = (application as CustomAlertApp).repository
    private val soundPlayer = SoundPlayer(application)

    val sounds: StateFlow<List<SoundAsset>> = repo.observeSounds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val mappedApps: StateFlow<List<AppMapping>> = repo.observeApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val globalRules: StateFlow<List<Rule>> = repo.observeGlobalRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val preferReplace: StateFlow<Boolean> = repo.preferences.preferReplace
    val onboardingDone: StateFlow<Boolean> = repo.preferences.onboardingDone
    val monitoringEnabled: StateFlow<Boolean> = repo.preferences.monitoringEnabled

    // Cache per-package flows so recomposition doesn't recreate StateFlows with
    // null/empty initial values (that caused the selected-sound flicker).
    private val appMappingFlows = mutableMapOf<String, StateFlow<AppMapping?>>()
    private val appRuleFlows = mutableMapOf<String, StateFlow<List<Rule>>>()

    fun observeAppRules(packageName: String): StateFlow<List<Rule>> =
        appRuleFlows.getOrPut(packageName) {
            repo.observeAppRules(packageName)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    fun observeApp(packageName: String): StateFlow<AppMapping?> =
        appMappingFlows.getOrPut(packageName) {
            repo.observeApp(packageName)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    fun loadInstalledApps(): List<InstalledApp> {
        val pm = getApplication<Application>().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == getApplication<Application>().packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString() ?: pkg
                InstalledApp(pkg, label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    fun appLabel(packageName: String): String {
        val pm = getApplication<Application>().packageManager
        return try {
            val ai: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    fun completeOnboarding() {
        repo.preferences.setOnboardingDone(true)
        setMonitoring(true)
    }

    fun setPreferReplace(value: Boolean) {
        repo.preferences.setPreferReplace(value)
    }

    fun setMonitoring(enabled: Boolean) {
        repo.preferences.setMonitoringEnabled(enabled)
        val context = getApplication<Application>()
        if (enabled) {
            MonitorForegroundService.start(context)
        } else {
            MonitorForegroundService.stop(context)
        }
    }

    fun setAppEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.setAppEnabled(packageName, appLabel(packageName), enabled)
        }
    }

    fun ensureApp(packageName: String) {
        viewModelScope.launch {
            repo.ensureAppMapping(packageName, appLabel(packageName))
        }
    }

    fun setAppDefaultSound(packageName: String, soundId: String?) {
        viewModelScope.launch {
            repo.setAppDefaultSound(packageName, appLabel(packageName), soundId)
        }
    }

    suspend fun loadRule(id: String): Rule? = repo.getRule(id)

    fun saveRule(
        id: String?,
        scope: RuleScope,
        packageName: String?,
        name: String,
        pattern: String,
        matchField: MatchField,
        soundId: String,
        enabled: Boolean,
        priority: Int?
    ) {
        viewModelScope.launch {
            val ruleId = id ?: UUID.randomUUID().toString()
            val resolvedPriority = priority
                ?: if (id == null) repo.nextPriority(packageName) else {
                    repo.getRule(ruleId)?.priority ?: repo.nextPriority(packageName)
                }
            repo.saveRule(
                Rule(
                    id = ruleId,
                    scope = scope,
                    packageName = if (scope == RuleScope.APP) packageName else null,
                    name = name.ifBlank { pattern },
                    pattern = pattern,
                    matchField = matchField,
                    soundId = soundId,
                    priority = resolvedPriority,
                    enabled = enabled
                )
            )
        }
    }

    fun deleteRule(id: String) {
        viewModelScope.launch { repo.deleteRule(id) }
    }

    fun moveRule(rules: List<Rule>, from: Int, to: Int) {
        if (from == to || from !in rules.indices || to !in rules.indices) return
        val mutable = rules.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        viewModelScope.launch { repo.reorderRules(mutable) }
    }

    fun importSound(uri: Uri, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repo.importSound(uri)
            } catch (e: Exception) {
                onError(e.message ?: "Import failed")
            }
        }
    }

    fun deleteSound(id: String) {
        viewModelScope.launch { repo.deleteCustomSound(id) }
    }

    fun previewSound(asset: SoundAsset) {
        val uri = SoundResolver.resolveUri(getApplication(), asset) ?: return
        // Allow auditioning in the UI even if DND is on.
        soundPlayer.play(uri, respectInterruptionFilter = false)
    }

    fun stopPreview() {
        soundPlayer.stop()
    }

    override fun onCleared() {
        soundPlayer.release()
        super.onCleared()
    }
}
