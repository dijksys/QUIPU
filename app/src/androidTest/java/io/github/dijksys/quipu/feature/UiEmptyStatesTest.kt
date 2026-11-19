package io.github.dijksys.quipu.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.theme.QuipuTheme
import io.github.dijksys.quipu.domain.model.NoteFilter
import io.github.dijksys.quipu.domain.model.ReminderFilter
import io.github.dijksys.quipu.feature.notes.EmptyNotesState
import io.github.dijksys.quipu.feature.reminders.ReminderEmptyState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiEmptyStatesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun notesEmptyStateShowsMessageAndAction() {
        composeRule.setContent {
            QuipuTheme {
                EmptyNotesState(
                    filter = NoteFilter.ACTIVE,
                    hasQuery = false,
                    onBack = {},
                    onCreateNote = {},
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.notes_empty_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.notes_empty_action))
            .assertIsDisplayed()
    }

    @Test
    fun notesSearchEmptyStateIsDistinct() {
        composeRule.setContent {
            QuipuTheme {
                EmptyNotesState(
                    filter = NoteFilter.ACTIVE,
                    hasQuery = true,
                    onBack = {},
                    onCreateNote = {},
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.notes_search_empty_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.notes_search_empty_message))
            .assertIsDisplayed()
    }

    @Test
    fun remindersEmptyStateShowsCreateAction() {
        composeRule.setContent {
            QuipuTheme {
                ReminderEmptyState(
                    filter = ReminderFilter.PENDING,
                    onCreateReminder = {},
                )
            }
        }

        composeRule
            .onNodeWithText(context.getString(R.string.reminders_empty_pending_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.reminders_empty_action))
            .assertIsDisplayed()
    }
}
