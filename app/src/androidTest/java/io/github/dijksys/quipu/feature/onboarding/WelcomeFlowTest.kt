package io.github.dijksys.quipu.feature.onboarding

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.MainActivity
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.datastore.AppSettingsDataStore
import io.github.dijksys.quipu.core.notifications.ReminderNotificationManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeFlowTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun resetOnboarding() {
        runBlocking {
            AppSettingsDataStore(context).setOnboardingCompleted(false)
        }
    }

    @After
    fun restoreOnboarding() {
        scenario?.close()
        runBlocking {
            AppSettingsDataStore(context).setOnboardingCompleted(true)
        }
    }

    @Test
    fun welcomeAppearsOnceAndCompletesIntoHome() {
        scenario = ActivityScenario.launch(MainActivity::class.java)

        waitForTag("welcome_start")
        composeRule.onNodeWithTag("welcome_logo", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_tagline", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_headline").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_notes_icon", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_reminders_icon", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_privacy_icon", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_notes_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_reminders_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_privacy_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.welcome_footer))
            .assertIsDisplayed()

        composeRule.onNodeWithTag("welcome_start").performClick()

        waitForTag("home_search")
        composeRule.onNodeWithTag("welcome_start").assertDoesNotExist()
        assertEquals(
            true,
            runBlocking {
                AppSettingsDataStore(context).settings.first().onboardingCompleted
            },
        )

        scenario?.recreate()
        waitForTag("home_search")
        composeRule.onNodeWithTag("welcome_start").assertDoesNotExist()
    }

    @Test
    fun welcomeSurvivesActivityRecreationBeforeCompletion() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForTag("welcome_start")

        scenario?.recreate()

        waitForTag("welcome_start")
        composeRule.onNodeWithTag("welcome_start").assertIsDisplayed()
    }

    @Test
    fun systemBackDoesNotBypassWelcome() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForTag("welcome_start")

        scenario?.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            scenario?.state == androidx.lifecycle.Lifecycle.State.DESTROYED
        }

        scenario = ActivityScenario.launch(MainActivity::class.java)
        waitForTag("welcome_start")
        composeRule.onNodeWithTag("welcome_start").assertIsDisplayed()
    }

    @Test
    fun pendingReminderSurvivesOnboarding() {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(
                ReminderNotificationManager.EXTRA_OPEN_REMINDER_ID,
                "missing-reminder-for-onboarding-test",
            )
        }
        scenario = ActivityScenario.launch(intent)

        waitForTag("welcome_start")
        composeRule.onNodeWithTag("welcome_start").performClick()

        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(
                context.getString(R.string.reminder_not_found_title),
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(
            context.getString(R.string.reminder_not_found_title),
        ).assertIsDisplayed()
    }

    private fun waitForTag(tag: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
