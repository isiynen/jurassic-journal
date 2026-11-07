package com.sufficienteffort.jurassicjournal.data.user

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Migrates a hand-built v1 (pre-profiles) database through every migration and lets
 * Room validate the result against the current entity schema on open.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UserDatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    private fun createV1() {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName), null)
        db.execSQL("CREATE TABLE user_wallet (coins INTEGER NOT NULL, hardCash INTEGER NOT NULL, lastUpdated INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE user_dna_inventory (dinoId INTEGER NOT NULL, dnaAmount INTEGER NOT NULL, lastUpdated INTEGER NOT NULL, PRIMARY KEY(dinoId))")
        db.execSQL("CREATE TABLE user_catalysts (catalystType TEXT NOT NULL, quantityOwned INTEGER NOT NULL, PRIMARY KEY(catalystType))")
        db.execSQL("CREATE TABLE user_dinos (dinoId INTEGER NOT NULL, isUnlocked INTEGER NOT NULL, currentLevel INTEGER NOT NULL, currentXp INTEGER NOT NULL, PRIMARY KEY(dinoId))")
        db.execSQL("CREATE TABLE user_dino_enhancements (dinoId INTEGER NOT NULL, enhancementId INTEGER NOT NULL, isUnlocked INTEGER NOT NULL, unlockedDate INTEGER, PRIMARY KEY(dinoId, enhancementId))")
        db.execSQL("CREATE TABLE user_boosts (dinoId INTEGER NOT NULL, stat TEXT NOT NULL, boostsApplied INTEGER NOT NULL, PRIMARY KEY(dinoId, stat))")
        db.execSQL("CREATE TABLE omega_training_allocations (dinoId INTEGER NOT NULL, stat TEXT NOT NULL, pointsAllocated INTEGER NOT NULL, PRIMARY KEY(dinoId, stat))")
        db.execSQL("CREATE TABLE user_preferences (`key` TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY(`key`))")

        db.execSQL("INSERT INTO user_wallet VALUES (1500, 42, 99)")
        db.execSQL("INSERT INTO user_dna_inventory VALUES (7, 300, 11)")
        db.execSQL("INSERT INTO user_catalysts VALUES ('RARE', 5)")
        db.execSQL("INSERT INTO user_dinos VALUES (7, 1, 25, 1200)")
        db.execSQL("INSERT INTO user_dino_enhancements VALUES (7, 3, 1, 555)")
        db.execSQL("INSERT INTO user_boosts VALUES (7, 'HEALTH', 4)")
        db.execSQL("INSERT INTO omega_training_allocations VALUES (7, 'ATTACK', 6)")
        db.execSQL("INSERT INTO user_preferences VALUES ('theme', 'dark')")
        db.version = 1
        db.close()
    }

    private fun openMigrated(): UserDatabase =
        Room.databaseBuilder(context, UserDatabase::class.java, dbName)
            .addMigrations(*UserDatabaseMigrations.ALL)
            .allowMainThreadQueries()
            .build()

    private fun UserDatabase.long(sql: String): Long =
        openHelper.readableDatabase.query(sql).use { it.moveToFirst(); it.getLong(0) }

    @Test
    fun migrateV1ToCurrent_passesRoomSchemaValidationAndKeepsData() {
        createV1()
        val db = openMigrated()
        try {
            // Opening triggers migrations + Room's schema validation.
            db.openHelper.writableDatabase

            assertEquals(1L, db.long("SELECT COUNT(*) FROM profiles"))
            assertEquals(1L, db.long("SELECT id FROM profiles"))
            assertEquals(1500L, db.long("SELECT coins FROM user_wallet WHERE profileId = 1"))
            assertEquals(42L, db.long("SELECT hardCash FROM user_wallet WHERE profileId = 1"))
            assertEquals(300L, db.long("SELECT dnaAmount FROM user_dna_inventory WHERE profileId = 1 AND dinoId = 7"))
            assertEquals(5L, db.long("SELECT quantityOwned FROM user_catalysts WHERE profileId = 1 AND catalystType = 'RARE'"))
            assertEquals(25L, db.long("SELECT currentLevel FROM user_dinos WHERE profileId = 1 AND dinoId = 7"))
            assertEquals(555L, db.long("SELECT unlockedDate FROM user_dino_enhancements WHERE profileId = 1 AND dinoId = 7 AND enhancementId = 3"))
            assertEquals(4L, db.long("SELECT boostsApplied FROM user_boosts WHERE profileId = 1 AND dinoId = 7 AND stat = 'HEALTH'"))
            assertEquals(6L, db.long("SELECT pointsAllocated FROM omega_training_allocations WHERE profileId = 1 AND dinoId = 7 AND stat = 'ATTACK'"))
            assertEquals(1L, db.long("SELECT COUNT(*) FROM user_preferences WHERE `key` = 'theme' AND value = 'dark'"))
        } finally {
            db.close()
        }
    }

    @Test
    fun migratedDatabase_cascadesProfileDeleteToTeamsAndNewDinos() {
        createV1()
        val db = openMigrated()
        try {
            val sql = db.openHelper.writableDatabase
            sql.execSQL("INSERT INTO teams (id, profileId, name, createdAt, sortOrder) VALUES (1, 1, 'A', 0, 0)")
            sql.execSQL("INSERT INTO team_members (teamId, dinoId, slotOrder) VALUES (1, 7, 0)")
            sql.execSQL("INSERT INTO new_dinos (profileId, dinoSlug) VALUES (1, 'rex')")

            sql.execSQL("DELETE FROM profiles WHERE id = 1")

            assertEquals(0L, db.long("SELECT COUNT(*) FROM teams"))
            assertEquals(0L, db.long("SELECT COUNT(*) FROM team_members"))
            assertEquals(0L, db.long("SELECT COUNT(*) FROM new_dinos"))
        } finally {
            db.close()
        }
    }
}
