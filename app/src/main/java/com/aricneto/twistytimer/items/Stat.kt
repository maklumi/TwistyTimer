package com.aricneto.twistytimer.items

/**
 * Stores a statistic, for use in [com.aricneto.twistytimer.adapter.StatGridAdapter]
 */
data class Stat(
    var time: String = "",
    val row: Int,
) {
    /**
     * Secondary constructor to match Java's Stat(String, int, int)
     * The scope parameter is currently unused in the original Java implementation.
     */
    constructor(time: String = "", scope: Int, row: Int) : this(time, row)

    companion object {
        const val SCOPE_GLOBAL = 0
        const val SCOPE_SESSION = 1
        const val SCOPE_CURRENT = 2
    }
}
