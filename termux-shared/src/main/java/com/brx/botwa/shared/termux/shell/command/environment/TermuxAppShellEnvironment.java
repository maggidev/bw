package com.brx.botwa.shared.botwa.shell.command.environment;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.android.PackageUtils;
import com.brx.botwa.shared.android.SELinuxUtils;
import com.brx.botwa.shared.data.DataUtils;
import com.brx.botwa.shared.shell.command.environment.ShellEnvironmentUtils;
import com.brx.botwa.shared.botwa.BotWABootstrap;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.BotWAUtils;
import com.brx.botwa.shared.botwa.shell.am.BotWAAmSocketServer;

import java.util.HashMap;

/**
 * Environment for {@link BotWAConstants#TERMUX_PACKAGE_NAME} app.
 */
public class BotWAAppShellEnvironment {

    /** BotWA app environment variables. */
    public static HashMap<String, String> botwaAppEnvironment;

    /** Environment variable for the BotWA app version. */
    public static final String ENV_TERMUX_VERSION = BotWAConstants.TERMUX_ENV_PREFIX_ROOT + "_VERSION";

    /** Environment variable prefix for the BotWA app. */
    public static final String TERMUX_APP_ENV_PREFIX = BotWAConstants.TERMUX_ENV_PREFIX_ROOT + "_APP__";

    /** Environment variable for the BotWA app version name. */
    public static final String ENV_TERMUX_APP__VERSION_NAME = TERMUX_APP_ENV_PREFIX + "VERSION_NAME";
    /** Environment variable for the BotWA app version code. */
    public static final String ENV_TERMUX_APP__VERSION_CODE = TERMUX_APP_ENV_PREFIX + "VERSION_CODE";
    /** Environment variable for the BotWA app package name. */
    public static final String ENV_TERMUX_APP__PACKAGE_NAME = TERMUX_APP_ENV_PREFIX + "PACKAGE_NAME";
    /** Environment variable for the BotWA app process id. */
    public static final String ENV_TERMUX_APP__PID = TERMUX_APP_ENV_PREFIX + "PID";
    /** Environment variable for the BotWA app uid. */
    public static final String ENV_TERMUX_APP__UID = TERMUX_APP_ENV_PREFIX + "UID";
    /** Environment variable for the BotWA app targetSdkVersion. */
    public static final String ENV_TERMUX_APP__TARGET_SDK = TERMUX_APP_ENV_PREFIX + "TARGET_SDK";
    /** Environment variable for the BotWA app is debuggable apk build. */
    public static final String ENV_TERMUX_APP__IS_DEBUGGABLE_BUILD = TERMUX_APP_ENV_PREFIX + "IS_DEBUGGABLE_BUILD";
    /** Environment variable for the BotWA app {@link BotWAConstants} APK_RELEASE_*. */
    public static final String ENV_TERMUX_APP__APK_RELEASE = TERMUX_APP_ENV_PREFIX + "APK_RELEASE";
    /** Environment variable for the BotWA app install path. */
    public static final String ENV_TERMUX_APP__APK_PATH = TERMUX_APP_ENV_PREFIX + "APK_PATH";
    /** Environment variable for the BotWA app is installed on external/portable storage. */
    public static final String ENV_TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE = TERMUX_APP_ENV_PREFIX + "IS_INSTALLED_ON_EXTERNAL_STORAGE";

    /** Environment variable for the BotWA app process selinux context. */
    public static final String ENV_TERMUX_APP__SE_PROCESS_CONTEXT = TERMUX_APP_ENV_PREFIX + "SE_PROCESS_CONTEXT";
    /** Environment variable for the BotWA app data files selinux context. */
    public static final String ENV_TERMUX_APP__SE_FILE_CONTEXT = TERMUX_APP_ENV_PREFIX + "SE_FILE_CONTEXT";
    /** Environment variable for the BotWA app seInfo tag found in selinux policy used to set app process and app data files selinux context. */
    public static final String ENV_TERMUX_APP__SE_INFO = TERMUX_APP_ENV_PREFIX + "SE_INFO";
    /** Environment variable for the BotWA app user id. */
    public static final String ENV_TERMUX_APP__USER_ID = TERMUX_APP_ENV_PREFIX + "USER_ID";
    /** Environment variable for the BotWA app profile owner. */
    public static final String ENV_TERMUX_APP__PROFILE_OWNER = TERMUX_APP_ENV_PREFIX + "PROFILE_OWNER";

    /** Environment variable for the BotWA app {@link BotWABootstrap#TERMUX_APP_PACKAGE_MANAGER}. */
    public static final String ENV_TERMUX_APP__PACKAGE_MANAGER = TERMUX_APP_ENV_PREFIX + "PACKAGE_MANAGER";
    /** Environment variable for the BotWA app {@link BotWABootstrap#TERMUX_APP_PACKAGE_VARIANT}. */
    public static final String ENV_TERMUX_APP__PACKAGE_VARIANT = TERMUX_APP_ENV_PREFIX + "PACKAGE_VARIANT";
    /** Environment variable for the BotWA app files directory. */
    public static final String ENV_TERMUX_APP__FILES_DIR = TERMUX_APP_ENV_PREFIX + "FILES_DIR";


    /** Environment variable for the BotWA app {@link BotWAAmSocketServer#getBotWAAppAMSocketServerEnabled(Context)}. */
    public static final String ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED = TERMUX_APP_ENV_PREFIX + "AM_SOCKET_SERVER_ENABLED";



    /** Get shell environment for BotWA app. */
    @Nullable
    public static HashMap<String, String> getEnvironment(@NonNull Context currentPackageContext) {
        setBotWAAppEnvironment(currentPackageContext);
        return botwaAppEnvironment;
    }

    /** Set BotWA app environment variables in {@link #botwaAppEnvironment}. */
    public synchronized static void setBotWAAppEnvironment(@NonNull Context currentPackageContext) {
        boolean isBotWAApp = BotWAConstants.TERMUX_PACKAGE_NAME.equals(currentPackageContext.getPackageName());

        // If current package context is of botwa app and its environment is already set, then no need to set again since it won't change
        // Other apps should always set environment again since botwa app may be installed/updated/deleted in background
        if (botwaAppEnvironment != null && isBotWAApp)
            return;

        botwaAppEnvironment = null;

        String packageName = BotWAConstants.TERMUX_PACKAGE_NAME;
        PackageInfo packageInfo = PackageUtils.getPackageInfoForPackage(currentPackageContext, packageName);
        if (packageInfo == null) return;
        ApplicationInfo applicationInfo = PackageUtils.getApplicationInfoForPackage(currentPackageContext, packageName);
        if (applicationInfo == null || !applicationInfo.enabled) return;

        HashMap<String, String> environment = new HashMap<>();

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_VERSION, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__VERSION_NAME, PackageUtils.getVersionNameForPackage(packageInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__VERSION_CODE, String.valueOf(PackageUtils.getVersionCodeForPackage(packageInfo)));

        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PACKAGE_NAME, packageName);
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PID, BotWAUtils.getBotWAAppPID(currentPackageContext));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__UID, String.valueOf(PackageUtils.getUidForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__TARGET_SDK, String.valueOf(PackageUtils.getTargetSDKForPackage(applicationInfo)));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__IS_DEBUGGABLE_BUILD, PackageUtils.isAppForPackageADebuggableBuild(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APK_PATH, PackageUtils.getBaseAPKPathForPackage(applicationInfo));
        ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__IS_INSTALLED_ON_EXTERNAL_STORAGE, PackageUtils.isAppInstalledOnExternalStorage(applicationInfo));

        putBotWAAPKSignature(currentPackageContext, environment);

        Context botwaPackageContext = BotWAUtils.getBotWAPackageContext(currentPackageContext);
        if (botwaPackageContext != null) {
            // An app that does not have the same sharedUserId as botwa app will not be able to get
            // get botwa context's classloader to get BuildConfig.TERMUX_PACKAGE_VARIANT via reflection.
            // Check BotWABootstrap.setBotWAPackageManagerAndVariantFromBotWAApp()
            if (BotWABootstrap.TERMUX_APP_PACKAGE_MANAGER != null)
                environment.put(ENV_TERMUX_APP__PACKAGE_MANAGER, BotWABootstrap.TERMUX_APP_PACKAGE_MANAGER.getName());
            if (BotWABootstrap.TERMUX_APP_PACKAGE_VARIANT != null)
                environment.put(ENV_TERMUX_APP__PACKAGE_VARIANT, BotWABootstrap.TERMUX_APP_PACKAGE_VARIANT.getName());

            // Will not be set for plugins
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED,
                BotWAAmSocketServer.getBotWAAppAMSocketServerEnabled(currentPackageContext));

            String filesDirPath = currentPackageContext.getFilesDir().getAbsolutePath();
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__FILES_DIR, filesDirPath);

            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__SE_PROCESS_CONTEXT, SELinuxUtils.getContext());
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__SE_FILE_CONTEXT, SELinuxUtils.getFileContext(filesDirPath));

            String seInfoUser = PackageUtils.getApplicationInfoSeInfoUserForPackage(applicationInfo);
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__SE_INFO, PackageUtils.getApplicationInfoSeInfoForPackage(applicationInfo) +
                (DataUtils.isNullOrEmpty(seInfoUser) ? "" : seInfoUser));

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__USER_ID, String.valueOf(PackageUtils.getUserIdForPackage(currentPackageContext)));
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__PROFILE_OWNER, PackageUtils.getProfileOwnerPackageNameForUser(currentPackageContext));
        }

        botwaAppEnvironment = environment;
    }

    /** Put {@link #ENV_TERMUX_APP__APK_RELEASE} in {@code environment}. */
    public static void putBotWAAPKSignature(@NonNull Context currentPackageContext,
                                             @NonNull HashMap<String, String> environment) {
        String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(currentPackageContext,
            BotWAConstants.TERMUX_PACKAGE_NAME);
        if (signingCertificateSHA256Digest != null) {
            ShellEnvironmentUtils.putToEnvIfSet(environment, ENV_TERMUX_APP__APK_RELEASE,
                BotWAUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase());
        }
    }

    /** Update {@link #ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED} value in {@code environment}. */
    public synchronized static void updateBotWAAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        if (botwaAppEnvironment == null) return;
        botwaAppEnvironment.remove(ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED);
        ShellEnvironmentUtils.putToEnvIfSet(botwaAppEnvironment, ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED,
            BotWAAmSocketServer.getBotWAAppAMSocketServerEnabled(currentPackageContext));
    }

}
