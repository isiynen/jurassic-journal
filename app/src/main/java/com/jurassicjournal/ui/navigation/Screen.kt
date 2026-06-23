package com.jurassicjournal.ui.navigation

sealed class Screen(val route: String) {
    data object DinoList : Screen("dino_list")
    data class DinoDetail(val dinoId: Long, val hideTeams: Boolean = false) : Screen(
        buildString {
            append("dino_detail/$dinoId")
            if (hideTeams) append("?hideTeams=true")
        }
    ) {
        companion object {
            const val ROUTE = "dino_detail/{dinoId}?hideTeams={hideTeams}"
        }
    }
    data class HybridCalculator(val dinoId: Long) : Screen("hybrid_calculator/$dinoId") {
        companion object {
            const val ROUTE = "hybrid_calculator/{dinoId}"
        }
    }
    data class SanctuaryCalculator(val dinoId: Long) : Screen("sanctuary_calculator/$dinoId") {
        companion object {
            const val ROUTE = "sanctuary_calculator/{dinoId}"
        }
    }
    data object ManageProfiles : Screen("manage_profiles")
    data object ManageTeams : Screen("manage_teams")
    data class TeamDetail(val teamId: Long) : Screen("team_detail/$teamId") {
        companion object {
            const val ROUTE = "team_detail/{teamId}"
        }
    }
    data class TeamDinoPicker(val teamId: Long) : Screen("team_dino_picker/$teamId") {
        companion object {
            const val ROUTE = "team_dino_picker/{teamId}"
        }
    }
}
