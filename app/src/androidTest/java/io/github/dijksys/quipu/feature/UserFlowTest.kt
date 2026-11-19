package io.github.dijksys.quipu.feature

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.MainActivity
import io.github.dijksys.quipu.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun createNoteAndKeepItAfterActivityRecreation() {
        val unique = System.currentTimeMillis()
        val title = "Nota E2E $unique"
        val content = "Contenido E2E $unique"

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_new_note").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_new_note").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("note_title").performTextInput(title)
        composeRule.onNodeWithTag("note_content").performTextInput(content)
        composeRule.onNodeWithContentDescription(
            composeRule.activity.getString(R.string.action_back),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }

    @Test
    fun createReminderAndFindItInTheRemindersList() {
        val unique = System.currentTimeMillis()
        val title = "Recordatorio E2E $unique"

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_new_reminder").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_new_reminder").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("reminder_title").performTextInput(title)
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.reminder_editor_save),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_new_reminder").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.nav_reminders),
        ).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }
}
