package com.jurassicjournal.di

import android.content.Context
import androidx.room.Room
import com.jurassicjournal.data.game.GameDatabase
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
import com.jurassicjournal.data.user.UserDatabase
import com.jurassicjournal.data.user.UserDatabaseMigrations
import com.jurassicjournal.data.user.dao.NewDinoDao
import com.jurassicjournal.data.user.dao.OmegaTrainingAllocationDao
import com.jurassicjournal.data.user.dao.ProfileDao
import com.jurassicjournal.data.user.dao.TeamDao
import com.jurassicjournal.data.user.dao.TeamMemberDao
import com.jurassicjournal.data.user.dao.UserBoostDao
import com.jurassicjournal.data.user.dao.UserDinoDao
import com.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.jurassicjournal.data.user.dao.UserWalletDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideGameDatabase(@ApplicationContext context: Context): GameDatabase =
        Room.databaseBuilder(context, GameDatabase::class.java, "game_database")
            .createFromAsset("game_database.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase =
        Room.databaseBuilder(context, UserDatabase::class.java, "user_database")
            .addMigrations(*UserDatabaseMigrations.ALL)
            .build()

    @Provides fun provideDinoDao(db: GameDatabase): DinoDao = db.dinoDao()
    @Provides fun provideDinoBaseStatDao(db: GameDatabase): DinoBaseStatDao = db.dinoBaseStatDao()
    @Provides fun provideDinoResistanceDao(db: GameDatabase): DinoResistanceDao = db.dinoResistanceDao()
    @Provides fun provideDinoSpawnLocationDao(db: GameDatabase): DinoSpawnLocationDao = db.dinoSpawnLocationDao()
    @Provides fun provideMoveDao(db: GameDatabase): MoveDao = db.moveDao()
    @Provides fun provideDinoMoveDao(db: GameDatabase): DinoMoveDao = db.dinoMoveDao()
    @Provides fun provideDinoHybridIngredientDao(db: GameDatabase): DinoHybridIngredientDao = db.dinoHybridIngredientDao()
    @Provides fun provideOmegaTrainingConfigDao(db: GameDatabase): OmegaTrainingConfigDao = db.omegaTrainingConfigDao()
    @Provides fun provideDinoSanctuaryPointDao(db: GameDatabase): DinoSanctuaryPointDao = db.dinoSanctuaryPointDao()
    @Provides fun provideLevelUpCostDao(db: GameDatabase): LevelUpCostDao = db.levelUpCostDao()

    @Provides fun provideProfileDao(db: UserDatabase): ProfileDao = db.profileDao()
    @Provides fun provideUserWalletDao(db: UserDatabase): UserWalletDao = db.userWalletDao()
    @Provides fun provideUserDinoDao(db: UserDatabase): UserDinoDao = db.userDinoDao()
    @Provides fun provideUserBoostDao(db: UserDatabase): UserBoostDao = db.userBoostDao()
    @Provides fun provideOmegaTrainingAllocationDao(db: UserDatabase): OmegaTrainingAllocationDao = db.omegaTrainingAllocationDao()
    @Provides fun provideUserDnaInventoryDao(db: UserDatabase): UserDnaInventoryDao = db.userDnaInventoryDao()
    @Provides fun provideTeamDao(db: UserDatabase): TeamDao = db.teamDao()
    @Provides fun provideTeamMemberDao(db: UserDatabase): TeamMemberDao = db.teamMemberDao()
    @Provides fun provideNewDinoDao(db: UserDatabase): NewDinoDao = db.newDinoDao()
}
