package com.aricneto.twistytimer.solver


interface Tip {
    val tipId: String
    val puzzleId: String
    val tipDescription: String
    fun getTip(scramble: String): String
}