package com.brx.botwa.shared.botwa.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.settings.preferences.AppSharedPreferences;
import com.brx.botwa.shared.settings.preferences.SharedPreferenceUtils;
import com.brx.botwa.shared.botwa.BotWAUtils;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAPreferenceConstants.TERMUX_WIDGET_APP;
import com.brx.botwa.shared.botwa.BotWAConstants;

import java.util.UUID;

public class BotWAWidgetAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "BotWAWidgetAppSharedPreferences";

    private BotWAWidgetAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                BotWAConstants.TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                BotWAConstants.TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link BotWAWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_WIDGET_PACKAGE_NAME}.
     * @return Returns the {@link BotWAWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static BotWAWidgetAppSharedPreferences build(@NonNull final Context context) {
        Context botwaWidgetPackageContext = PackageUtils.getContextForPackage(context, BotWAConstants.TERMUX_WIDGET_PACKAGE_NAME);
        if (botwaWidgetPackageContext == null)
            return null;
        else
            return new BotWAWidgetAppSharedPreferences(botwaWidgetPackageContext);
    }

    /**
     * Get the {@link BotWAWidgetAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_WIDGET_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link BotWAWidgetAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static BotWAWidgetAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context botwaWidgetPackageContext = BotWAUtils.getContextForPackageOrExitApp(context, BotWAConstants.TERMUX_WIDGET_PACKAGE_NAME, exitAppOnError);
        if (botwaWidgetPackageContext == null)
            return null;
        else
            return new BotWAWidgetAppSharedPreferences(botwaWidgetPackageContext);
    }



    public static String getGeneratedToken(@NonNull Context context) {
        BotWAWidgetAppSharedPreferences preferences = BotWAWidgetAppSharedPreferences.build(context, true);
        if (preferences == null) return null;
        return preferences.getGeneratedToken();
    }

    public String getGeneratedToken() {
        String token =  SharedPreferenceUtils.getString(mSharedPreferences, TERMUX_WIDGET_APP.KEY_TOKEN, null, true);
        if (token == null) {
            token = UUID.randomUUID().toString();
            SharedPreferenceUtils.setString(mSharedPreferences, TERMUX_WIDGET_APP.KEY_TOKEN, token, true);
        }
        return token;
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_WIDGET_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_WIDGET_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
