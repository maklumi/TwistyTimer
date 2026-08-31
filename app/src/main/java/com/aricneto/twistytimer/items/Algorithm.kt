package com.aricneto.twistytimer.items

/**
 * Stores an algorithm for use in the alg list
 */
data class Algorithm(
    val id: Long,
    var subset: String = "",
    var name: String = "",
    var state: String = "",
    var algs: String = "",
    var progress: Int
)
