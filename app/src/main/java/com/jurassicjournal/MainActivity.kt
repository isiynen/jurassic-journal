package com.jurassicjournal

import android.content.Context
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.jurassicjournal.data.update.AbilityIconSync
import com.jurassicjournal.data.update.DinoImageSync
import com.jurassicjournal.data.update.NewDinoDetector
import com.jurassicjournal.data.update.SyncProgressTracker
import com.jurassicjournal.data.update.UpdateInfo
import com.jurassicjournal.ui.update.UpdateProgressStrip
import com.jurassicjournal.ui.calculator.HybridCalculatorScreen
import com.jurassicjournal.ui.enhancement.EnhancementEstimatorScreen
import com.jurassicjournal.ui.dino.DinoDetailScreen
import com.jurassicjournal.ui.dino.DinoListScreen
import com.jurassicjournal.ui.navigation.Screen
import com.jurassicjournal.ui.profile.ManageProfilesScreen
import com.jurassicjournal.ui.sanctuary.SanctuaryCalculatorScreen
import com.jurassicjournal.ui.team.ManageTeamsScreen
import com.jurassicjournal.ui.team.TeamDetailScreen
import com.jurassicjournal.ui.team.TeamDinoPickerScreen
import com.jurassicjournal.ui.theme.JurassicJournalTheme
import com.jurassicjournal.ui.update.UpdateCheckViewModel
import com.jurassicjournal.ui.update.UpdateState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateVm: UpdateCheckViewModel by viewModels()

    @Inject lateinit var newDinoDetector: NewDinoDetector
    @Inject lateinit var dinoImageSync: DinoImageSync
    @Inject lateinit var abilityIconSync: AbilityIconSync
    @Inject lateinit var syncProgressTracker: SyncProgressTracker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch(Dispatchers.IO) { newDinoDetector.detect() }
        lifecycleScope.launch(Dispatchers.IO) { dinoImageSync.syncMissingImages() }
        lifecycleScope.launch(Dispatchers.IO) { abilityIconSync.syncMissingIcons() }
        enableEdgeToEdge()
        setContent {
            JurassicJournalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        JurassicJournalNav()

                        val syncProgress by syncProgressTracker.progress.collectAsState()
                        UpdateProgressStrip(
                            progress = syncProgress,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    val updateInfo by updateVm.updateInfo.collectAsState()
                    val updateState by updateVm.state.collectAsState()

                    UpdatePromptOverlay(
                        updateInfo  = updateInfo,
                        updateState = updateState,
                        onConfirm   = updateVm::confirmUpdate,
                        onDismiss   = updateVm::dismissUpdate,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatePromptOverlay(
    updateInfo:  UpdateInfo?,
    updateState: UpdateState,
    onConfirm:   () -> Unit,
    onDismiss:   () -> Unit,
) {
    when {
        updateState == UpdateState.RestartReady -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) { restartApp(context) }
        }

        updateInfo != null && updateState == UpdateState.Idle -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Database Update Available") },
                text  = {
                    Text(
                        "A new dino database (${updateInfo.tag}) is available.\n\n" +
                        "The app will restart automatically after installing."
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) { Text("Install") }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Later") }
                },
            )
        }
    }
}

private fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)!!
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}

private fun androidx.navigation.NavController.navigateSafe(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        navigate(route)
    }
}

private fun androidx.navigation.NavController.popBackStackSafe() {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

@Composable
private fun JurassicJournalNav() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.DinoList.route,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) },
    ) {
        composable(Screen.DinoList.route) {
            DinoListScreen(
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId).route) },
                onManageProfiles = { navController.navigateSafe(Screen.ManageProfiles.route) },
                onManageTeams = { navController.navigateSafe(Screen.ManageTeams.route) },
                onTeamClick = { teamId -> navController.navigateSafe(Screen.TeamDetail(teamId).route) },
            )
        }
        composable(Screen.ManageProfiles.route) {
            ManageProfilesScreen(onBack = { navController.popBackStackSafe() })
        }
        composable(Screen.ManageTeams.route) {
            ManageTeamsScreen(
                onBack = { navController.popBackStackSafe() },
                onTeamClick = { teamId -> navController.navigateSafe(Screen.TeamDetail(teamId).route) },
                onEditMembers = { teamId -> navController.navigateSafe(Screen.TeamDinoPicker(teamId).route) },
            )
        }
        composable(
            route = Screen.TeamDetail.ROUTE,
            arguments = listOf(navArgument("teamId") { type = NavType.LongType })
        ) { entry ->
            val teamId = entry.arguments?.getLong("teamId") ?: 0L
            TeamDetailScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId).route) },
                onEditMembers = { navController.navigateSafe(Screen.TeamDinoPicker(teamId).route) },
            )
        }
        composable(
            route = Screen.TeamDinoPicker.ROUTE,
            arguments = listOf(navArgument("teamId") { type = NavType.LongType })
        ) {
            TeamDinoPickerScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId, hideTeams = true).route) },
            )
        }
        composable(
            route = Screen.DinoDetail.ROUTE,
            arguments = listOf(
                navArgument("dinoId") { type = NavType.LongType },
                navArgument("hideTeams") { type = NavType.BoolType; defaultValue = false },
            )
        ) { entry ->
            val hideTeams = entry.arguments?.getBoolean("hideTeams") ?: false
            DinoDetailScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId).route) },
                onCalculate = { dinoId -> navController.navigateSafe(Screen.HybridCalculator(dinoId).route) },
                onSanctuaryCalculate = { dinoId -> navController.navigateSafe(Screen.SanctuaryCalculator(dinoId).route) },
                onEnhancementEstimate = { dinoId, current -> navController.navigateSafe(Screen.EnhancementEstimator(dinoId, current).route) },
                showTeamSelector = !hideTeams,
            )
        }
        composable(
            route = Screen.HybridCalculator.ROUTE,
            arguments = listOf(navArgument("dinoId") { type = NavType.LongType })
        ) {
            HybridCalculatorScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId).route) },
            )
        }
        composable(
            route = Screen.SanctuaryCalculator.ROUTE,
            arguments = listOf(navArgument("dinoId") { type = NavType.LongType })
        ) {
            SanctuaryCalculatorScreen(onBack = { navController.popBackStackSafe() })
        }
        composable(
            route = Screen.EnhancementEstimator.ROUTE,
            arguments = listOf(
                navArgument("dinoId") { type = NavType.LongType },
                navArgument("currentEnhancement") { type = NavType.IntType },
            )
        ) {
            EnhancementEstimatorScreen(onBack = { navController.popBackStackSafe() })
        }
    }
}
