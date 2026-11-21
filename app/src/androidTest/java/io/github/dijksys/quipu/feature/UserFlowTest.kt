package io.github.dijksys.quipu.feature

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.MainActivity
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.datastore.AppSettingsDataStore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
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
    fun homeShowsProminentSearchField() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_search").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("home_search").assertIsDisplayed()
        composeRule.onNodeWithTag("home_new_note").assertIsDisplayed()
    }

    @Test
    fun markdownFormattingKeepsSelectionFocusAndAutosave() {
        val unique = System.currentTimeMillis()
        val title = "Markdown E2E $unique"

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_new_note").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_new_note").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_content").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("markdown_bold").assertDoesNotExist()

        composeRule.onNodeWithTag("note_title").performTextInput(title)
        composeRule.onNodeWithTag("note_content").performTextInput("Hola mundo")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("markdown_bold").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_content")
            .performTextInputSelection(TextRange(5, 10))
        composeRule.onNodeWithTag("markdown_bold").performClick()
        composeRule.onNodeWithTag("note_content").assertTextContains("Hola **mundo**")

        composeRule.onNodeWithTag("markdown_heading").performClick()
        composeRule.onNodeWithText(context.getString(R.string.note_markdown_h2))
            .performClick()

        composeRule.onNodeWithTag("note_content").assertIsFocused()
        composeRule.onNodeWithTag("markdown_bold").assertIsDisplayed()
        composeRule.onNodeWithTag("note_content")
            .assertTextContains("## Hola **mundo**")

        composeRule.onNodeWithTag("note_content")
            .performTextInputSelection(TextRange(17, 17))
        composeRule.onNodeWithTag("note_content").performTextInput(" final")
        composeRule.onNodeWithTag("note_content")
            .assertTextContains("## Hola **mundo** final")
        composeRule.onNodeWithTag("note_mode_preview").assertDoesNotExist()

        composeRule.onNodeWithContentDescription(
            context.getString(R.string.action_back),
        ).performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onAllNodesWithText(title)[0].performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_content").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_content")
            .assertTextContains("## Hola **mundo** final")
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
    fun homeSearchNavigatesToNotesAndFindsTheNote() {
        val unique = System.currentTimeMillis()
        val title = "Busqueda E2E $unique"

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_new_note").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_new_note").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("note_content").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note_title").performTextInput(title)
        composeRule.onNodeWithTag("note_content").performTextInput("contenido de búsqueda")
        composeRule.onNodeWithContentDescription(
            context.getString(R.string.action_back),
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("home_search").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("home_search").performTextInput(title)
        composeRule.onNodeWithTag("home_search").performImeAction()

        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("notes_search").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("notes_search").assertTextContains(title)
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().size >= 2
        }
        assertTrue(
            composeRule.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty(),
        )
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
