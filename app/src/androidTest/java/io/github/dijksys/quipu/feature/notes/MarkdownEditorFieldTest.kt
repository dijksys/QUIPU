package io.github.dijksys.quipu.feature.notes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.dijksys.quipu.core.designsystem.theme.QuipuTheme
import io.github.dijksys.quipu.domain.formatting.MarkdownVisualPlanner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownEditorFieldTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun autolinkOpensWhenTapped() {
        var opened: String? = null
        setEditor("https://example.com/path") { opened = it }

        composeRule.onNodeWithTag("editor_test")
            .performTouchInput { click(Offset(10f, 10f)) }

        composeRule.runOnIdle {
            assertEquals("https://example.com/path", opened)
        }
    }

    @Test
    fun bareDomainOpensWithHttps() {
        var opened: String? = null
        setEditor("platform.deepseek.com") { opened = it }

        composeRule.onNodeWithTag("editor_test")
            .performTouchInput { click(Offset(10f, 10f)) }

        composeRule.runOnIdle {
            assertEquals("https://platform.deepseek.com", opened)
        }
    }

    @Test
    fun normalTextDoesNotOpenAnything() {
        var opened: String? = null
        setEditor("texto https://example.com") { opened = it }

        composeRule.onNodeWithTag("editor_test")
            .performTouchInput { click(Offset(5f, 10f)) }

        composeRule.runOnIdle { assertNull(opened) }

        composeRule.onNodeWithTag("editor_test")
            .performTouchInput { click(Offset(200f, 10f)) }

        composeRule.runOnIdle { assertEquals("https://example.com", opened) }
    }

    @Test
    fun invalidUrlAndEmailDoNotOpen() {
        var opened: String? = null
        setEditor("usuario@example.com palabra...otra") { opened = it }

        composeRule.onNodeWithTag("editor_test")
            .performTouchInput { click(Offset(10f, 10f)) }

        composeRule.runOnIdle { assertNull(opened) }
    }

    @Test
    fun longPressSelectsInsteadOfOpening() {
        var opened: String? = null
        setEditor("https://example.com") { opened = it }

        composeRule.onNodeWithTag("editor_test")
            .performTouchInput { longClick(Offset(10f, 10f)) }

        composeRule.runOnIdle { assertNull(opened) }
    }

    private fun setEditor(
        initialText: String,
        onOpenLink: (String) -> Unit,
    ) {
        composeRule.setContent {
            QuipuTheme {
                var value by remember { mutableStateOf(TextFieldValue(initialText)) }
                var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                val plan = remember(value.text) { MarkdownVisualPlanner.plan(value.text) }
                val scheme = MaterialTheme.colorScheme
                val transformation = remember(
                    plan,
                    scheme.primary,
                    scheme.surfaceVariant,
                    scheme.surfaceContainerHigh,
                ) {
                    MarkdownVisualTransformation(
                        plan = plan,
                        linkColor = scheme.primary,
                        inlineCodeBackground = scheme.surfaceVariant,
                        codeBlockBackground = scheme.surfaceContainerHigh,
                        markerColor = scheme.primary,
                    )
                }

                Box(Modifier.width(320.dp)) {
                    MarkdownEditorField(
                        value = value,
                        onValueChange = { value = it },
                        visualTransformation = transformation,
                        plan = plan,
                        textLayout = layout,
                        onTextLayout = { layout = it },
                        onOpenLink = onOpenLink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_test"),
                    )
                }
            }
        }
    }
}
