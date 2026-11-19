package io.github.dijksys.quipu.di

import io.github.dijksys.quipu.core.alarms.AndroidReminderAlarmScheduler
import io.github.dijksys.quipu.core.alarms.ReminderAlarmScheduler
import io.github.dijksys.quipu.core.notifications.ReminderNotificationManager
import io.github.dijksys.quipu.core.notifications.ReminderNotifier
import io.github.dijksys.quipu.data.repository.NoteRepositoryImpl
import io.github.dijksys.quipu.data.repository.ReminderRepositoryImpl
import io.github.dijksys.quipu.data.repository.SettingsRepositoryImpl
import io.github.dijksys.quipu.domain.repository.NoteRepository
import io.github.dijksys.quipu.domain.repository.ReminderRepository
import io.github.dijksys.quipu.domain.repository.SettingsRepository
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
