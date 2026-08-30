package com.aricneto.twistytimer.utils

import android.view.View
import androidx.core.view.isGone

/**
 * Created by Ari on 15/03/2016.
 */
object AnimUtils {
    @JvmStatic
    fun toggleContentVisibility(vararg views: View) {
        for (v in views) {
            if (v.isGone) {
                v.alpha = 0f
                v.visibility = View.VISIBLE
                v.animate()
                    .alpha(1f)
                    .start()
            } else {
                v.animate()
                    .alpha(0f)
                    .setDuration(100)
                    .withEndAction { v.visibility = View.GONE }
                    .start()
            }
        }
    }
}
