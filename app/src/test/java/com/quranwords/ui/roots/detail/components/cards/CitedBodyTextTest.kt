package com.quranwords.ui.roots.detail.components.cards

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CitedBodyTextTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun session_reused_when_body_unchanged_and_renumbered_when_body_changes() {
        val tick = mutableStateOf(0)
        val body = mutableStateOf("نص [1]")
        val sessions = mutableListOf<Any>()

        compose.setContent {
            val session = rememberCitationSession(body.value)
            SideEffect { sessions += session }
            Text("tick=${tick.value}")
        }

        compose.runOnIdle { tick.value++ }
        compose.runOnIdle {
            assertSame(sessions[0], sessions[1])
        }

        compose.runOnIdle { body.value = "نص آخر [1]" }
        compose.runOnIdle {
            assertNotSame(sessions[1], sessions[2])
        }
    }
}
