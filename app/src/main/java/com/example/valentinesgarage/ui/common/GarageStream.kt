package com.example.valentinesgarage.ui.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * A repository stream that is safe for a screen to collect.
 *
 * Firestore reports listener failures - a missing index, security rules that
 * deny the read, credentials that have expired - by closing the flow with an
 * exception. A [StateFlow] built straight on top of such a flow rethrows that
 * exception on the main dispatcher, which terminates the process rather than
 * showing the user anything. Every screen therefore consumes streams through
 * this wrapper: [items] falls back to an empty list and the failure is exposed
 * through [errorMessage] so the UI can explain itself.
 */
class GarageStream<T>(
    val items: StateFlow<List<T>>,
    val errorMessage: StateFlow<String?>
)

/**
 * Shares this stream in [scope], converting a terminal failure into state.
 *
 * Sharing stops five seconds after the last subscriber leaves, so returning to
 * a screen after a failure starts a fresh listener and clears the message.
 */
fun <T> Flow<List<T>>.asGarageStream(scope: CoroutineScope): GarageStream<T> {
    val errors = MutableStateFlow<String?>(null)
    val items = onEach { errors.value = null }
        .catch { throwable ->
            errors.value = throwable.message ?: "Live workshop data is unavailable right now."
            emit(emptyList())
        }
        .stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), emptyList())
    return GarageStream(items, errors.asStateFlow())
}

/** Reports the first stream that is currently failing, so one banner covers a whole screen. */
fun combineErrors(
    scope: CoroutineScope,
    streams: List<GarageStream<*>>
): StateFlow<String?> = combine(streams.map(GarageStream<*>::errorMessage)) { messages ->
    messages.firstOrNull { it != null }
}.stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

private const val STOP_TIMEOUT_MILLIS = 5_000L
