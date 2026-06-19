package com.jurassicjournal.data.user

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object UserDatabaseMigrations {

    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // ── profiles ───────────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS profiles (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    sortOrder INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
            db.execSQL(
                "INSERT INTO profiles (id, name, createdAt, sortOrder) VALUES (1, 'Default', ${System.currentTimeMillis()}, 0)"
            )

            // ── user_wallet ────────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE user_wallet_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    coins INTEGER NOT NULL DEFAULT 0,
                    hardCash INTEGER NOT NULL DEFAULT 0,
                    lastUpdated INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(profileId)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO user_wallet_new (profileId, coins, hardCash, lastUpdated) SELECT 1, coins, hardCash, lastUpdated FROM user_wallet")
            db.execSQL("DROP TABLE user_wallet")
            db.execSQL("ALTER TABLE user_wallet_new RENAME TO user_wallet")

            // ── user_dna_inventory ─────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE user_dna_inventory_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    dinoId INTEGER NOT NULL,
                    dnaAmount INTEGER NOT NULL DEFAULT 0,
                    lastUpdated INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(profileId, dinoId)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO user_dna_inventory_new SELECT 1, dinoId, dnaAmount, lastUpdated FROM user_dna_inventory")
            db.execSQL("DROP TABLE user_dna_inventory")
            db.execSQL("ALTER TABLE user_dna_inventory_new RENAME TO user_dna_inventory")

            // ── user_catalysts ─────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE user_catalysts_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    catalystType TEXT NOT NULL,
                    quantityOwned INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(profileId, catalystType)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO user_catalysts_new SELECT 1, catalystType, quantityOwned FROM user_catalysts")
            db.execSQL("DROP TABLE user_catalysts")
            db.execSQL("ALTER TABLE user_catalysts_new RENAME TO user_catalysts")

            // ── user_dinos ─────────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE user_dinos_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    dinoId INTEGER NOT NULL,
                    isUnlocked INTEGER NOT NULL DEFAULT 0,
                    currentLevel INTEGER NOT NULL DEFAULT 1,
                    currentXp INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(profileId, dinoId)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO user_dinos_new SELECT 1, dinoId, isUnlocked, currentLevel, currentXp FROM user_dinos")
            db.execSQL("DROP TABLE user_dinos")
            db.execSQL("ALTER TABLE user_dinos_new RENAME TO user_dinos")

            // ── user_dino_enhancements ─────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE user_dino_enhancements_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    dinoId INTEGER NOT NULL,
                    enhancementId INTEGER NOT NULL,
                    isUnlocked INTEGER NOT NULL DEFAULT 0,
                    unlockedDate INTEGER,
                    PRIMARY KEY(profileId, dinoId, enhancementId)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO user_dino_enhancements_new SELECT 1, dinoId, enhancementId, isUnlocked, unlockedDate FROM user_dino_enhancements")
            db.execSQL("DROP TABLE user_dino_enhancements")
            db.execSQL("ALTER TABLE user_dino_enhancements_new RENAME TO user_dino_enhancements")

            // ── user_boosts ────────────────────────────────────────────────────
            db.execSQL("""
                CREATE TABLE user_boosts_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    dinoId INTEGER NOT NULL,
                    stat TEXT NOT NULL,
                    boostsApplied INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(profileId, dinoId, stat)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO user_boosts_new SELECT 1, dinoId, stat, boostsApplied FROM user_boosts")
            db.execSQL("DROP TABLE user_boosts")
            db.execSQL("ALTER TABLE user_boosts_new RENAME TO user_boosts")

            // ── omega_training_allocations ─────────────────────────────────────
            db.execSQL("""
                CREATE TABLE omega_training_allocations_new (
                    profileId INTEGER NOT NULL DEFAULT 1,
                    dinoId INTEGER NOT NULL,
                    stat TEXT NOT NULL,
                    pointsAllocated INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(profileId, dinoId, stat)
                )
            """.trimIndent())
            db.execSQL("INSERT INTO omega_training_allocations_new SELECT 1, dinoId, stat, pointsAllocated FROM omega_training_allocations")
            db.execSQL("DROP TABLE omega_training_allocations")
            db.execSQL("ALTER TABLE omega_training_allocations_new RENAME TO omega_training_allocations")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS teams (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    profileId INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL DEFAULT 0,
                    sortOrder INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(profileId) REFERENCES profiles(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_teams_profileId ON teams(profileId)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS team_members (
                    teamId INTEGER NOT NULL,
                    dinoId INTEGER NOT NULL,
                    slotOrder INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(teamId, dinoId),
                    FOREIGN KEY(teamId) REFERENCES teams(id) ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS index_team_members_teamId ON team_members(teamId)")
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
