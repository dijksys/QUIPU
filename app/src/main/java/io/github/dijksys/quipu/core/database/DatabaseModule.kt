package io.github.dijksys.quipu.core.database

import android.content.Context
import androidx.room.Room
import io.github.dijksys.quipu.data.local.dao.NoteDao
import io.github.dijksys.quipu.data.local.dao.NoteFtsDao
import io.github.dijksys.quipu.data.local.dao.NoteTagDao
import io.github.dijksys.quipu.data.local.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        AppDatabase.DATABASE_NAME,
    )
        .addMigrations(*AppDatabase.MIGRATIONS)
        .build()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideReminderDao(database: AppDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideNoteFtsDao(database: AppDatabase): NoteFtsDao = database.noteFtsDao()

    @Provides
    fun provideNoteTagDao(database: AppDatabase): NoteTagDao = database.noteTagDao()
}
