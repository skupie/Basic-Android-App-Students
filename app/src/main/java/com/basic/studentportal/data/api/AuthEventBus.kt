package com.basic.studentportal.data.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton event bus that AuthInterceptor uses to signal a 401 Unauthenticated
 * response. MainActivity collects this and redirects to LoginActivity.
 * Using SharedFlow so the event is fire-and-forget with no replay.
 */
@Singleton
class AuthEventBus @Inject constructor() {
    private val _unauthorizedEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val unauthorizedEvent = _unauthorizedEvent.asSharedFlow()

    fun sendUnauthorizedEvent() {
        _unauthorizedEvent.tryEmit(Unit)
    }
}
