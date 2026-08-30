package com.aricneto.twistytimer.watcher

import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher

/**
 * A TextWatcher to format a number input into a valid time input for the user
 */
class SolveTimeNumberTextWatcher : TextWatcher {
    private var isFormatting = false
    private var mLen = 0
    private var mUnformatted: String = ""

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
    }

    override fun afterTextChanged(s: Editable) {
        if (isFormatting) return

        isFormatting = true

        // Since the keyboard input type is "number", we can't punctuation with the actual
        // filters, so we clear them and restore once we finish formatting
        val filters = s.filters // save filters
        s.filters = arrayOf<InputFilter>() // clear filters


        // Clear all formatting from editable
        // Regex matches the characters ':', '.', 'h' and a leading zero, if present
        mUnformatted = s.toString().replace("^0+|[h]|:|\\.".toRegex(), "")
        mLen = mUnformatted.length

        s.clear()
        s.insert(0, mUnformatted)


        if (mLen <= 2 && mLen > 0) { // 12 -> 0.12
            s.insert(0, "0.")
        } else if (mLen == 3) { // 123 -> 1.23
            s.insert(1, ".")
        } else if (mLen == 4) { // 1234 -> 12.34
            s.insert(2, ".")
        } else if (mLen == 5) { // 12345 -> 1:23.45
            s.insert(1, ":")
            s.insert(4, ".")
        } else if (mLen == 6) { // 123456 -> 12:34.56
            s.insert(2, ":")
            s.insert(5, ".")
        } else if (mLen == 7) { // 1234567 -> 1:23:45.67
            s.insert(1, "h")
            s.insert(4, ":")
            s.insert(7, ".")
        } else if (mLen == 8) { // 12345678 -> 12:34:56.78
            s.insert(2, "h")
            s.insert(5, ":")
            s.insert(8, ".")
        }

        isFormatting = false

        // Restore filters
        s.filters = filters
    }
}
