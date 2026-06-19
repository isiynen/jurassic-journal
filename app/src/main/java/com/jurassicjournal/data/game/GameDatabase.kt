package com.jurassicjournal.data.game

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jurassicjournal.data.game.dao.DinoBaseStatDao
import com.jurassicjournal.data.game.dao.DinoDao
import com.jurassicjournal.data.game.dao.DinoHybridIngredientDao
import com.jurassicjournal.data.game.dao.DinoMoveDao
import com.jurassicjournal.data.game.dao.DinoResistanceDao
import com.jurassicjournal.data.game.dao.DinoSanctuaryPointDao
import com.jurassicjournal.data.game.dao.DinoSpawnLocationDao
import com.jurassicjournal.data.game.dao.LevelUpCostDao
import com.jurassicjournal.data.game.dao.MoveDao
import com.jurassicjournal.data.game.dao.OmegaTrainingConfigDao
import com.jurassicjournal.data.game.entity.BoostRule
import com.jurassicjournal.data.game.entity.Catalyst
import com.jurassicjournal.data.game.entity.CatalystRecipe
import com.jurassicjournal.data.game.entity.Dino
import com.jurassicjournal.data.game.entity.DinoBaseStat
import com.jurassicjournal.data.game.entity.DinoHybridIngredient
import com.jurassicjournal.data.game.entity.DinoMove
import com.jurassicjournal.data.game.entity.DinoResistance
import com.jurassicjournal.data.game.entity.DinoSanctuaryPoint
import com.jurassicjournal.data.game.entity.DinoSpawnLocation
import com.jurassicjournal.data.game.entity.Enhancement
import com.jurassicjournal.data.game.entity.EnhancementCatalystCost
import com.jurassicjournal.data.game.entity.EnhancementMoveUnlock
import com.jurassicjournal.data.game.entity.EnhancementStatBonus
import com.jurassicjournal.data.game.entity.HybridLevelCost
import com.jurassicjournal.data.game.entity.LevelUpCost
import com.jurassicjournal.data.game.entity.Move
import com.jurassicjournal.data.game.entity.OmegaTrainingConfig
import com.jurassicjournal.data.game.entity.SanctuaryYield

@Database(
    entities = [
        Dino::class,
        DinoBaseStat::class,
        DinoResistance::class,
        DinoSpawnLocation::class,
        DinoHybridIngredient::class,
        OmegaTrainingConfig::class,
        DinoSanctuaryPoint::class,
        SanctuaryYield::class,
        Move::class,
        DinoMove::class,
        LevelUpCost::class,
        HybridLevelCost::class,
        BoostRule::class,
        Enhancement::class,
        EnhancementStatBonus::class,
        EnhancementMoveUnlock::class,
        Catalyst::class,
        CatalystRecipe::class,
        EnhancementCatalystCost::class,
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(GameConverters::class)
abstract class GameDatabase : RoomDatabase() {
    abstract fun dinoDao(): DinoDao
    abstract fun dinoBaseStatDao(): DinoBaseStatDao
    abstract fun dinoResistanceDao(): DinoResistanceDao
    abstract fun dinoSpawnLocationDao(): DinoSpawnLocationDao
    abstract fun moveDao(): MoveDao
    abstract fun dinoMoveDao(): DinoMoveDao
    abstract fun dinoHybridIngredientDao(): DinoHybridIngredientDao
    abstract fun omegaTrainingConfigDao(): OmegaTrainingConfigDao
    abstract fun dinoSanctuaryPointDao(): DinoSanctuaryPointDao
    abstract fun levelUpCostDao(): LevelUpCostDao
}
