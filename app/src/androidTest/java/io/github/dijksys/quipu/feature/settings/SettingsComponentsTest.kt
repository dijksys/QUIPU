package io.github.dijksys.quipu.feature.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.BuildConfig
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.theme.QuipuTheme
import io.github.dijksys.quipu.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun themeSelectorShowsThreeHorizontalPills() {
        composeRule.setContent {
            QuipuTheme {
                ThemeModeSelector(selected = ThemeMode.SYSTEM, onSelect = {})
            }
        }

        val light = composeRule.onNodeWithTag("settings_theme_light")
            .fetchSemanticsNode().boundsInRoot
        val dark = composeRule.onNodeWithTag("settings_theme_dark")
            .fetchSemanticsNode().boundsInRoot
        val system = composeRule.onNodeWithTag("settings_theme_system")
            .fetchSemanticsNode().boundsInRoot

        assertEquals(light.center.y, dark.center.y, 1f)
        assertEquals(dark.center.y, system.center.y, 1f)
        assertTrue(light.left < dark.left)
        assertTrue(dark.left < system.left)
        composeRule.onNodeWithTag("settings_theme_system").assertIsSelected()
        composeRule.onNodeWithTag("settings_theme_light").assertIsNotSelected()
        composeRule.onNodeWithTag("settings_theme_dark").assertIsNotSelected()
    }

    @Test
    fun themeSelectorInvokesEveryMode() {
        val selected = mutableListOf<ThemeMode>()
        composeRule.setContent {
            QuipuTheme {
                ThemeModeSelector(selected = ThemeMode.SYSTEM, onSelect = { selected += it })
            }
        }

        composeRule.onNodeWithTag("settings_theme_light").performClick()
        composeRule.onNodeWithTag("settings_theme_dark").performClick()
        composeRule.onNodeWithTag("settings_theme_system").performClick()

        assertEquals(
            listOf(ThemeMode.LIGHT, ThemeMode.DARK, ThemeMode.SYSTEM),
            selected,
        )
    }

    @Test
    fun themeSelectorRendersInDarkTheme() {
        composeRule.setContent {
            QuipuTheme(darkTheme = true) {
                ThemeModeSelector(selected = ThemeMode.DARK, onSelect = {})
            }
        }

        composeRule.onNodeWithTag("settings_theme_light").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_theme_dark").assertIsSelected()
        composeRule.onNodeWithTag("settings_theme_system").assertIsDisplayed()
    }

    @Test
    fun notificationToggleCardUpdatesTheSwitch() {
        var enabled by mutableStateOf(true)
        composeRule.setContent {
            QuipuTheme {
                NotificationToggleCard(
                    title = context.getString(R.string.settings_notifications_enable),
                    description = context.getString(R.string.settings_notifications_enable_hint),
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                )
            }
        }

        composeRule.onNodeWithTag("settings_notifications_switch").assertIsOn()
        composeRule.onNodeWithTag("settings_notifications_switch").performClick()
        composeRule.onNodeWithTag("settings_notifications_switch").assertIsOff()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_notifications_enable),
        ).assertIsDisplayed()
    }

    @Test
    fun notificationPermissionCardShowsStatusAndActions() {
        var requested = 0
        var openedSettings = 0
        composeRule.setContent {
            QuipuTheme {
                NotificationPermissionCard(
                    title = context.getString(R.string.settings_notification_permission),
                    description = context.getString(
                        R.string.settings_notification_permission_description,
                    ),
                    state = PermissionState.MISSING,
                    onRequest = { requested++ },
                    onOpenSettings = { openedSettings++ },
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permission_missing),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_notification_permission_request").performClick()
        composeRule.onNodeWithTag("settings_notification_permission_open_settings").performClick()

        assertEquals(1, requested)
        assertEquals(1, openedSettings)
    }

    @Test
    fun exactAlarmCardShowsExplanationAndRequestAction() {
        var requested = 0
        composeRule.setContent {
            QuipuTheme {
                ExactAlarmPermissionCard(
                    title = context.getString(R.string.settings_exact_alarm_permission),
                    description = context.getString(
                        R.string.settings_exact_alarm_permission_description,
                    ),
                    explanation = context.getString(R.string.settings_exact_alarm_explanation),
                    state = PermissionState.MISSING,
                    onRequest = { requested++ },
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.settings_exact_alarm_explanation),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_exact_alarm_request").performClick()
        assertEquals(1, requested)
    }

    @Test
    fun exactAlarmCardShowsNotRequiredWithoutAction() {
        composeRule.setContent {
            QuipuTheme {
                ExactAlarmPermissionCard(
                    title = context.getString(R.string.settings_exact_alarm_permission),
                    description = context.getString(
                        R.string.settings_exact_alarm_permission_description,
                    ),
                    explanation = context.getString(R.string.settings_exact_alarm_explanation),
                    state = PermissionState.NOT_REQUIRED,
                    onRequest = {},
                )
            }
        }

        composeRule.onNodeWithText(
            context.getString(R.string.settings_permission_not_needed),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_exact_alarm_request").assertDoesNotExist()
    }

    @Test
    fun aboutCardShowsLogoVersionPrivacyAndOfflineInformation() {
        composeRule.setContent {
            QuipuTheme {
                AboutCard()
            }
        }

        composeRule.onNodeWithTag("settings_about_logo").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_about_version")
            .assertTextEquals(
                context.getString(R.string.settings_version, BuildConfig.VERSION_NAME),
            )
        composeRule.onNodeWithText(
            context.getString(R.string.settings_about_privacy_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_about_offline_title),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_privacy_message),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            context.getString(R.string.settings_offline_message),
        ).assertIsDisplayed()
        composeRule.onNodeWithTag("settings_project_profile").assertDoesNotExist()
    }

    @Test
    fun aboutAndProjectCardsRenderInDarkTheme() {
        composeRule.setContent {
            QuipuTheme(darkTheme = true) {
                ProjectLinksSection(onOpenLink = {})
            }
        }

        composeRule.onNodeWithTag("settings_project_profile").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_project_repository").assertIsDisplayed()
    }
}
