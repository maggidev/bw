package com.brx.botwa.shared.botwa.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.settings.preferences.AppSharedPreferences;
import com.brx.botwa.shared.settings.preferences.SharedPreferenceUtils;
import com.brx.botwa.shared.botwa.BotWAUtils;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAPreferenceConstants.TERMUX_STYLING_APP;
import com.brx.botwa.shared.botwa.BotWAConstants;

public class BotWAStylingAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "BotWAStylingAppSharedPreferences";

    private BotWAStylingAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                BotWAConstants.TERMUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                BotWAConstants.TERMUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link BotWAStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_STYLING_PACKAGE_NAME}.
     * @return Returns the {@link BotWAStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static BotWAStylingAppSharedPreferences build(@NonNull final Context context) {
        Context botwaStylingPackageContext = PackageUtils.getContextForPackage(context, BotWAConstants.TERMUX_STYLING_PACKAGE_NAME);
        if (botwaStylingPackageContext == null)
            return null;
        else
            return new BotWAStylingAppSharedPreferences(botwaStylingPackageContext);
    }

    /**
     * Get {@link BotWAStylingAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_STYLING_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link BotWAStylingAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static BotWAStylingAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context botwaStylingPackageContext = BotWAUtils.getContextForPackageOrExitApp(context, BotWAConstants.TERMUX_STYLING_PACKAGE_NAME, exitAppOnError);
        if (botwaStylingPackageContext == null)
            return null;
        else
            return new BotWAStylingAppSharedPreferences(botwaStylingPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_STYLING_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_STYLING_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
