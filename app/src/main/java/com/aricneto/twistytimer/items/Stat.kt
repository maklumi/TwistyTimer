package com.aricneto.twistytimer.items

/**
 * Stores a statistic, for use in [com.aricneto.twistytimer.adapter.StatGridAdapter]
 */
data class Stat(
    var time: String = "",
    var scope: Int = 0,
    val row: Int,
) {
    companion object {
        const val SCOPE_GLOBAL = 0
        const val SCOPE_SESSION = 1
        const val SCOPE_CURRENT = 2
    }
}
