package com.example.quipu.fakes

import com.example.quipu.core.common.Clock

class TestClock(var currentTime: Long = 1_700_000_000_000L) : Clock {
    override fun nowMillis(): Long = currentTime
}
