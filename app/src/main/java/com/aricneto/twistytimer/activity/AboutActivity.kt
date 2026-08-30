package com.aricneto.twistytimer.activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import android.util.Xml
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.aricneto.twistify.R
import com.aricneto.twistify.databinding.ActivityAboutBinding
import com.aricneto.twistytimer.utils.LocaleUtils.updateLocale
import com.aricneto.twistytimer.utils.StoreUtils.getStringFromRaw
import com.aricneto.twistytimer.utils.ThemeUtils.preferredTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class AboutActivity : AppCompatActivity() {
    private var binding: ActivityAboutBinding? = null

    var clickListener: View.OnClickListener = View.OnClickListener { view: View ->
        when (view.id) {
            R.id.feedbackButton -> {
                val emailIntent =
                    Intent(Intent.ACTION_SENDTO, ("mailto:" + "aricnetodev@gmail.com").toUri())
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback")
                startActivity(
                    Intent.createChooser(
                        emailIntent,
                        getString(R.string.send_email_title)
                    )
                )
            }

            R.id.licenseButton -> showLicensesDialog()
            R.id.rateButton -> try {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        ("market://details?id=$APP_NAME").toUri()
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            R.id.testersButton -> MaterialAlertDialogBuilder(this@AboutActivity)
                .setTitle(R.string.testers)
                .setMessage(R.string.testers_content)
                .setPositiveButton(R.string.action_ok, null)
                .show()

            R.id.translatorsButton -> MaterialAlertDialogBuilder(this@AboutActivity)
                .setTitle(R.string.translators)
                .setMessage(
                    getString(
                        R.string.translators_content,
                        getStringFromRaw(resources, R.raw.translators)
                    )
                )
                .setPositiveButton(R.string.action_ok, null)
                .show()

            R.id.contributorsButton -> MaterialAlertDialogBuilder(this@AboutActivity)
                .setTitle(R.string.contributors)
                .setMessage(
                    getString(
                        R.string.contributors_content,
                        getString(R.string.contributors_names)
                    )
                )
                .setPositiveButton(R.string.action_ok, null)
                .show()

            R.id.sourceButton -> {
                val browserIntent =
                    Intent(Intent.ACTION_VIEW, "https://github.com/aricneto/TwistyTimer".toUri())
                startActivity(browserIntent)
            }

            R.id.translateButton -> {
                val translateBrowserIntent =
                    Intent(Intent.ACTION_VIEW, "https://crwd.in/twisty-timer".toUri())
                startActivity(translateBrowserIntent)
            }
        }
    }

    private fun showLicensesDialog() {
        val builder = StringBuilder()
        try {
            resources.openRawResource(R.raw.notices_app).use { stream ->
                val parser = Xml.newPullParser()
                parser.setInput(stream, "UTF-8")

                var eventType = parser.eventType
                var currentTag = ""
                var name = ""
                var url = ""
                var copyright = ""
                var license = ""
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        currentTag = parser.name
                    } else if (eventType == XmlPullParser.TEXT) {
                        val text = parser.text.trim { it <= ' ' }
                        if (!text.isEmpty()) {
                            when (currentTag) {
                                "name" -> name = text
                                "url" -> url = text
                                "copyright" -> copyright = text
                                "license" -> license = text
                            }
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (parser.name == "notice") {
                            if (!name.isEmpty()) {
                                builder.append("<b>").append(name).append("</b><br>")
                                if (!url.isEmpty()) builder.append("<small>").append(url)
                                    .append("</small><br>")
                                if (!copyright.isEmpty()) builder.append(copyright).append("<br>")
                                if (!license.isEmpty()) builder.append("<i>").append(license)
                                    .append("</i><br>")
                                builder.append("<br>")
                            }
                            name = ""
                            url = ""
                            copyright = ""
                            license = ""
                        }
                    }
                    eventType = parser.next()
                }
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.license)
            .setMessage(
                Html.fromHtml(
                    builder.toString(),
                    Html.FROM_HTML_MODE_LEGACY
                )
            )
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(updateLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(preferredTheme)

        this.enableEdgeToEdge()

        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        ViewCompat.setOnApplyWindowInsetsListener(
            binding!!.getRoot()
        ) { v: View, insets: WindowInsetsCompat ->
            v.setPadding(
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
            insets
        }

        binding!!.back.setOnClickListener { _: View -> onBackPressedDispatcher.onBackPressed() }

        try {
            val versionName = packageManager.getPackageInfo(packageName, 0).versionName
            binding!!.appVersion.text = versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        binding!!.feedbackButton.setOnClickListener(clickListener)
        binding!!.licenseButton.setOnClickListener(clickListener)
        binding!!.rateButton.setOnClickListener(clickListener)
        binding!!.testersButton.setOnClickListener(clickListener)
        binding!!.sourceButton.setOnClickListener(clickListener)
        binding!!.translatorsButton.setOnClickListener(clickListener)
        binding!!.contributorsButton.setOnClickListener(clickListener)
        binding!!.translateButton.setOnClickListener(clickListener)
    }

    companion object {
        private const val APP_NAME = "com.aricneto.twistytimer"
    }
}
