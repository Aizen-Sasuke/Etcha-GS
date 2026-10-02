package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [HabitEntry::class, PreferenceEntry::class, Tracker::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun trackerDao(): TrackerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private fun addColumnIfNotExists(db: SupportSQLiteDatabase, table: String, column: String, definition: String) {
            val cursor = db.query("PRAGMA table_info(`$table`)")
            var exists = false
            while (cursor.moveToNext()) {
                val nameIdx = cursor.getColumnIndex("name")
                if (nameIdx != -1 && cursor.getString(nameIdx).equals(column, ignoreCase = true)) {
                    exists = true
                    break
                }
            }
            cursor.close()
            if (!exists) {
                db.execSQL("ALTER TABLE `$table` ADD COLUMN `$column` $definition")
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `preference_entries` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create trackers table
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `trackers` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `icon` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
                // Insert a default tracker if there are entries
                db.execSQL("INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder) VALUES ('sobriety', 'Sobriety', '💪', 0)")

                // Check if existing habit_entries already had trackerId column
                val cursor = db.query("PRAGMA table_info(`habit_entries`)")
                var hasTrackerId = false
                while (cursor.moveToNext()) {
                    val nameIdx = cursor.getColumnIndex("name")
                    if (nameIdx != -1 && cursor.getString(nameIdx).equals("trackerId", ignoreCase = true)) {
                        hasTrackerId = true
                        break
                    }
                }
                cursor.close()

                val selectSql = if (hasTrackerId) {
                    "SELECT `id`, `dateString`, `count`, `timestamp`, `notes`, `trackerId` FROM `habit_entries`"
                } else {
                    "SELECT `id`, `dateString`, `count`, `timestamp`, `notes`, 'sobriety' FROM `habit_entries`"
                }

                // Recreate habit_entries with foreign key
                db.execSQL("CREATE TABLE IF NOT EXISTS `habit_entries_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateString` TEXT NOT NULL, `count` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, `notes` TEXT, `trackerId` TEXT NOT NULL, FOREIGN KEY(`trackerId`) REFERENCES `trackers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
                db.execSQL("INSERT INTO `habit_entries_new` (`id`, `dateString`, `count`, `timestamp`, `notes`, `trackerId`) $selectSql")
                db.execSQL("DROP TABLE `habit_entries`")
                db.execSQL("ALTER TABLE `habit_entries_new` RENAME TO `habit_entries`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_entries_trackerId` ON `habit_entries` (`trackerId`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "trackers", "accentColor", "TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "trackers", "type", "TEXT NOT NULL DEFAULT 'good'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfNotExists(db, "trackers", "frequencyType", "TEXT NOT NULL DEFAULT 'daily'")
                addColumnIfNotExists(db, "trackers", "targetDays", "TEXT NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI,SAT,SUN'")
                addColumnIfNotExists(db, "trackers", "targetCount", "INTEGER NOT NULL DEFAULT 1")
                addColumnIfNotExists(db, "trackers", "timeOfDay", "TEXT NOT NULL DEFAULT 'anytime'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit_tracker_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigrationOnDowngrade(true)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        db.execSQL("INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder, accentColor, type, frequencyType, targetDays, targetCount, timeOfDay) VALUES ('sobriety', 'Sobriety', '🚭', 0, NULL, 'good', 'daily', 'MON,TUE,WED,THU,FRI,SAT,SUN', 1, 'anytime')")
                        db.execSQL("INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder, accentColor, type, frequencyType, targetDays, targetCount, timeOfDay) VALUES ('gym', 'Gym & Workout', '🏋️‍♂️', 1, '#FF5722', 'good', 'weekly', 'MON,TUE,THU,FRI,SAT', 1, 'morning')")
                        db.execSQL("INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder, accentColor, type, frequencyType, targetDays, targetCount, timeOfDay) VALUES ('gardening', 'Gardening', '🌿', 2, '#4CAF50', 'good', 'daily', 'MON,TUE,WED,THU,FRI,SAT,SUN', 1, 'afternoon')")
                        db.execSQL("INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder, accentColor, type, frequencyType, targetDays, targetCount, timeOfDay) VALUES ('reading', 'Daily Reading', '📚', 3, '#2196F3', 'good', 'daily', 'MON,TUE,WED,THU,FRI,SAT,SUN', 1, 'evening')")
                        db.execSQL("INSERT OR IGNORE INTO trackers (id, title, icon, sortOrder, accentColor, type, frequencyType, targetDays, targetCount, timeOfDay) VALUES ('water', 'Hydration', '💧', 4, '#00BCD4', 'good', 'daily', 'MON,TUE,WED,THU,FRI,SAT,SUN', 8, 'anytime')")
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
