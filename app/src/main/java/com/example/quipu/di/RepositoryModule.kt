package com.example.quipu.di

import com.example.quipu.core.alarms.AndroidReminderAlarmScheduler
import com.example.quipu.core.alarms.ReminderAlarmScheduler
import com.example.quipu.core.notifications.ReminderNotificationManager
import com.example.quipu.core.notifications.ReminderNotifier
import com.example.quipu.data.repository.NoteRepositoryImpl
import com.example.quipu.data.repository.ReminderRepositoryImpl
import com.example.quipu.data.repository.SettingsRepositoryImpl
import com.example.quipu.domain.repository.NoteRepository
import com.example.quipu.domain.repository.ReminderRepository
import com.example.quipu.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindReminderAlarmScheduler(
        impl: AndroidReminderAlarmScheduler,
    ): ReminderAlarmScheduler

    @Binds
    @Singleton
    abstract fun bindReminderNotifier(impl: ReminderNotificationManager): ReminderNotifier
}
