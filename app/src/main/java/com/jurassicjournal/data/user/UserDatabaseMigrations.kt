package com.jurassicjournal.data.user

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * All UserDatabase schema migrations, in version order.
 *
 * When bumping UserDatabase.version:
 *  1. Add a Migration(oldVersion, newVersion) object below.
 *  2. Add it to the ALL array.
 *  3. Do NOT use fallbackToDestructiveMigration — that silently wipes user data.
 *
 * Example for adding a column in a future version 2:
 *
 *   val MIGRATION_1_2 = object : Migration(1, 2) {
 *       override fun migrate(db: SupportSQLiteDatabase) {
 *           db.execSQL("ALTER TABLE user_dino ADD COLUMN notes TEXT")
 *       }
 *   }
 *
 * Then add MIGRATION_1_2 to ALL below and bump version to 2 in UserDatabase.
 */
object UserDatabaseMigrations {
    val ALL: Array<Migration> = emptyArray()
}
