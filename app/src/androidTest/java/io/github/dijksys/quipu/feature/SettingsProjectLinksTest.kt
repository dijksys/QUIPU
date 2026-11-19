package io.github.dijksys.quipu.feature

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.core.designsystem.theme.QuipuTheme
import io.github.dijksys.quipu.feature.settings.ProjectSection
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsProjectLinksTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun projectRowsOpenTheExpectedDestinations() {
        val openedUrls = mutableListOf<String>()
        composeRule.setContent {
            QuipuTheme {
                ProjectSection(onOpenLink = { openedUrls += it })
            }
        }

        composeRule.onNodeWithTag("settings_project_profile").performClick()
        composeRule.onNodeWithTag("settings_project_repository").performClick()

        assertEquals(
            listOf("https://github.com/dijksys", "https://github.com/dijksys/QUIPU"),
            openedUrls,
        )
    }
}
