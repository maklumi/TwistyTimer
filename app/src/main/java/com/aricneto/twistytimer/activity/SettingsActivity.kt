package com.aricneto.twistytimer.activity

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.ActivitySettingsBinding
import com.aricneto.twistytimer.fragment.SettingsFragment
import com.aricneto.twistytimer.listener.OnBackPressedInFragmentListener
import com.aricneto.twistytimer.utils.LocaleUtils.updateLocale
import com.aricneto.twistytimer.utils.Prefs
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTextStyle
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme

class SettingsActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private var binding: ActivitySettingsBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(preferredTheme)

        // Set text styling
        if (Prefs.getString(R.string.pk_text_style, "default") != "default") {
            theme.applyStyle(preferredTextStyle, true)
        }

        updateLocale(applicationContext)

        this.enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.root) { v: View, insets: WindowInsetsCompat ->
            v.setPadding(
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        binding!!.back.setOnClickListener { onBackPressedDispatcher.onBackPressed() }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_activity_container, SettingsFragment(), "fragment_settings")
                .commit()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val settingsFragment = supportFragmentManager.findFragmentByTag("fragment_settings")

                if (settingsFragment is OnBackPressedInFragmentListener) {
                    if (settingsFragment.onBackPressedInFragment()) {
                        return
                    }
                }

                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat,
        pref: PreferenceScreen
    ): Boolean {
        val fragment = SettingsFragment()
        val args = Bundle()
        args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, pref.key)
        fragment.arguments = args

        supportFragmentManager.beginTransaction()
            .replace(R.id.main_activity_container, fragment, "fragment_settings")
            .addToBackStack(pref.key)
            .commit()
        return true
    }

    override fun onResume() {
        try {
            super.onResume()
        } catch (e: Exception) {
            Log.e(TAG, "Lifecycle exception", e)
        }
    }

    fun onRecreateRequired() {
        Handler(Looper.getMainLooper()).post {
            ActivityCompat.recreate(this@SettingsActivity)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    companion object {
        private val TAG: String = SettingsActivity::class.java.simpleName
    }
}
