package io.github.dijksys.quipu.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.MainActivity
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.common.DateTimeUtils
import io.github.dijksys.quipu.core.datastore.AppSettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UserFlowTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        runBlocking {
            AppSettingsDataStore(context).setOnboardingCompleted(true)
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        scenario.close()
    }

    @Test
    fun homeShowsDateAndTypographicHeader() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_heading").fetchSemanticsNodes().isNotEmpty()
        }

        val expectedDate = DateTimeUtils.formatLongDate(System.currentTimeMillis())
            .replaceFirstChar { it.uppercase() }

        composeRule.onNodeWithTag("home_date").assertIsDisplayed()
        composeRule.onNodeWithTag("home_date").assertTextEquals(expectedDate)
        composeRule.onNodeWithTag("home_heading").assertTextEquals(
            context.getString(R.string.home_heading),
        )
        composeRule.onNodeWithTag("home_subtitle").assertTextEquals(
            context.getString(R.string.home_subtitle),
        )
        composeRule.onNodeWithTag("home_new_note").assertIsDisplayed()
    }

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
            context.getString(R.string.action_back),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()

        scenario.recreate()
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
            context.getString(R.string.reminder_editor_save),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_new_reminder").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            context.getString(R.string.nav_reminders),
        ).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(title).assertIsDisplayed()
    }
}
