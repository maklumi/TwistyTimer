package com.aricneto.twistytimer.utils

import android.content.res.Resources
import androidx.annotation.BoolRes
import com.aricneto.twistytimer.TwistyTimer

/**
 * Utility class to facilitate accessing the default arguments for preferences
 */
object DefaultPrefs {

    private var mRes: Resources? = null

    /**
     * Gets the default shared preferences for this application.
     *
     * @return The default shared preferences.
     */
    @JvmStatic
    fun getRes(): Resources {
        if (mRes == null) {
            mRes = TwistyTimer.getAppContext().resources
        }
        return mRes!!
    }

    /**
     * Returns the boolean value assigned to the resource key
     *
     * @param defaultResID The resource key ID
     * @return The resource value
     */
    @JvmStatic
    fun getBoolean(@BoolRes defaultResID: Int): Boolean {
        return getRes().getBoolean(defaultResID)
    }
}
