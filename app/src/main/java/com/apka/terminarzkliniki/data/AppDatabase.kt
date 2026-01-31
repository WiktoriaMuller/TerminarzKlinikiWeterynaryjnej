package com.apka.terminarzkliniki.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Baza Room (lokalna baza SQLite).
@Database(
    entities = [Visit::class],
    version = 4,
    exportSchema = false // usuwa ostrzeżenie o schemaLocation
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun visitDao(): VisitDao
}

// Prosty singleton DB (żeby nie tworzyć jej wiele razy).
object DbProvider {
    @Volatile private var db: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        db ?: synchronized(this) {
            db ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "terminarz_kliniki.db"
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { db = it }
        }
}
