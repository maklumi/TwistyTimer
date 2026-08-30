package com.aricneto.twistytimer.utils

import java.util.AbstractList

object StatUtils {
    @JvmStatic
    fun asList(l: LongArray): MutableList<Long> {
        return object : AbstractList<Long>() {
            override fun get(index: Int): Long {
                return l[index]
            }

            override fun set(index: Int, element: Long): Long {
                val oldVal = l[index]
                l[index] = element
                return oldVal
            }

            override val size: Int
                get() = l.size
        }
    }
}
