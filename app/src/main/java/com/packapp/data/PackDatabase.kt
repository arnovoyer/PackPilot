package com.packapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PackingListEntity::class,
        PackingItemEntity::class,
        ActivityEventEntity::class,
        PackingSessionEntity::class,
        TripEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class PackDatabase : RoomDatabase() {
    abstract fun packDao(): PackDao

    companion object {
        @Volatile
        private var INSTANCE: PackDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS activity_events (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        eventType TEXT NOT NULL,
                        listId INTEGER,
                        itemId INTEGER,
                        listName TEXT,
                        itemTitle TEXT,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add packing_sessions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS packing_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        listName TEXT NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        itemsPacked INTEGER NOT NULL,
                        totalItems INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Add new columns to packing_items
                db.execSQL("ALTER TABLE packing_items ADD COLUMN weightGrams INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE packing_items ADD COLUMN wasUsed INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add trips table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS trips (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        location TEXT NOT NULL,
                        startDate INTEGER NOT NULL,
                        endDate INTEGER,
                        completedDate INTEGER,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                // Add tripId to packing_lists
                db.execSQL("ALTER TABLE packing_lists ADD COLUMN tripId INTEGER")
            }
        }

        fun getInstance(context: Context): PackDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PackDatabase::class.java,
                    "pack_app.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
