package com.takeruf.perapplocale.root

/**
 * Coarse description of what the device's `su` can currently do for us.
 *
 * Kept to the four answers a user can act on: ask, re-ask, nothing to do, or "this device has
 * no root". [UNKNOWN] is the state before the first request, so the setup screen never claims an
 * answer it does not have yet.
 */
enum class RootState {
    /** Nothing has been asked yet this process. */
    UNKNOWN,

    /** `su` answered with a uid 0 shell. */
    GRANTED,

    /** `su` exists but the request was refused (or never came back as root). */
    DENIED,

    /** The device ships no `su` binary at all. */
    NOT_AVAILABLE,
}
