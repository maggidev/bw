package com.brx.botwa.app;

import android.app.Application;
import android.content.Context;

import com.brx.botwa.BuildConfig;
import com.brx.botwa.shared.errors.Error;
import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.botwa.BotWABootstrap;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.crash.BotWACrashUtils;
import com.brx.botwa.shared.botwa.file.BotWAFileUtils;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAAppSharedPreferences;
import com.brx.botwa.shared.botwa.settings.properties.BotWAAppSharedProperties;
import com.brx.botwa.shared.botwa.shell.command.environment.BotWAShellEnvironment;
import com.brx.botwa.shared.botwa.shell.am.BotWAAmSocketServer;
import com.brx.botwa.shared.botwa.shell.BotWAShellManager;
import com.brx.botwa.shared.botwa.theme.BotWAThemeUtils;

public class BotWAApplication extends Application {

    private static final String LOG_TAG = "BotWAApplication";

    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Set crash handler for the app
        BotWACrashUtils.setDefaultCrashHandler(this);

        // Set log config for the app
        setLogConfig(context);

        Logger.logDebug("Starting Application");

        // Set BotWABootstrap.TERMUX_APP_PACKAGE_MANAGER and BotWABootstrap.TERMUX_APP_PACKAGE_VARIANT
        BotWABootstrap.setBotWAPackageManagerAndVariant(BuildConfig.TERMUX_PACKAGE_VARIANT);

        // Init app wide SharedProperties loaded from botwa.properties
        BotWAAppSharedProperties properties = BotWAAppSharedProperties.init(context);

        // Init app wide shell manager
        BotWAShellManager shellManager = BotWAShellManager.init(context);

        // Set NightMode.APP_NIGHT_MODE
        BotWAThemeUtils.setAppNightMode(properties.getNightMode());

        // Check and create botwa files directory. If failed to access it like in case of secondary
        // user or external sd card installation, then don't run files directory related code
        Error error = BotWAFileUtils.isBotWAFilesDirectoryAccessible(this, true, true);
        boolean isBotWAFilesDirectoryAccessible = error == null;
        if (isBotWAFilesDirectoryAccessible) {
            Logger.logInfo(LOG_TAG, "BotWA files directory is accessible");

            error = BotWAFileUtils.isAppsBotWAAppDirectoryAccessible(true, true);
            if (error != null) {
                Logger.logErrorExtended(LOG_TAG, "Create apps/botwa-app directory failed\n" + error);
                return;
            }

            // Setup botwa-am-socket server
            BotWAAmSocketServer.setupBotWAAmSocketServer(context);
        } else {
            Logger.logErrorExtended(LOG_TAG, "BotWA files directory is not accessible\n" + error);
        }

        // Init BotWAShellEnvironment constants and caches after everything has been setup including botwa-am-socket server
        BotWAShellEnvironment.init(this);

        if (isBotWAFilesDirectoryAccessible) {
            BotWAShellEnvironment.writeEnvironmentToFile(this);
        }
    }

    public static void setLogConfig(Context context) {
        Logger.setDefaultLogTag(BotWAConstants.TERMUX_APP_NAME);

        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        BotWAAppSharedPreferences preferences = BotWAAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel());
    }

}
