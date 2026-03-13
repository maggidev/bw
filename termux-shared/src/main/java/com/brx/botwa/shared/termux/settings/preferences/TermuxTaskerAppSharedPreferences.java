package com.brx.botwa.shared.botwa.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.settings.preferences.AppSharedPreferences;
import com.brx.botwa.shared.settings.preferences.SharedPreferenceUtils;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.BotWAUtils;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAPreferenceConstants.TERMUX_TASKER_APP;
import com.brx.botwa.shared.logger.Logger;

public class BotWATaskerAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "BotWATaskerAppSharedPreferences";

    private  BotWATaskerAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                BotWAConstants.TERMUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                BotWAConstants.TERMUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link BotWATaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_TASKER_PACKAGE_NAME}.
     * @return Returns the {@link BotWATaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static BotWATaskerAppSharedPreferences build(@NonNull final Context context) {
        Context botwaTaskerPackageContext = PackageUtils.getContextForPackage(context, BotWAConstants.TERMUX_TASKER_PACKAGE_NAME);
        if (botwaTaskerPackageContext == null)
            return null;
        else
            return new BotWATaskerAppSharedPreferences(botwaTaskerPackageContext);
    }

    /**
     * Get {@link BotWATaskerAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_TASKER_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link BotWATaskerAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static  BotWATaskerAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context botwaTaskerPackageContext = BotWAUtils.getContextForPackageOrExitApp(context, BotWAConstants.TERMUX_TASKER_PACKAGE_NAME, exitAppOnError);
        if (botwaTaskerPackageContext == null)
            return null;
        else
            return new BotWATaskerAppSharedPreferences(botwaTaskerPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_TASKER_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_TASKER_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }



    public int getLastPendingIntentRequestCode() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, TERMUX_TASKER_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE);
    }

    public void setLastPendingIntentRequestCode(int lastPendingIntentRequestCode) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_TASKER_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, lastPendingIntentRequestCode, false);
    }

}
