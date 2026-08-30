package com.aricneto.twistytimer.solver

object StringUtils {
    fun join(separator: String, values: Array<String>): String {
        if (values.size == 0) {
            return ""
        }

        val s = StringBuilder()
        for (i in 0..<values.size - 1) {
            s.append(values[i]).append(separator)
        }
        s.append(values[values.size - 1])

        return s.toString()
    }
}
