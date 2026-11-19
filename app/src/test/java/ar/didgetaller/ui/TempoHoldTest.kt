package ar.didgetaller.ui

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TempoHoldTest {
    @Test fun repeatsProgressivelyAndStopsWhenReleased() = runBlocking {
        val pending = Channel<Pair<Long, CompletableDeferred<Unit>>>(Channel.UNLIMITED)
        val hold = TempoHold(this) { interval ->
            val elapsed = CompletableDeferred<Unit>()
            pending.send(interval to elapsed)
            elapsed.await()
        }
        val plus = Any()
        var bpm = 100
        hold.press(plus)
        assertEquals(100, bpm)
        hold.start(plus) { bpm++; true }
        val intervals = (150L downTo 60L step 10L).toList() + listOf(55L, 55L)
        intervals.forEachIndexed { index, expected ->
            val (interval, elapsed) = pending.receive()
            assertEquals(expected, interval)
            assertEquals(101 + index, bpm)
            if (index == intervals.lastIndex) hold.release(plus)
            elapsed.complete(Unit)
        }
        yield()
        assertEquals(112, bpm)
        assertFalse(hold.owns(plus))
    }

    @Test fun newerPressOwnsRepeatEvenWhenTheOldPointerReleasesLate() = runBlocking {
        val hold = TempoHold(this) { error("No interval is needed when the boundary is reached") }
        val plus = Any()
        val minus = Any()
        var bpm = 31
        hold.press(plus)
        hold.start(plus) { bpm++; true }
        hold.press(minus)
        hold.start(minus) { bpm--; false }
        hold.release(plus)
        yield()
        assertEquals(30, bpm)
        hold.cancel()
    }
}
