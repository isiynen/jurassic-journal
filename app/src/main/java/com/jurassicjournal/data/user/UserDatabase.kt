package com.jurassicjournal.data.user

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jurassicjournal.data.user.dao.OmegaTrainingAllocationDao
import com.jurassicjournal.data.user.dao.ProfileDao
import com.jurassicjournal.data.user.dao.TeamDao
import com.jurassicjournal.data.user.dao.TeamMemberDao
import com.jurassicjournal.data.user.dao.UserBoostDao
import com.jurassicjournal.data.user.dao.UserDinoDao
import com.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.jurassicjournal.data.user.dao.UserWalletDao
import com.jurassicjournal.data.user.entity.OmegaTrainingAllocation
import com.jurassicjournal.data.user.entity.Profile
import com.jurassicjournal.data.user.entity.Team
import com.jurassicjournal.data.user.entity.TeamMember
import com.jurassicjournal.data.user.entity.UserBoost
import com.jurassicjournal.data.user.entity.UserCatalyst
import com.jurassicjournal.data.user.entity.UserDino
import com.jurassicjournal.data.user.entity.UserDinoEnhancement
import com.jurassicjournal.data.user.entity.UserDnaInventory
import com.jurassicjournal.data.user.entity.UserPreference
import com.jurassicjournal.data.user.entity.UserWallet

@Database(
    entities = [
        Profile::class,
        UserWallet::class,
        UserDnaInventory::class,
        UserCatalyst::class,
        UserDino::class,
        UserDinoEnhancement::class,
        UserBoost::class,
        OmegaTrainingAllocation::class,
        UserPreference::class,
        Team::class,
        TeamMember::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(UserConverters::class)
abstract class UserDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun userWalletDao(): UserWalletDao
    abstract fun userDinoDao(): UserDinoDao
    abstract fun userBoostDao(): UserBoostDao
    abstract fun omegaTrainingAllocationDao(): OmegaTrainingAllocationDao
    abstract fun userDnaInventoryDao(): UserDnaInventoryDao
    abstract fun teamDao(): TeamDao
    abstract fun teamMemberDao(): TeamMemberDao
}
