package io.github.dijksys.quipu.feature.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.theme.QuipuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lightThemeShowsIdentityAndFeatures() {
        composeRule.setContent {
            QuipuTheme(darkTheme = false) {
                WelcomeScreen(isCompleting = false, error = null, onStart = {})
            }
        }

        composeRule.onNodeWithTag("welcome_logo", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_headline").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_notes_icon", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_reminders_icon", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_privacy_icon", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_start").assertIsDisplayed().assertIsEnabled()
        composeRule.onNodeWithTag("welcome_footer").assertIsDisplayed()
    }

    @Test
    fun darkThemeShowsIdentityAndFeatures() {
        composeRule.setContent {
            QuipuTheme(darkTheme = true) {
                WelcomeScreen(isCompleting = false, error = null, onStart = {})
            }
        }

        composeRule.onNodeWithTag("welcome_headline").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_start").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_footer").assertIsDisplayed()
        composeRule.onNodeWithText("Privacidad local").assertIsDisplayed()
    }

    @Test
    fun compactScreenKeepsTheStartButtonAccessible() {
        composeRule.setContent {
            QuipuTheme {
                Box(Modifier.size(width = 320.dp, height = 420.dp)) {
                    WelcomeScreen(isCompleting = false, error = null, onStart = {})
                }
            }
        }

        composeRule.onNodeWithTag("welcome_start").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_footer").assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_headline").assertIsDisplayed()
    }

    @Test
    fun completingDisablesTheButton() {
        composeRule.setContent {
            QuipuTheme {
                WelcomeScreen(isCompleting = true, error = null, onStart = {})
            }
        }

        composeRule.onNodeWithTag("welcome_start").assertIsNotEnabled()
    }

    @Test
    fun completionErrorIsVisibleAndRetryRemainsAvailable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        composeRule.setContent {
            QuipuTheme {
                WelcomeScreen(
                    isCompleting = false,
                    error = WelcomeError.COMPLETION,
                    onStart = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.welcome_error_completion),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("welcome_start").assertIsEnabled()
    }
}
