package com.sufficienteffort.jurassicjournal.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes. Arguments are the constructor properties; a
 * ViewModel reads them with `savedStateHandle.toRoute<Screen.X>()`.
 */
sealed interface Screen {
    @Serializable data object DinoList : Screen
    @Serializable data class DinoDetail(val dinoId: Long, val hideTeams: Boolean = false) : Screen
    @Serializable data class HybridCalculator(val dinoId: Long) : Screen
    @Serializable data class SanctuaryCalculator(val dinoId: Long) : Screen
    @Serializable data object ManageProfiles : Screen
    @Serializable data object ManageTeams : Screen
    @Serializable data class TeamDetail(val teamId: Long) : Screen
    @Serializable data class TeamDinoPicker(val teamId: Long) : Screen
    @Serializable data class EnhancementEstimator(val dinoId: Long, val currentEnhancement: Int) : Screen
    /** [currentLevel] < 0 means "use the saved level". */
    @Serializable data class LevelUpCalculator(val dinoId: Long, val currentLevel: Int = -1) : Screen
}
