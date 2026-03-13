package com.brx.botwa.shared.botwa.shell.command.environment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.brx.botwa.shared.errors.Error;
import com.brx.botwa.shared.file.FileUtils;
import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.shell.command.ExecutionCommand;
import com.brx.botwa.shared.shell.command.environment.AndroidShellEnvironment;
import com.brx.botwa.shared.shell.command.environment.ShellEnvironmentUtils;
import com.brx.botwa.shared.shell.command.environment.ShellCommandShellEnvironment;
import com.brx.botwa.shared.botwa.BotWABootstrap;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.shell.BotWAShellUtils;

import java.nio.charset.Charset;
import java.util.HashMap;

/**
 * Environment for BotWA.
 */
public class BotWAShellEnvironment extends AndroidShellEnvironment {

    private static final String LOG_TAG = "BotWAShellEnvironment";

    /** Environment variable for the botwa {@link BotWAConstants#TERMUX_PREFIX_DIR_PATH}. */
    public static final String ENV_PREFIX = "PREFIX";

    public BotWAShellEnvironment() {
        super();
        shellCommandShellEnvironment = new BotWAShellCommandShellEnvironment();
    }


    /** Init {@link BotWAShellEnvironment} constants and caches. */
    public synchronized static void init(@NonNull Context currentPackageContext) {
        BotWAAppShellEnvironment.setBotWAAppEnvironment(currentPackageContext);
    }

    /** Init {@link BotWAShellEnvironment} constants and caches. */
    public synchronized static void writeEnvironmentToFile(@NonNull Context currentPackageContext) {
        HashMap<String, String> environmentMap = new BotWAShellEnvironment().getEnvironment(currentPackageContext, false);
        String environmentString = ShellEnvironmentUtils.convertEnvironmentToDotEnvFile(environmentMap);

        // Write environment string to temp file and then move to final location since otherwise
        // writing may happen while file is being sourced/read
        Error error = FileUtils.writeTextToFile("botwa.env.tmp", BotWAConstants.TERMUX_ENV_TEMP_FILE_PATH,
            Charset.defaultCharset(), environmentString, false);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
            return;
        }

        error = FileUtils.moveRegularFile("botwa.env.tmp", BotWAConstants.TERMUX_ENV_TEMP_FILE_PATH, BotWAConstants.TERMUX_ENV_FILE_PATH, true);
        if (error != null) {
            Logger.logErrorExtended(LOG_TAG, error.toString());
        }
    }

    /** Get shell environment for BotWA. */
    @NonNull
    @Override
    public HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext, boolean isFailSafe) {

        // BotWA environment builds upon the Android environment
        HashMap<String, String> environment = super.getEnvironment(currentPackageContext, isFailSafe);

        HashMap<String, String> botwaAppEnvironment = BotWAAppShellEnvironment.getEnvironment(currentPackageContext);
        if (botwaAppEnvironment != null)
            environment.putAll(botwaAppEnvironment);

        HashMap<String, String> botwaApiAppEnvironment = BotWAAPIShellEnvironment.getEnvironment(currentPackageContext);
        if (botwaApiAppEnvironment != null)
            environment.putAll(botwaApiAppEnvironment);

        environment.put(ENV_HOME, BotWAConstants.TERMUX_HOME_DIR_PATH);
        environment.put(ENV_PREFIX, BotWAConstants.TERMUX_PREFIX_DIR_PATH);

        // If failsafe is not enabled, then we keep default PATH and TMPDIR so that system binaries can be used
        if (!isFailSafe) {
            environment.put(ENV_TMPDIR, BotWAConstants.TERMUX_TMP_PREFIX_DIR_PATH);
            if (BotWABootstrap.isAppPackageVariantAPTAndroid5()) {
                // BotWA in android 5/6 era shipped busybox binaries in applets directory
                environment.put(ENV_PATH, BotWAConstants.TERMUX_BIN_PREFIX_DIR_PATH + ":" + BotWAConstants.TERMUX_BIN_PREFIX_DIR_PATH + "/applets");
                environment.put(ENV_LD_LIBRARY_PATH, BotWAConstants.TERMUX_LIB_PREFIX_DIR_PATH);
            } else {
                // BotWA binaries on Android 7+ rely on DT_RUNPATH, so LD_LIBRARY_PATH should be unset by default
                environment.put(ENV_PATH, BotWAConstants.TERMUX_BIN_PREFIX_DIR_PATH);
                environment.remove(ENV_LD_LIBRARY_PATH);
            }
        }

        return environment;
    }


    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return BotWAConstants.TERMUX_HOME_DIR_PATH;
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return BotWAConstants.TERMUX_BIN_PREFIX_DIR_PATH;
    }

    @NonNull
    @Override
    public String[] setupShellCommandArguments(@NonNull String executable, String[] arguments) {
        return BotWAShellUtils.setupShellCommandArguments(executable, arguments);
    }

}
