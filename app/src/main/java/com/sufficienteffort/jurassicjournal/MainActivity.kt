package com.sufficienteffort.jurassicjournal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.sufficienteffort.jurassicjournal.data.update.AbilityIconSync
import com.sufficienteffort.jurassicjournal.data.update.DinoImageSync
import com.sufficienteffort.jurassicjournal.data.update.NewDinoDetector
import com.sufficienteffort.jurassicjournal.data.update.SyncProgressTracker
import com.sufficienteffort.jurassicjournal.data.update.UpdateInfo
import com.sufficienteffort.jurassicjournal.data.user.ActiveProfileRepository
import com.sufficienteffort.jurassicjournal.ui.calculator.HybridCalculatorScreen
import com.sufficienteffort.jurassicjournal.ui.calculator.LevelUpCalculatorScreen
import com.sufficienteffort.jurassicjournal.ui.dino.DinoDetailScreen
import com.sufficienteffort.jurassicjournal.ui.dino.DinoListScreen
import com.sufficienteffort.jurassicjournal.ui.enhancement.EnhancementEstimatorScreen
import com.sufficienteffort.jurassicjournal.ui.navigation.Screen
import com.sufficienteffort.jurassicjournal.ui.profile.ManageProfilesScreen
import com.sufficienteffort.jurassicjournal.ui.sanctuary.SanctuaryCalculatorScreen
import com.sufficienteffort.jurassicjournal.ui.team.ManageTeamsScreen
import com.sufficienteffort.jurassicjournal.ui.team.TeamDetailScreen
import com.sufficienteffort.jurassicjournal.ui.team.TeamDinoPickerScreen
import com.sufficienteffort.jurassicjournal.ui.theme.JurassicJournalTheme
import com.sufficienteffort.jurassicjournal.ui.update.UpdateCheckViewModel
import com.sufficienteffort.jurassicjournal.ui.update.UpdateProgressStrip
import com.sufficienteffort.jurassicjournal.ui.update.UpdateState
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
    @Inject lateinit var activeProfileRepository: ActiveProfileRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // savedInstanceState guard: these must not re-fire on every rotation /
        // theme change. Sequential on purpose — the image and icon syncs share
        // one SyncProgressTracker, and running them concurrently lets each
        // clobber the other's phase and reset the progress strip mid-sync.
        if (savedInstanceState == null) {
            lifecycleScope.launch(Dispatchers.IO) {
                activeProfileRepository.pruneOrphanedData()
                newDinoDetector.detect()
                dinoImageSync.syncMissingImages()
                abilityIconSync.syncMissingIcons()
            }
        }
        enableEdgeToEdge()
        setContent {
            JurassicJournalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        JurassicJournalNav()

                        val syncProgress by syncProgressTracker.progress.collectAsStateWithLifecycle()
                        UpdateProgressStrip(
                            progress = syncProgress,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }

                    val updateInfo by updateVm.updateInfo.collectAsStateWithLifecycle()
                    val updateState by updateVm.state.collectAsStateWithLifecycle()

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
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
    Process.killProcess(Process.myPid())
}

/** Ignores taps that land while a transition is still running (double-navigation guard). */
private fun NavController.navigateSafe(route: Screen) {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        navigate(route)
    }
}

private fun NavController.popBackStackSafe() {
    if (currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true) {
        popBackStack()
    }
}

@Composable
private fun JurassicJournalNav() {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.DinoList,
        enterTransition = { fadeIn(animationSpec = tween(150)) },
        exitTransition = { fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { fadeIn(animationSpec = tween(150)) },
        popExitTransition = { fadeOut(animationSpec = tween(150)) },
    ) {
        composable<Screen.DinoList> {
            DinoListScreen(
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId)) },
                onManageProfiles = { navController.navigateSafe(Screen.ManageProfiles) },
                onManageTeams = { navController.navigateSafe(Screen.ManageTeams) },
                onTeamClick = { teamId -> navController.navigateSafe(Screen.TeamDetail(teamId)) },
            )
        }
        composable<Screen.ManageProfiles> {
            ManageProfilesScreen(onBack = { navController.popBackStackSafe() })
        }
        composable<Screen.ManageTeams> {
            ManageTeamsScreen(
                onBack = { navController.popBackStackSafe() },
                onTeamClick = { teamId -> navController.navigateSafe(Screen.TeamDetail(teamId)) },
                onEditMembers = { teamId -> navController.navigateSafe(Screen.TeamDinoPicker(teamId)) },
            )
        }
        composable<Screen.TeamDetail> { entry ->
            val teamId = entry.toRoute<Screen.TeamDetail>().teamId
            TeamDetailScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId)) },
                onEditMembers = { navController.navigateSafe(Screen.TeamDinoPicker(teamId)) },
            )
        }
        composable<Screen.TeamDinoPicker> {
            TeamDinoPickerScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId, hideTeams = true)) },
            )
        }
        composable<Screen.DinoDetail> { entry ->
            val hideTeams = entry.toRoute<Screen.DinoDetail>().hideTeams
            DinoDetailScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId)) },
                onCalculate = { dinoId -> navController.navigateSafe(Screen.HybridCalculator(dinoId)) },
                onLevelUpCalculate = { dinoId, level -> navController.navigateSafe(Screen.LevelUpCalculator(dinoId, level)) },
                onSanctuaryCalculate = { dinoId -> navController.navigateSafe(Screen.SanctuaryCalculator(dinoId)) },
                onEnhancementEstimate = { dinoId, current -> navController.navigateSafe(Screen.EnhancementEstimator(dinoId, current)) },
                showTeamSelector = !hideTeams,
            )
        }
        composable<Screen.HybridCalculator> {
            HybridCalculatorScreen(
                onBack = { navController.popBackStackSafe() },
                onDinoClick = { dinoId -> navController.navigateSafe(Screen.DinoDetail(dinoId)) },
            )
        }
        composable<Screen.SanctuaryCalculator> {
            SanctuaryCalculatorScreen(onBack = { navController.popBackStackSafe() })
        }
        composable<Screen.EnhancementEstimator> {
            EnhancementEstimatorScreen(onBack = { navController.popBackStackSafe() })
        }
        composable<Screen.LevelUpCalculator> {
            LevelUpCalculatorScreen(onBack = { navController.popBackStackSafe() })
        }
    }
}
