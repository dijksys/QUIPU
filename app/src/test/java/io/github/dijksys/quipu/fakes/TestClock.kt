package io.github.dijksys.quipu.fakes

import io.github.dijksys.quipu.core.common.Clock

class TestClock(var currentTime: Long = 1_700_000_000_000L) : Clock {
    override fun nowMillis(): Long = currentTime
}
