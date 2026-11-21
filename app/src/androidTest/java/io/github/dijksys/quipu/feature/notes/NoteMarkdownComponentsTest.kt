package io.github.dijksys.quipu.feature.notes

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.R
import io.github.dijksys.quipu.core.designsystem.theme.QuipuTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteMarkdownComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun toolbarExposesAllFormattingActions() {
        composeRule.setContent {
            QuipuTheme {
                NoteMarkdownToolbar(onAction = {}, onHeading = {})
            }
        }

        listOf(
            "markdown_bold",
            "markdown_italic",
            "markdown_heading",
            "markdown_unordered",
            "markdown_ordered",
            "markdown_checklist",
            "markdown_link",
            "markdown_code",
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }

    @Test
    fun toolbarInvokesActionsAndHeadingLevels() {
        val actions = mutableListOf<MarkdownToolbarAction>()
        val headings = mutableListOf<Int>()
        composeRule.setContent {
            QuipuTheme {
                NoteMarkdownToolbar(
                    onAction = { actions += it },
                    onHeading = { headings += it },
                )
            }
        }

        composeRule.onNodeWithTag("markdown_bold").performClick()
        composeRule.onNodeWithTag("markdown_italic").performClick()
        composeRule.onNodeWithTag("markdown_unordered").performClick()
        composeRule.onNodeWithTag("markdown_ordered").performClick()
        composeRule.onNodeWithTag("markdown_checklist").performClick()
        composeRule.onNodeWithTag("markdown_link").performClick()
        composeRule.onNodeWithTag("markdown_code").performClick()

        composeRule.onNodeWithTag("markdown_heading").performClick()
        composeRule.onNodeWithText(context.getString(R.string.note_markdown_h2))
            .performClick()

        assertEquals(
            listOf(
                MarkdownToolbarAction.BOLD,
                MarkdownToolbarAction.ITALIC,
                MarkdownToolbarAction.UNORDERED_LIST,
                MarkdownToolbarAction.ORDERED_LIST,
                MarkdownToolbarAction.CHECKLIST,
                MarkdownToolbarAction.LINK,
                MarkdownToolbarAction.CODE_BLOCK,
            ),
            actions,
        )
        assertEquals(listOf(2), headings)
    }

    @Test
    fun modeToggleSelectsPreview() {
        var isPreview by mutableStateOf(false)
        composeRule.setContent {
            QuipuTheme {
                NoteEditorModeToggle(
                    isPreview = isPreview,
                    onEdit = { isPreview = false },
                    onPreview = { isPreview = true },
                )
            }
        }

        composeRule.onNodeWithTag("note_mode_edit").assertIsSelected()
        composeRule.onNodeWithTag("note_mode_preview").performClick()
        composeRule.onNodeWithTag("note_mode_preview").assertIsSelected()
    }

    @Test
    fun previewRendersHeadingsListsCodeAndInlineStyles() {
        composeRule.setContent {
            QuipuTheme {
                NoteMarkdownPreview(
                    markdown = """
                        # Título

                        **Importante:** revisar *documentación*.

                        - Uno
                        - Dos

                        1. Primero

                        ```kotlin
                        fun main() {}
                        ```
                    """.trimIndent(),
                    onToggleChecklist = {},
                    onOpenLink = {},
                )
            }
        }

        composeRule.onNodeWithText("Título").assertIsDisplayed()
        composeRule.onNodeWithText("Importante:", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("documentación", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Uno").assertIsDisplayed()
        composeRule.onNodeWithText("Primero", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("fun main() {}", substring = true).assertIsDisplayed()
    }

    @Test
    fun previewCheckboxCanToggleTheMarkdownLine() {
        var toggledLine: Int? = null
        composeRule.setContent {
            QuipuTheme {
                NoteMarkdownPreview(
                    markdown = "- [ ] Comprar componentes",
                    onToggleChecklist = { toggledLine = it },
                    onOpenLink = {},
                )
            }
        }

        composeRule.onNodeWithTag("markdown_checkbox_0").performClick()
        assertEquals(0, toggledLine)
        composeRule.onNodeWithText("Comprar componentes").assertIsDisplayed()
    }

    @Test
    fun previewLinkOpensTheVisibleDestination() {
        var openedUrl: String? = null
        composeRule.setContent {
            QuipuTheme {
                NoteMarkdownPreview(
                    markdown = "[QUIPU](https://github.com/dijksys/QUIPU)",
                    onToggleChecklist = {},
                    onOpenLink = { openedUrl = it },
                )
            }
        }

        composeRule.onNodeWithText("QUIPU").performClick()
        assertEquals("https://github.com/dijksys/QUIPU", openedUrl)
    }

    @Test
    fun previewRendersInDarkTheme() {
        composeRule.setContent {
            QuipuTheme(darkTheme = true) {
                NoteMarkdownPreview(
                    markdown = "## Título\n\nTexto **importante**.",
                    onToggleChecklist = {},
                    onOpenLink = {},
                )
            }
        }

        composeRule.onNodeWithText("Título").assertIsDisplayed()
        composeRule.onNodeWithText("importante", substring = true).assertIsDisplayed()
    }
}
