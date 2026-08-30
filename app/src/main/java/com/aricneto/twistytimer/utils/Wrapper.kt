package com.aricneto.twistytimer.utils

/**
 * A simple wrapper for other objects. This is useful when using a `Loader` with an activity
 * or fragment if the same loaded object can be modified and returned as "new" data when a change
 * to the data is notified. In this case, the `LoaderManager` will not call
 * `onLoadFinished` unless the loaded object is different from the one previously delivered.
 * To work around this, the same data can be returned, but in a different `Wrapper` each time,
 * so `onLoadFinished` will be fired as expected.
 *
 * @param T The type of the content object being wrapped.
 *
 * @author damo
 */
class Wrapper<T> private constructor(private val mContent: T?) {

    /**
     * Gets the content object from the wrapper.
     *
     * @return The content object. May be `null`.
     */
    fun content(): T? = mContent

    /**
     * Indicates if the content object is `null` or not.
     *
     * @return `true` if the content object is `null`; `false` otherwise.
     */
    fun isEmpty(): Boolean = mContent == null

    /**
     * Creates a new wrapper around the content object taken from this wrapper. The content object
     * may be `null`.
     *
     * @return
     * The new wrapper containing the same content object as this (old) wrapper.
     */
    fun rewrap(): Wrapper<T> = Wrapper(content())

    companion object {
        /**
         * Creates a new wrapper around the given content object.
         *
         * @param content
         * The content object for the wrapper. May be `null`.
         * @param T
         * The type of the content object.
         *
         * @return
         * The new wrapper containing `content`.
         */
        @JvmStatic
        fun <T> wrap(content: T?): Wrapper<T> = Wrapper(content)
    }
}
