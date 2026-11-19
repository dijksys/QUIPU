package com.example.quipu.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.example.quipu.data.local.dao.NoteDao
import com.example.quipu.data.local.dao.ReminderDao
import com.example.quipu.data.local.entity.NoteEntity
import com.example.quipu.data.local.entity.ReminderEntity

@Database(
    entities = [NoteEntity::class, ReminderEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val DATABASE_NAME = "quipu.db"

        /**
         * Migraciones incrementales. Cada cambio de esquema debe añadir aquí una
         * migración explícita; la app nunca usa fallbackToDestructiveMigration().
         */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
