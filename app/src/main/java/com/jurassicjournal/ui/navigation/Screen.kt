package com.jurassicjournal.ui.navigation

sealed class Screen(val route: String) {
    data object DinoList : Screen("dino_list")
    data class DinoDetail(val dinoId: Long) : Screen("dino_detail/$dinoId") {
        companion object {
            const val ROUTE = "dino_detail/{dinoId}"
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
}
