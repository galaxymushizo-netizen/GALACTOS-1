package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [HealthRecord::class, UserGoal::class, UserProfile::class], version = 1, exportSchema = false)
abstract class HealthDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao

    companion object {
        @Volatile
        private var INSTANCE: HealthDatabase? = null

        fun getDatabase(context: Context): HealthDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HealthDatabase::class.java,
                    "health_tracker_database"
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default goals cleanly via raw SQL on the underlying DB to avoid re-entrance deadlocks
                        db.execSQL("INSERT OR REPLACE INTO user_goals (type, targetValue) VALUES ('STEPS', 10000.0)")
                        db.execSQL("INSERT OR REPLACE INTO user_goals (type, targetValue) VALUES ('WATER', 2500.0)")
                        db.execSQL("INSERT OR REPLACE INTO user_goals (type, targetValue) VALUES ('SLEEP', 480.0)")
                        db.execSQL("INSERT OR REPLACE INTO user_goals (type, targetValue) VALUES ('CALORIES', 2200.0)")
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
