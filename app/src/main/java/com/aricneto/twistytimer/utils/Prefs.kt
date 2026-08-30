package com.aricneto.twistytimer.utils

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.preference.PreferenceManager
import androidx.annotation.BoolRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes
import com.aricneto.twistytimer.TwistyTimer

/**
 * Utility class to access the default shared preferences.
 *
 * @author damo
 */
object Prefs {
    /**
     * The preferences instance. There is only one shared preferences instance per preferences
     * file for each process, so this can be cached safely and will reflect any changes made by
     * any other code that makes changes to the preferences.
     */
    private var sPrefs: SharedPreferences? = null

    /**
     * Gets the default shared preferences for this application.
     *
     * @return The default shared preferences.
     */
    @JvmStatic
    fun getPrefs(): SharedPreferences {
        if (sPrefs == null) {
            sPrefs = PreferenceManager.getDefaultSharedPreferences(TwistyTimer.getAppContext())
        }
        return sPrefs!!
    }

    /**
     * Returns an [Int] given an [IntegerRes]
     * @param res an [IntegerRes]
     * @return an [Int] associated with the given [IntegerRes]
     */
    @JvmStatic
    fun getDefaultIntValue(@IntegerRes res: Int): Int {
        return TwistyTimer.getAppContext().resources.getInteger(res)
    }

    /**
     * Returns a [Boolean] given a [BoolRes]
     * @param res an [BoolRes]
     * @return an [Boolean] associated with the given [BoolRes]
     */
    @JvmStatic
    fun getDefaultBoolValue(@BoolRes res: Int): Boolean {
        return TwistyTimer.getAppContext().resources.getBoolean(res)
    }

    /**
     * Gets the string value of a shared preference.
     *
     * @param prefKeyResID
     * The string resource ID for the name of the preference. See `values/pref_keys.xml`.
     * @param defaultValue
     * The default preference value to return if the preference is not defined.
     *
     * @return
     * The value of the preference, or the given default value if the preference is not defined.
     */
    @JvmStatic
    fun getString(@StringRes prefKeyResID: Int, defaultValue: String?): String? {
        return getPrefs().getString(
            TwistyTimer.getAppContext().getString(prefKeyResID), defaultValue
        )
    }

    /**
     * Gets the integer value of a shared preference.
     *
     * @param prefKeyResID
     * The string resource ID for the name of the preference. See `values/pref_keys.xml`.
     * @param defaultValue
     * The default preference value to return if the preference is not defined.
     *
     * @return
     * The value of the preference, or the given default value if the preference is not defined.
     */
    @JvmStatic
    fun getInt(@StringRes prefKeyResID: Int, defaultValue: Int): Int {
        return getPrefs().getInt(
            TwistyTimer.getAppContext().getString(prefKeyResID), defaultValue
        )
    }

    /**
     * Gets the Boolean value of a shared preference.
     *
     * @param prefKeyResID
     * The string resource ID for the name of the preference. See `values/pref_keys.xml`.
     * @param defaultValue
     * The default preference value to return if the preference is not defined.
     *
     * @return
     * The value of the preference, or the given default value if the preference is not defined.
     */
    @JvmStatic
    fun getBoolean(@StringRes prefKeyResID: Int, defaultValue: Boolean): Boolean {
        return getPrefs().getBoolean(
            TwistyTimer.getAppContext().getString(prefKeyResID), defaultValue
        )
    }

    /**
     *
     * Identifies the resource ID from those given whose string value matches the given shared
     * preferences key. This is useful when constructing `switch` statements. For example:
     *
     * <pre>
     * String key = preference.getKey();
     *
     * switch (keyToResourceID(key, R.string.pk_1, R.string.pk_2, R.string.pk_3)) {
     *     case R.string.pk_1:
     *         // Do something.
     *         break;
     *     case R.string.pk_2:
     *         // Do something else.
     *         break;
     *     case R.string.pk_3:
     *         // Do something entirely different.
     *         break;
     *     default:
     *         // "key" did not match any of the string value of any of the given resource IDs.
     *         break;
     * }
     * </pre>
     *
     * @param key
     * The string value of the shared preferences key to be matched to a resource ID.
     * @param prefKeyResIDs
     * Any number of string resource IDs. See `values/pref_keys.xml`.
     *
     * @return
     * The first resource ID whose string value matches the given key; or zero if the key is
     * `null`, there are no resource IDs given, or the key does not match the string
     * value of any of the given string resources.
     */
    @JvmStatic
    fun keyToResourceID(key: String?, vararg prefKeyResIDs: Int): Int {
        if (key != null && prefKeyResIDs.isNotEmpty()) {
            val context = TwistyTimer.getAppContext()
            for (resID in prefKeyResIDs) {
                if (key == context.getString(resID)) {
                    return resID
                }
            }
        }
        return 0
    }

    /**
     * Gets an editor for the default shared preferences. When editing is complete, call
     * [Editor.apply] on the editor to save the changes.
     */
    @SuppressLint("CommitPrefEdits")
    @JvmStatic
    fun edit(): Editor {
        return Editor(getPrefs().edit())
    }

    /**
     * A helper function for Kotlin callers to edit preferences idiomatically.
     */
    inline fun edit(action: Editor.() -> Unit) {
        val editor = edit()
        action(editor)
        editor.apply()
    }

    /**
     * A simple wrapper for the shared preference editor that provides a easy way to use string
     * resource IDs when setting preference values.
     */
    class Editor(private val mSPEditor: SharedPreferences.Editor) {

        /**
         * Commits any changes made using this editor.
         */
        fun apply() {
            mSPEditor.apply()
        }

        /**
         * Sets the value of a shared preference to the given string.
         *
         * @param prefKeyResID
         * The string resource ID for the name of the preference key.
         * See `values/pref_keys.xml`.
         * @param value
         * The new value of the preference.
         *
         * @return
         * This editor, to allow method calls to be chained.
         */
        fun putString(@StringRes prefKeyResID: Int, value: String?): Editor {
            mSPEditor.putString(TwistyTimer.getAppContext().getString(prefKeyResID), value)
            return this
        }

        /**
         * Sets the value of a shared preference to the given string set.
         *
         * @param prefKeyResID
         * The string resource ID for the name of the preference key.
         * See `values/pref_keys.xml`.
         * @param value
         * The new value of the preference.
         *
         * @return
         * This editor, to allow method calls to be chained.
         */
        fun putStringSet(@StringRes prefKeyResID: Int, value: Set<String>?): Editor {
            mSPEditor.putStringSet(TwistyTimer.getAppContext().getString(prefKeyResID), value)
            return this
        }

        /**
         * Sets the value of a shared preference to the given integer.
         *
         * @param prefKeyResID
         * The string resource ID for the name of the preference key.
         * See `values/pref_keys.xml`.
         * @param value
         * The new value of the preference.
         *
         * @return
         * This editor, to allow method calls to be chained.
         */
        fun putInt(@StringRes prefKeyResID: Int, value: Int): Editor {
            mSPEditor.putInt(TwistyTimer.getAppContext().getString(prefKeyResID), value)
            return this
        }

        /**
         * Sets the value of a shared preference to the given Boolean.
         *
         * @param prefKeyResID
         * The string resource ID for the name of the preference key.
         * See `values/pref_keys.xml`.
         * @param value
         * The new value of the preference.
         *
         * @return
         * This editor, to allow method calls to be chained.
         */
        fun putBoolean(@StringRes prefKeyResID: Int, value: Boolean): Editor {
            mSPEditor.putBoolean(TwistyTimer.getAppContext().getString(prefKeyResID), value)
            return this
        }
    }
}
