package com.aricneto.twistytimer.utils

import android.content.Context
import android.util.Pair
import com.aricneto.twistify.R
import com.aricneto.twistytimer.TwistyTimer
import com.aricneto.twistytimer.utils.Prefs.edit
import com.aricneto.twistytimer.utils.Prefs.getString
import java.util.Locale
import kotlin.collections.toTypedArray

/**
 * Utility class to facilitate locale customization by the user
 * Modified from [...](http://gunhansancar.com/change-language-programmatically-in-android/)
 */
object LocaleUtils {
    // The codes used are alpha-2 ISO 639-1, followed by underline
    // and alpha-2 ISO 3166 country/subdivision code if necessary.
    // English is separated into "normal" and "USA" since America has its own date format
    const val ARABIC: String = "ar_SA"
    const val INDONESIAN: String = "in_ID"
    const val ENGLISH: String = "en_GB"
    const val ENGLISH_USA: String = "en_US"


    @JvmStatic
    fun updateLocale(context: Context): Context? {
        // toString returns language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions
        val language = getString(R.string.pk_locale, Locale.getDefault().toString())
        return LocaleUtils.updateResources(context, language!!)
    }

    var locale: String?
        /**
         * Gets current locale
         * 
         * @return the locale
         */
        get() = getString(
            R.string.pk_locale,
            Locale.getDefault().getLanguage()
        )
        /**
         * Sets current locale
         * 
         * @param language the language code (use one of the constants)
         */
        set(language) {
            edit().putString(R.string.pk_locale, language).apply()
            updateLocale(TwistyTimer.getAppContext())
        }

    private var localeHash: LinkedHashMap<String, Pair<Int, Int>> = LinkedHashMap()

    val localeHashMap: LinkedHashMap<String, Pair<Int, Int>>
        /**
         * Returns a HashMap containing [Pair] with each language name and flag resource ID.
         * The languages are keyed by the locale code
         */
        get() {
            if (localeHash.isEmpty()) {
                localeHash = object :
                    LinkedHashMap<String, Pair<Int, Int>>() {
                    init {
                        put(
                            ENGLISH,
                            Pair<Int, Int>(
                                R.string.language_english,
                                R.drawable.flag_united_kingdom
                            )
                        )
                        put(
                            ENGLISH_USA,
                            Pair<Int, Int>(
                                R.string.language_english,
                                R.drawable.flag_united_states
                            )
                        )
                        put(
                            INDONESIAN,
                            Pair<Int, Int>(
                                R.string.language_indonesian,
                                R.drawable.flag_indonesia
                            )
                        )
                        put(
                            ARABIC,
                            Pair<Int, Int>(
                                R.string.language_arabic,
                                R.drawable.flag_sudan
                            )
                        )
                    }
                }
            }

            return localeHash
        }

    val localeArray: Array<String>
        /**
         * Returns an array with all available locale codes
         */
        get() = localeHashMap.keys.toTypedArray() as Array<String>


    private fun updateResources(context: Context, language: String): Context? {
        val locale = fetchLocaleFromString(language)
        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        return context.createConfigurationContext(configuration)
    }

    /**
     * Converts a string like pt_BR into its appropriate locale object
     */
    private fun fetchLocaleFromString(language: String): Locale {
        if (language.length > 2) {
            // e.g.: pt_BR becomes "pt" (index 0) and "BR" (index 1)
            val localeString: Array<String?> =
                language.split("_|-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            return Locale(localeString[0], localeString[1])
        } else {
            // e.g.: en, pt
            return Locale(language)
        }
    }
}
