package com.sufficienteffort.jurassicjournal.data.user

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sufficienteffort.jurassicjournal.data.user.dao.OmegaTrainingAllocationDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoEnhancementDao
import com.sufficienteffort.jurassicjournal.data.user.dao.ProfileDao
import com.sufficienteffort.jurassicjournal.data.user.dao.TeamDao
import com.sufficienteffort.jurassicjournal.data.user.dao.NewDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.TeamMemberDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserBoostDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDinoDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserDnaInventoryDao
import com.sufficienteffort.jurassicjournal.data.user.dao.UserWalletDao
import com.sufficienteffort.jurassicjournal.data.user.entity.NewDino
import com.sufficienteffort.jurassicjournal.data.user.entity.OmegaTrainingAllocation
import com.sufficienteffort.jurassicjournal.data.user.entity.Profile
import com.sufficienteffort.jurassicjournal.data.user.entity.Team
import com.sufficienteffort.jurassicjournal.data.user.entity.TeamMember
import com.sufficienteffort.jurassicjournal.data.user.entity.UserBoost
import com.sufficienteffort.jurassicjournal.data.user.entity.UserCatalyst
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDino
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDinoEnhancement
import com.sufficienteffort.jurassicjournal.data.user.entity.UserDnaInventory
import com.sufficienteffort.jurassicjournal.data.user.entity.UserPreference
import com.sufficienteffort.jurassicjournal.data.user.entity.UserWallet

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
        NewDino::class,
    ],
    version = 4,
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
    abstract fun newDinoDao(): NewDinoDao
    abstract fun userDinoEnhancementDao(): UserDinoEnhancementDao
}
