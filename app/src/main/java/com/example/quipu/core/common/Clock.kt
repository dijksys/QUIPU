package com.example.quipu.core.common

/** Abstracción de tiempo para mantener casos de uso y ViewModels testeables. */
fun interface Clock {
    fun nowMillis(): Long
}

class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}
