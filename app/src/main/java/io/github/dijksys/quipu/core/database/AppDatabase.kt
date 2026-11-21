package io.github.dijksys.quipu.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import io.github.dijksys.quipu.data.local.dao.NoteDao
import io.github.dijksys.quipu.data.local.dao.NoteFtsDao
import io.github.dijksys.quipu.data.local.dao.NoteTagDao
import io.github.dijksys.quipu.data.local.dao.ReminderDao
import io.github.dijksys.quipu.data.local.entity.NoteEntity
import io.github.dijksys.quipu.data.local.entity.NoteFtsEntity
import io.github.dijksys.quipu.data.local.entity.NoteTagCrossRef
import io.github.dijksys.quipu.data.local.entity.NoteTagEntity
import io.github.dijksys.quipu.data.local.entity.ReminderEntity

@Database(
    entities = [
        NoteEntity::class,
        ReminderEntity::class,
        NoteFtsEntity::class,
        NoteTagEntity::class,
        NoteTagCrossRef::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun noteFtsDao(): NoteFtsDao
    abstract fun noteTagDao(): NoteTagDao

    companion object {
        const val DATABASE_NAME = "quipu.db"

        /**
         * Migraciones incrementales. Cada cambio de esquema debe a��adir aqu�� una
         * migraci��n expl��cita; la app nunca usa fallbackToDestructiveMigration().
         */
        val MIGRATIONS: Array<Migration> = arrayOf(Migration1To2)
    }
}
