package com.aricneto.twistytimer.listener

/**
 * A listener interface to be implemented by fragments that need to be notified if the "Back"
 * button has been pressed. This allows fragments to take actions in response to the "Back" button.
 * A fragment can "consume" the press or not. If consumed, the caller should take no further action
 * to propagate the button press event.
 * 
 * @author damo
 */
interface OnBackPressedInFragmentListener {
    /**
     * Notifies the listening fragment that the "Back" button has been pressed. If the fragment
     * needs to take some action, it should return `true` to mark the event as "consumed".
     * 
     * @return
     * `true` if the event was consumed by the fragment and should not be propagated
     * any further; or `false` if the event was ignored by the fragment and should be
     * propagated to other listeners.
     */
    fun onBackPressedInFragment(): Boolean
}
