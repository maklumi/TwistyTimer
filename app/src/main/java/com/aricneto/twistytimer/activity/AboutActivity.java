package com.aricneto.twistytimer.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.text.Html;
import android.util.Xml;

import com.aricneto.twistify.R;
import com.aricneto.twistify.databinding.ActivityAboutBinding;
import com.aricneto.twistytimer.utils.LocaleUtils;
import com.aricneto.twistytimer.utils.StoreUtils;
import com.aricneto.twistytimer.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends AppCompatActivity {

    private final static String APP_PNAME = "com.aricneto.twistytimer";

    private ActivityAboutBinding binding;

    View.OnClickListener clickListener = view -> {
        switch (view.getId()) {
            case R.id.feedbackButton:
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + "aricnetodev@gmail.com"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Feedback");
                startActivity(Intent.createChooser(emailIntent, getString(R.string.send_email_title)));
                break;
            case R.id.licenseButton:
                showLicensesDialog();
                break;
            case R.id.rateButton:
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case R.id.testersButton:
                new MaterialAlertDialogBuilder(AboutActivity.this)
                    .setTitle(R.string.testers)
                    .setMessage(R.string.testers_content)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
                break;
            case R.id.translatorsButton:
                new MaterialAlertDialogBuilder(AboutActivity.this)
                    .setTitle(R.string.translators)
                    .setMessage(getString(R.string.translators_content, StoreUtils.getStringFromRaw(getResources(), R.raw.translators)))
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
                break;
            case R.id.contributorsButton:
                new MaterialAlertDialogBuilder(AboutActivity.this)
                        .setTitle(R.string.contributors)
                        .setMessage(getString(R.string.contributors_content, getString(R.string.contributors_names)))
                        .setPositiveButton(R.string.action_ok, null)
                        .show();
                break;
            case R.id.sourceButton:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aricneto/TwistyTimer"));
                startActivity(browserIntent);
                break;
            case R.id.translateButton:
                Intent translateBrowserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://crwd.in/twisty-timer"));
                startActivity(translateBrowserIntent);
                break;
        }
    };

    private void showLicensesDialog() {
        StringBuilder builder = new StringBuilder();
        try (InputStream is = getResources().openRawResource(R.raw.notices_app)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(is, "UTF-8");

            int eventType = parser.getEventType();
            String currentTag = "";
            String name = "", url = "", copyright = "", license = "";

            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    currentTag = parser.getName();
                } else if (eventType == XmlPullParser.TEXT) {
                    String text = parser.getText().trim();
                    if (!text.isEmpty()) {
                        switch (currentTag) {
                            case "name": name = text; break;
                            case "url": url = text; break;
                            case "copyright": copyright = text; break;
                            case "license": license = text; break;
                        }
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("notice")) {
                        if (!name.isEmpty()) {
                            builder.append("<b>").append(name).append("</b><br>");
                            if (!url.isEmpty()) builder.append("<small>").append(url).append("</small><br>");
                            if (!copyright.isEmpty()) builder.append(copyright).append("<br>");
                            if (!license.isEmpty()) builder.append("<i>").append(license).append("</i><br>");
                            builder.append("<br>");
                        }
                        name = ""; url = ""; copyright = ""; license = "";
                    }
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException | IOException e) {
            e.printStackTrace();
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.license)
                .setMessage(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ?
                        Html.fromHtml(builder.toString(), Html.FROM_HTML_MODE_LEGACY) :
                        Html.fromHtml(builder.toString()))
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleUtils.updateLocale(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtils.getPreferredTheme());

        EdgeToEdge.enable(this);

        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        binding.back.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        try {
            String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            binding.appVersion.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        binding.feedbackButton.setOnClickListener(clickListener);
        binding.licenseButton.setOnClickListener(clickListener);
        binding.rateButton.setOnClickListener(clickListener);
        binding.testersButton.setOnClickListener(clickListener);
        binding.sourceButton.setOnClickListener(clickListener);
        binding.translatorsButton.setOnClickListener(clickListener);
        binding.contributorsButton.setOnClickListener(clickListener);
        binding.translateButton.setOnClickListener(clickListener);
    }
}
