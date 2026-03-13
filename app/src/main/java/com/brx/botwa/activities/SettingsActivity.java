package com.brx.botwa.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.brx.botwa.R;
import com.brx.botwa.shared.activities.ReportActivity;
import com.brx.botwa.shared.file.FileUtils;
import com.brx.botwa.shared.models.ReportInfo;
import com.brx.botwa.app.models.UserAction;
import com.brx.botwa.shared.interact.ShareUtils;
import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAAPIAppSharedPreferences;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAFloatAppSharedPreferences;
import com.brx.botwa.shared.botwa.settings.preferences.BotWATaskerAppSharedPreferences;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAWidgetAppSharedPreferences;
import com.brx.botwa.shared.android.AndroidUtils;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.BotWAUtils;
import com.brx.botwa.shared.activity.media.AppCompatActivityUtils;
import com.brx.botwa.shared.theme.NightMode;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AppCompatActivityUtils.setNightMode(this, NightMode.getAppNightMode().getName(), true);

        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }

        AppCompatActivityUtils.setToolbar(this, com.brx.botwa.shared.R.id.toolbar);
        AppCompatActivityUtils.setShowBackButtonInActionBar(this, true);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            new Thread() {
                @Override
                public void run() {
                    configureBotWAAPIPreference(context);
                    configureBotWAFloatPreference(context);
                    configureBotWATaskerPreference(context);
                    configureBotWAWidgetPreference(context);
                    configureAboutPreference(context);
                    configureDonatePreference(context);
                }
            }.start();
        }

        private void configureBotWAAPIPreference(@NonNull Context context) {
            Preference botwaAPIPreference = findPreference("botwa_api");
            if (botwaAPIPreference != null) {
                BotWAAPIAppSharedPreferences preferences = BotWAAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                botwaAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureBotWAFloatPreference(@NonNull Context context) {
            Preference botwaFloatPreference = findPreference("botwa_float");
            if (botwaFloatPreference != null) {
                BotWAFloatAppSharedPreferences preferences = BotWAFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                botwaFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureBotWATaskerPreference(@NonNull Context context) {
            Preference botwaTaskerPreference = findPreference("botwa_tasker");
            if (botwaTaskerPreference != null) {
                BotWATaskerAppSharedPreferences preferences = BotWATaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                botwaTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureBotWAWidgetPreference(@NonNull Context context) {
            Preference botwaWidgetPreference = findPreference("botwa_widget");
            if (botwaWidgetPreference != null) {
                BotWAWidgetAppSharedPreferences preferences = BotWAWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                botwaWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(BotWAUtils.getAppInfoMarkdownString(context, BotWAUtils.AppInfoMode.TERMUX_AND_PLUGIN_PACKAGES));
                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context, true));
                            aboutString.append("\n\n").append(BotWAUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();

                            ReportInfo reportInfo = new ReportInfo(userActionName,
                                BotWAConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, title);
                            reportInfo.setReportString(aboutString.toString());
                            reportInfo.setReportSaveFileLabelAndPath(userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(BotWAConstants.TERMUX_APP_NAME + "-" + userActionName + ".log", true, true));

                            ReportActivity.startReportActivity(context, reportInfo);
                        }
                    }.start();

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since BotWA isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = BotWAUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(BotWAConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openUrl(context, BotWAConstants.TERMUX_DONATE_URL);
                    return true;
                });
            }
        }
    }

}
