package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [StudentEntity::class, AttendanceEntity::class],
    version = 4,
    exportSchema = false
)
abstract class MealDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao

    companion object {
        @Volatile
        private var INSTANCE: MealDatabase? = null

        private fun recreateTables(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `attendance`")
            db.execSQL("DROP TABLE IF EXISTS `students`")
            db.execSQL("CREATE TABLE IF NOT EXISTS `students` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `rollNo` TEXT NOT NULL, `name` TEXT NOT NULL, `roomNo` TEXT NOT NULL, `phone` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `attendance` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `studentId` INTEGER NOT NULL, `date` TEXT NOT NULL, `mealType` TEXT NOT NULL, `isPresent` INTEGER NOT NULL, `recordedAt` INTEGER NOT NULL, FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_attendance_studentId_date_mealType` ON `attendance` (`studentId`, `date`, `mealType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_date_mealType` ON `attendance` (`date`, `mealType`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_attendance_date` ON `attendance` (`date`)")
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateTables(db)
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateTables(db)
            }
        }

        private val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateTables(db)
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                recreateTables(db)
            }
        }

        fun getDatabase(context: Context): MealDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MealDatabase::class.java,
                    "meal_attendance_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_1_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
