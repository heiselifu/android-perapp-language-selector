package com.takeruf.perapplocale.root

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * The app's single root touchpoint: it asks the device's `su` for a shell and reports whether the
 * shell came back as uid 0.
 *
 * Scope is deliberately narrow. This class only *requests* root and remembers the answer; the
 * privileged work the app already does keeps going through Shizuku untouched. [request] is called
 * from the root card on the setup screen and nowhere else, so the system's root prompt only ever
 * appears when the user tapped for it — never on launch, and never as a side effect of some other
 * action.
 *
 * A process-wide [state] is enough because, unlike Shizuku, there is no binder to watch: the answer
 * only changes when the user answers the prompt, which only [request] triggers. It is still exposed
 * as a [StateFlow] so Compose can render it without any extra plumbing.
 */
object RootRepository {

    private const val SU = "su"
    private const val ID_COMMAND = "id"

    private val _state = MutableStateFlow(RootState.UNKNOWN)
    val state: StateFlow<RootState> = _state.asStateFlow()

    /**
     * Runs `su -c id` and keeps the outcome.
     *
     * Suspends on the IO dispatcher: `su` blocks until the user answers the root prompt (some
     * managers make them confirm on every call), and that is exactly the delay a "Requesting…"
     * label is there to cover.
     */
    suspend fun request(): RootState = withContext(Dispatchers.IO) {
        probe().also { _state.value = it }
    }

    /** Forgets the last answer so the next screen visit starts from [RootState.UNKNOWN] again. */
    fun reset() {
        _state.value = RootState.UNKNOWN
    }

    private fun probe(): RootState {
        val process = try {
            ProcessBuilder(SU, "-c", ID_COMMAND).redirectErrorStream(true).start()
        } catch (e: IOException) {
            // No su on the PATH: a non-rooted device, and there is nothing to prompt for.
            return RootState.NOT_AVAILABLE
        }
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        return if (exitCode == 0 && output.contains("uid=0")) {
            RootState.GRANTED
        } else {
            RootState.DENIED
        }
    }
}
