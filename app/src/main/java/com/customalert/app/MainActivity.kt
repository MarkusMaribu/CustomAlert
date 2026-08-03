package com.customalert.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.customalert.app.data.Rule
import com.customalert.app.data.RuleScope
import com.customalert.app.ui.AppViewModel
import com.customalert.app.ui.screens.AppDetailScreen
import com.customalert.app.ui.screens.AppsScreen
import com.customalert.app.ui.screens.GlobalRulesScreen
import com.customalert.app.ui.screens.HomeScreen
import com.customalert.app.ui.screens.OnboardingScreen
import com.customalert.app.ui.screens.RuleEditorScreen
import com.customalert.app.ui.screens.SettingsScreen
import com.customalert.app.ui.screens.SoundsScreen
import com.customalert.app.ui.theme.CustomAlertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CustomAlertTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Keep all screens clear of status and navigation bars under edge-to-edge.
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        CustomAlertNav()
                    }
                }
            }
        }
    }
}

private object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Apps = "apps"
    const val AppDetail = "app/{packageName}"
    const val GlobalRules = "global_rules"
    const val Sounds = "sounds"
    const val Settings = "settings"
    const val RuleEditor = "rule_editor?scope={scope}&packageName={packageName}&ruleId={ruleId}"

    fun appDetail(packageName: String) = "app/$packageName"
    fun ruleEditor(scope: RuleScope, packageName: String? = null, ruleId: String? = null): String {
        val pkg = packageName ?: "none"
        val id = ruleId ?: "new"
        return "rule_editor?scope=${scope.name}&packageName=$pkg&ruleId=$id"
    }
}

@Composable
private fun CustomAlertNav(vm: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val onboardingDone by vm.onboardingDone.collectAsStateWithLifecycle()
    val monitoringEnabled by vm.monitoringEnabled.collectAsStateWithLifecycle()
    val preferReplace by vm.preferReplace.collectAsStateWithLifecycle()
    val sounds by vm.sounds.collectAsStateWithLifecycle()
    val mappedApps by vm.mappedApps.collectAsStateWithLifecycle()
    val globalRules by vm.globalRules.collectAsStateWithLifecycle()

    val start = if (onboardingDone) Routes.Home else Routes.Onboarding

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                onFinished = {
                    vm.completeOnboarding()
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                monitoringEnabled = monitoringEnabled,
                onMonitoringChange = vm::setMonitoring,
                onOpenApps = { navController.navigate(Routes.Apps) },
                onOpenGlobalRules = { navController.navigate(Routes.GlobalRules) },
                onOpenSounds = { navController.navigate(Routes.Sounds) },
                onOpenSettings = { navController.navigate(Routes.Settings) }
            )
        }
        composable(Routes.Apps) {
            val installed = remember { vm.loadInstalledApps() }
            AppsScreen(
                installedApps = installed,
                mappedApps = mappedApps,
                onBack = { navController.popBackStack() },
                onOpenApp = { pkg ->
                    vm.ensureApp(pkg)
                    navController.navigate(Routes.appDetail(pkg))
                }
            )
        }
        composable(
            route = Routes.AppDetail,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { entry ->
            val packageName = entry.arguments?.getString("packageName").orEmpty()
            val appFlow = remember(packageName) { vm.observeApp(packageName) }
            val rulesFlow = remember(packageName) { vm.observeAppRules(packageName) }
            val mapping by appFlow.collectAsStateWithLifecycle()
            val rules by rulesFlow.collectAsStateWithLifecycle()
            val label = remember(packageName) { vm.appLabel(packageName) }
            AppDetailScreen(
                packageName = packageName,
                label = label,
                mapping = mapping,
                rules = rules,
                sounds = sounds,
                onBack = { navController.popBackStack() },
                onEnabledChange = { vm.setAppEnabled(packageName, it) },
                onDefaultSoundChange = { vm.setAppDefaultSound(packageName, it) },
                onAddRule = {
                    navController.navigate(Routes.ruleEditor(RuleScope.APP, packageName, null))
                },
                onEditRule = { ruleId ->
                    navController.navigate(Routes.ruleEditor(RuleScope.APP, packageName, ruleId))
                },
                onDeleteRule = vm::deleteRule,
                onMoveRule = { from, to -> vm.moveRule(rules, from, to) },
                onPreviewSound = vm::previewSound
            )
        }
        composable(Routes.GlobalRules) {
            GlobalRulesScreen(
                rules = globalRules,
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.ruleEditor(RuleScope.GLOBAL)) },
                onEdit = { ruleId ->
                    navController.navigate(Routes.ruleEditor(RuleScope.GLOBAL, ruleId = ruleId))
                },
                onDelete = vm::deleteRule,
                onMove = { from, to -> vm.moveRule(globalRules, from, to) }
            )
        }
        composable(Routes.Sounds) {
            SoundsScreen(
                sounds = sounds,
                onBack = { navController.popBackStack() },
                onImport = vm::importSound,
                onPreview = vm::previewSound,
                onDelete = vm::deleteSound
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                preferReplace = preferReplace,
                monitoringEnabled = monitoringEnabled,
                onPreferReplaceChange = vm::setPreferReplace,
                onMonitoringChange = vm::setMonitoring,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.RuleEditor,
            arguments = listOf(
                navArgument("scope") { type = NavType.StringType },
                navArgument("packageName") {
                    type = NavType.StringType
                    defaultValue = "none"
                },
                navArgument("ruleId") {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) { entry ->
            val scope = RuleScope.valueOf(
                entry.arguments?.getString("scope") ?: RuleScope.GLOBAL.name
            )
            val packageName = entry.arguments?.getString("packageName")?.takeIf { it != "none" }
            val ruleId = entry.arguments?.getString("ruleId")?.takeIf { it != "new" }

            var existing by remember(ruleId) { mutableStateOf<Rule?>(null) }
            LaunchedEffect(ruleId) {
                existing = ruleId?.let { vm.loadRule(it) }
            }

            RuleEditorScreen(
                title = if (ruleId == null) "New rule" else "Edit rule",
                existing = existing,
                sounds = sounds,
                onBack = { navController.popBackStack() },
                onSave = { name, pattern, matchField, soundId, enabled ->
                    vm.saveRule(
                        id = ruleId,
                        scope = scope,
                        packageName = packageName,
                        name = name,
                        pattern = pattern,
                        matchField = matchField,
                        soundId = soundId,
                        enabled = enabled,
                        priority = existing?.priority
                    )
                    navController.popBackStack()
                },
                onPreviewSound = vm::previewSound
            )
        }
    }
}
