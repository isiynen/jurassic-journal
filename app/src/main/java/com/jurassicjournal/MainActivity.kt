package com.jurassicjournal

import android.content.Context
import android.os.Bundle
import android.os.Process
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import com.jurassicjournal.data.update.UpdateInfo
import com.jurassicjournal.ui.calculator.HybridCalculatorScreen
import com.jurassicjournal.ui.dino.DinoDetailScreen
import com.jurassicjournal.ui.dino.DinoListScreen
import com.jurassicjournal.ui.navigation.Screen
import com.jurassicjournal.ui.profile.ManageProfilesScreen
import com.jurassicjournal.ui.sanctuary.SanctuaryCalculatorScreen
import com.jurassicjournal.ui.team.ManageTeamsScreen
import com.jurassicjournal.ui.team.TeamDetailScreen
import com.jurassicjournal.ui.theme.JurassicJournalTheme
import com.jurassicjournal.ui.update.UpdateCheckViewModel
import com.jurassicjournal.ui.update.UpdateState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val updateVm: UpdateCheckViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JurassicJournalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    JurassicJournalNav()

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

        updateState == UpdateState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("Updating Database") },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(16.dp))
                        Text("Downloading new data…")
                    }
                },
                confirmButton = {},
            )
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

@Composable
private fun JurassicJournalNav() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.DinoList.route) {
        composable(Screen.DinoList.route) {
            DinoListScreen(
                onDinoClick = { dinoId -> navController.navigate(Screen.DinoDetail(dinoId).route) },
                onManageProfiles = { navController.navigate(Screen.ManageProfiles.route) },
                onManageTeams = { navController.navigate(Screen.ManageTeams.route) },
                onTeamClick = { teamId -> navController.navigate(Screen.TeamDetail(teamId).route) },
            )
        }
        composable(Screen.ManageProfiles.route) {
            ManageProfilesScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ManageTeams.route) {
            ManageTeamsScreen(
                onBack = { navController.popBackStack() },
                onTeamClick = { teamId -> navController.navigate(Screen.TeamDetail(teamId).route) },
            )
        }
        composable(
            route = Screen.TeamDetail.ROUTE,
            arguments = listOf(navArgument("teamId") { type = NavType.LongType })
        ) {
            TeamDetailScreen(
                onBack = { navController.popBackStack() },
                onDinoClick = { dinoId -> navController.navigate(Screen.DinoDetail(dinoId).route) },
            )
        }
        composable(
            route = Screen.DinoDetail.ROUTE,
            arguments = listOf(navArgument("dinoId") { type = NavType.LongType })
        ) {
            DinoDetailScreen(
                onBack = { navController.popBackStack() },
                onDinoClick = { dinoId -> navController.navigate(Screen.DinoDetail(dinoId).route) },
                onCalculate = { dinoId -> navController.navigate(Screen.HybridCalculator(dinoId).route) },
                onSanctuaryCalculate = { dinoId -> navController.navigate(Screen.SanctuaryCalculator(dinoId).route) },
            )
        }
        composable(
            route = Screen.HybridCalculator.ROUTE,
            arguments = listOf(navArgument("dinoId") { type = NavType.LongType })
        ) {
            HybridCalculatorScreen(
                onBack = { navController.popBackStack() },
                onDinoClick = { dinoId -> navController.navigate(Screen.DinoDetail(dinoId).route) },
            )
        }
        composable(
            route = Screen.SanctuaryCalculator.ROUTE,
            arguments = listOf(navArgument("dinoId") { type = NavType.LongType })
        ) {
            SanctuaryCalculatorScreen(onBack = { navController.popBackStack() })
        }
    }
}
