package com.brx.botwa.shared.botwa.shell.command.environment;

import android.content.Context;
import android.content.pm.PackageInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.shell.command.environment.ShellEnvironmentUtils;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.BotWAUtils;

import java.util.HashMap;

/**
 * Environment for {@link BotWAConstants#TERMUX_API_PACKAGE_NAME} app.
 */
public class BotWAAPIShellEnvironment {

    /** Environment variable prefix for the BotWA:API app. */
    public static final String TERMUX_API_APP_ENV_PREFIX = BotWAConstants.TERMUX_ENV_PREFIX_ROOT + "_API_APP__";

    /** Environment variable for the BotWA:API app version. */
    public static final String ENV_TERMUX_API_APP__VERSION_NAME = TERMUX_API_APP_ENV_PREFIX + "VERSION_NAME";

    /** Get shell environment for BotWA:API app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        if (BotWAUtils.isBotWAAPIAppInstalled(currentPackageContext) != null) return null;

        String packageName = BotWAConstants.TERMUX_API_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return null;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_API_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));

        return environment;
    }

}
