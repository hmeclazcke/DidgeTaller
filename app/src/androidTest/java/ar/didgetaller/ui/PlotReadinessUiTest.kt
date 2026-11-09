package ar.didgetaller.ui

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import ar.didgetaller.audio.VisualState
import ar.didgetaller.core.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/** Synthetic in-memory plot; no audio, recording, preferences or asynchronous loader. */
class PlotReadinessUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val viewport = PlotViewport(AxisWindow(40000.0, 80000.0), AxisWindow(50.0, 150.0))

    @Test fun collapsedPlotCanBecomeReadyAgainAndUsesTheMeasuredViewportForTouch() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        var height by mutableIntStateOf(0); var selected = -1.0
        val state = ExplorerState(InstrumentProfile.DIDGERIDOO)
        compose.setContent {
            Box(Modifier.width(360.dp).height(height.dp)) {
                PlotSurface(state, VisualState(), viewport, emptyList(), null, -1,
                    Modifier.width(360.dp).height(height.dp), onPoint = { time, _, _ -> selected = time })
            }
        }
        compose.onNodeWithTag("time-chart").assertExists()
        compose.runOnIdle { height = 240 }
        val chart = compose.onNodeWithTag("time-chart").assertIsDisplayed()
        val area = chart.fetchSemanticsNode().config[PlotDataArea]
        chart.performTouchInput { click(area.center) }
        compose.runOnIdle { assertEquals(60000.0, selected, 1.0) }
        compose.runOnIdle { height = 0 }; compose.waitForIdle()
        compose.runOnIdle { height = 260 }
        assertTrue(chart.fetchSemanticsNode().config[PlotDataArea].height > 0)
        compose.runOnIdle { assertFalse(state.gestureActive) }
    }

    @Test fun losingTheDrawableFrameCancelsAnActiveScrubWithoutCommittingASeek() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        var height by mutableIntStateOf(240)
        val state = ExplorerState(InstrumentProfile.DIDGERIDOO); val commits = mutableListOf<Long>()
        compose.setContent {
            Box(Modifier.width(360.dp).height(height.dp)) {
                PlotSurface(state, VisualState(), viewport, emptyList(), null, -1,
                    Modifier.width(360.dp).height(height.dp), onScrub = { time, commit ->
                        if (commit) commits += time else state.selection.scrubMs = time
                    })
            }
        }
        val chart = compose.onNodeWithTag("time-chart")
        val area = chart.fetchSemanticsNode().config[PlotDataArea]
        chart.performTouchInput { down(area.center); moveTo(area.center + Offset(100f, 0f)) }
        compose.runOnIdle { assertTrue(state.gestureActive); assertNotNull(state.selection.scrubMs); height = 0 }
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse("A hidden plot must release its gesture ownership", state.gestureActive)
            assertNull(state.selection.scrubMs); assertTrue(commits.isEmpty())
        }
    }
    @Test fun aPlotNoticeUsesDataBoundsWithoutStealingTimelineInput() {
        assumeTrue("Emulator only", Build.HARDWARE in listOf("ranchu", "goldfish"))
        val state = ExplorerState(InstrumentProfile.DIDGERIDOO)
        var selected = -1.0
        val previews = mutableListOf<Long>(); val commits = mutableListOf<Long>()
        compose.setContent {
            Box(Modifier.width(360.dp).height(240.dp)) {
                PlotSurface(state, VisualState(), viewport, emptyList(), null, -1,
                    Modifier.width(360.dp).height(240.dp), notice = "No approved measurements",
                    onPoint = { time, _, _ -> selected = time },
                    onScrub = { time, commit -> if (commit) commits += time else previews += time })
            }
        }
        val chart = compose.onNodeWithTag("time-chart")
        val graph = chart.fetchSemanticsNode()
        val area = graph.config[PlotDataArea]
        val notice = compose.onNodeWithTag("plot-notice").assertIsDisplayed()
        val text = notice.fetchSemanticsNode().boundsInRoot
        assertTrue(text.left >= graph.boundsInRoot.left + area.left)
        assertTrue(text.top >= graph.boundsInRoot.top + area.top)
        assertTrue(text.right <= graph.boundsInRoot.left + area.right)
        assertTrue(text.bottom <= graph.boundsInRoot.top + area.bottom)
        val touch = text.center - graph.boundsInRoot.topLeft
        val expected = viewport.x.low + (touch.x - area.left) / area.width * viewport.x.span
        notice.performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(expected, selected, 1.0) }
        chart.performTouchInput {
            down(touch)
            moveTo(Offset(area.left + area.width * .75f, touch.y))
            up()
        }
        compose.runOnIdle {
            assertTrue(previews.isNotEmpty())
            assertEquals(70000.0, previews.last().toDouble(), 2.0)
            assertEquals(70000.0, commits.single().toDouble(), 2.0)
            assertFalse(state.gestureActive)
        }
    }

}
