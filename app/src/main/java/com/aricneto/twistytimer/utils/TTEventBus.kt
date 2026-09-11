package com.aricneto.twistytimer.utils

import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * A simple event bus implementation using SharedFlow to replace LocalBroadcastManager.
 */
object TTEventBus {
    private val _events = MutableSharedFlow<Intent>(extraBufferCapacity = 64)
    val events: SharedFlow<Intent> = _events.asSharedFlow()

    /**
     * Posts an event (Intent) to the bus.
     */
    fun post(event: Intent) {
        _events.tryEmit(event)
    }
}
