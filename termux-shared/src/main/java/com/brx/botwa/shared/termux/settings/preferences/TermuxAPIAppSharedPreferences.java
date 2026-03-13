package com.brx.botwa.shared.botwa.settings.preferences;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.settings.preferences.AppSharedPreferences;
import com.brx.botwa.shared.settings.preferences.SharedPreferenceUtils;
import com.brx.botwa.shared.botwa.BotWAUtils;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAPreferenceConstants.TERMUX_API_APP;
import com.brx.botwa.shared.botwa.BotWAConstants;

public class BotWAAPIAppSharedPreferences extends AppSharedPreferences {

    private static final String LOG_TAG = "BotWAAPIAppSharedPreferences";

    private BotWAAPIAppSharedPreferences(@NonNull Context context) {
        super(context,
            SharedPreferenceUtils.getPrivateSharedPreferences(context,
                BotWAConstants.TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION),
            SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context,
                BotWAConstants.TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION));
    }

    /**
     * Get {@link BotWAAPIAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_API_PACKAGE_NAME}.
     * @return Returns the {@link BotWAAPIAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static BotWAAPIAppSharedPreferences build(@NonNull final Context context) {
        Context botwaAPIPackageContext = PackageUtils.getContextForPackage(context, BotWAConstants.TERMUX_API_PACKAGE_NAME);
        if (botwaAPIPackageContext == null)
            return null;
        else
            return new BotWAAPIAppSharedPreferences(botwaAPIPackageContext);
    }

    /**
     * Get {@link BotWAAPIAppSharedPreferences}.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link BotWAConstants#TERMUX_API_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link BotWAAPIAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static BotWAAPIAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context botwaAPIPackageContext = BotWAUtils.getContextForPackageOrExitApp(context, BotWAConstants.TERMUX_API_PACKAGE_NAME, exitAppOnError);
        if (botwaAPIPackageContext == null)
            return null;
        else
            return new BotWAAPIAppSharedPreferences(botwaAPIPackageContext);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMUX_API_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_API_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_API_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }


    public int getLastPendingIntentRequestCode() {
        return SharedPreferenceUtils.getInt(mSharedPreferences, TERMUX_API_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, TERMUX_API_APP.DEFAULT_VALUE_KEY_LAST_PENDING_INTENT_REQUEST_CODE);
    }

    public void setLastPendingIntentRequestCode(int lastPendingIntentRequestCode) {
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMUX_API_APP.KEY_LAST_PENDING_INTENT_REQUEST_CODE, lastPendingIntentRequestCode, true);
    }

}
