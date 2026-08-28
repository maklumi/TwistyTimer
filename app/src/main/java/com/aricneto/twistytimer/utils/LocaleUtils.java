package com.aricneto.twistytimer.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Pair;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.TwistyTimer;

import java.util.LinkedHashMap;
import java.util.Locale;

/**
 * Utility class to facilitate locale customization by the user
 * Modified from <a href="http://gunhansancar.com/change-language-programmatically-in-android/">...</a>
 */
public class LocaleUtils {

    // The codes used are alpha-2 ISO 639-1, followed by underline
    // and alpha-2 ISO 3166 country/subdivision code if necessary.

    // English is separated into "normal" and "USA" since America has its own date format
    public static final String ARABIC            = "ar_SA";
    public static final String INDONESIAN        = "in_ID";
    public static final String ENGLISH           = "en_GB";
    public static final String ENGLISH_USA       = "en_US";


    public static Context updateLocale(Context context) {
        // toString returns language + "_" + country + "_" + (variant + "_#" | "#") + script + "-" + extensions
        String language = Prefs.getString(R.string.pk_locale, Locale.getDefault().toString());
        return updateResources(context, language);
    }

    /**
     * Gets current locale
     *
     * @return the locale
     */
    public static String getLocale() {
        return Prefs.getString(R.string.pk_locale, Locale.getDefault().getLanguage());
    }

    private static LinkedHashMap<String, Pair<Integer, Integer>> localeHash = null;

    /**
     * Returns a HashMap containing {@link Pair} with each language name and flag resource ID.
     * The languages are keyed by the locale code
     */
    public static LinkedHashMap getLocaleHashMap() {
        if (localeHash == null) {
            localeHash = new LinkedHashMap<String, Pair<Integer, Integer>>() {
                {
                    put(ENGLISH, new Pair<>(R.string.language_english, R.drawable.flag_united_kingdom));
                    put(ENGLISH_USA, new Pair<>(R.string.language_english, R.drawable.flag_united_states));
                    put(INDONESIAN, new Pair<>(R.string.language_indonesian, R.drawable.flag_indonesia));
                    put(ARABIC, new Pair<>(R.string.language_arabic, R.drawable.flag_sudan));
                }
            };
        }

        return localeHash;
    }

    /**
     * Returns an array with all available locale codes
     */
    public static String[] getLocaleArray() {
        return (String[]) getLocaleHashMap().keySet().toArray(new String[0]);
    }

    /**
     * Sets current locale
     *
     * @param language the language code (use one of the constants)
     */
    public static void setLocale(String language) {
        Prefs.edit().putString(R.string.pk_locale, language).apply();
        updateLocale(TwistyTimer.getAppContext());
    }


    private static Context updateResources(Context context, String language) {
        Locale locale = fetchLocaleFromString(language);
        Configuration configuration = context.getResources().getConfiguration();
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }

    /**
     * Converts a string like pt_BR into its appropriate locale object
     */
    private static Locale fetchLocaleFromString(String language) {
        if (language.length() > 2) {
            // e.g.: pt_BR becomes "pt" (index 0) and "BR" (index 1)
            String[] localeString = language.split("_|-");
            return new Locale(localeString[0], localeString[1]);
        } else {
            // e.g.: en, pt
            return new Locale(language);
        }
    }
}
