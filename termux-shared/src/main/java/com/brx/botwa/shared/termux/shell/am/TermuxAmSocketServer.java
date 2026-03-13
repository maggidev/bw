package com.brx.botwa.shared.botwa.shell.am;

import android.content.Context;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.errors.Error;
import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.net.socket.local.LocalClientSocket;
import com.brx.botwa.shared.net.socket.local.LocalServerSocket;
import com.brx.botwa.shared.net.socket.local.LocalSocketManager;
import com.brx.botwa.shared.net.socket.local.LocalSocketManagerClientBase;
import com.brx.botwa.shared.net.socket.local.LocalSocketRunConfig;
import com.brx.botwa.shared.shell.am.AmSocketServerRunConfig;
import com.brx.botwa.shared.shell.am.AmSocketServer;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.crash.BotWACrashUtils;
import com.brx.botwa.shared.botwa.plugins.BotWAPluginUtils;
import com.brx.botwa.shared.botwa.settings.properties.BotWAAppSharedProperties;
import com.brx.botwa.shared.botwa.settings.properties.BotWAPropertyConstants;
import com.brx.botwa.shared.botwa.shell.command.environment.BotWAAppShellEnvironment;

/**
 * A wrapper for {@link AmSocketServer} for botwa-app usage.
 *
 * The static {@link #botwaAmSocketServer} variable stores the {@link LocalSocketManager} for the
 * {@link AmSocketServer}.
 *
 * The {@link BotWAAmSocketServerClient} extends the {@link AmSocketServer.AmSocketServerClient}
 * class to also show plugin error notifications for errors and disallowed client connections in
 * addition to logging the messages to logcat, which are only logged by {@link LocalSocketManagerClientBase}
 * if log level is debug or higher for privacy issues.
 *
 * It uses a filesystem socket server with the socket file at
 * {@link BotWAConstants.TERMUX_APP#TERMUX_AM_SOCKET_FILE_PATH}. It would normally only allow
 * processes belonging to the botwa user and root user to connect to it. If commands are sent by the
 * root user, then the am commands executed will be run as the botwa user and its permissions,
 * capabilities and selinux context instead of root.
 *
 * The `$PREFIX/bin/botwa-am` client connects to the server via `$PREFIX/bin/botwa-am-socket` to
 * run the am commands. It provides similar functionality to "$PREFIX/bin/am"
 * (and "/system/bin/am"), but should be faster since it does not require starting a dalvik vm for
 * every command as done by "am" via botwa/BotWAAm.
 *
 * The server is started by botwa-app Application class but is not started if
 * {@link BotWAPropertyConstants#KEY_RUN_TERMUX_AM_SOCKET_SERVER} is `false` which can be done by
 * adding the prop with value "false" to the "~/.botwa/botwa.properties" file. Changes
 * require botwa-app to be force stopped and restarted.
 *
 * The current state of the server can be checked with the
 * {@link BotWAAppShellEnvironment#ENV_TERMUX_APP__AM_SOCKET_SERVER_ENABLED} env variable, which is exported
 * for all shell sessions and tasks.
 *
 * https://github.com.brx.botwa/botwa-am-socket
 * https://github.com.brx.botwa/BotWAAm
 */
public class BotWAAmSocketServer {

    public static final String LOG_TAG = "BotWAAmSocketServer";

    public static final String TITLE = "BotWAAm";

    /** The static instance for the {@link BotWAAmSocketServer} {@link LocalSocketManager}. */
    private static LocalSocketManager botwaAmSocketServer;

    /** Whether {@link BotWAAmSocketServer} is enabled and running or not. */
    @Keep
    protected static Boolean TERMUX_APP_AM_SOCKET_SERVER_ENABLED;

    /**
     * Setup the {@link AmSocketServer} {@link LocalServerSocket} and start listening for
     * new {@link LocalClientSocket} if enabled.
     *
     * @param context The {@link Context} for {@link LocalSocketManager}.
     */
    public static void setupBotWAAmSocketServer(@NonNull Context context) {
        // Start botwa-am-socket server if enabled by user
        boolean enabled = false;
        if (BotWAAppSharedProperties.getProperties().shouldRunBotWAAmSocketServer()) {
            Logger.logDebug(LOG_TAG, "Starting " + TITLE + " socket server since its enabled");
            start(context);
            if (botwaAmSocketServer != null && botwaAmSocketServer.isRunning()) {
                enabled = true;
                Logger.logDebug(LOG_TAG, TITLE + " socket server successfully started");
            }
        } else {
            Logger.logDebug(LOG_TAG, "Not starting " + TITLE + " socket server since its not enabled");
        }

        // Once botwa-app has started, the server state must not be changed since the variable is
        // exported in shell sessions and tasks and if state is changed, then env of older shells will
        // retain invalid value. User should force stop the app to update state after changing prop.
        TERMUX_APP_AM_SOCKET_SERVER_ENABLED = enabled;
        BotWAAppShellEnvironment.updateBotWAAppAMSocketServerEnabled(context);
    }

    /**
     * Create the {@link AmSocketServer} {@link LocalServerSocket} and start listening for new {@link LocalClientSocket}.
     */
    public static synchronized void start(@NonNull Context context) {
        stop();

        AmSocketServerRunConfig amSocketServerRunConfig = new AmSocketServerRunConfig(TITLE,
            BotWAConstants.TERMUX_APP.TERMUX_AM_SOCKET_FILE_PATH, new BotWAAmSocketServerClient());

        botwaAmSocketServer = AmSocketServer.start(context, amSocketServerRunConfig);
    }

    /**
     * Stop the {@link AmSocketServer} {@link LocalServerSocket} and stop listening for new {@link LocalClientSocket}.
     */
    public static synchronized void stop() {
        if (botwaAmSocketServer != null) {
            Error error = botwaAmSocketServer.stop();
            if (error != null) {
                botwaAmSocketServer.onError(error);
            }
            botwaAmSocketServer = null;
        }
    }
    
    /**
     * Update the state of the {@link AmSocketServer} {@link LocalServerSocket} depending on current
     * value of {@link BotWAPropertyConstants#KEY_RUN_TERMUX_AM_SOCKET_SERVER}.
     */
    public static synchronized void updateState(@NonNull Context context) {
        BotWAAppSharedProperties properties = BotWAAppSharedProperties.getProperties();
        if (properties.shouldRunBotWAAmSocketServer()) {
            if (botwaAmSocketServer == null) {
                Logger.logDebug(LOG_TAG, "updateState: Starting " + TITLE + " socket server");
                start(context);
            }
        } else {
            if (botwaAmSocketServer != null) {
                Logger.logDebug(LOG_TAG, "updateState: Disabling " + TITLE + " socket server");
                stop();
            }
        }
    }
    
    /**
     * Get {@link #botwaAmSocketServer}.
     */
    public static synchronized LocalSocketManager getBotWAAmSocketServer() {
        return botwaAmSocketServer;
    }

    /**
     * Show an error notification on the {@link BotWAConstants#TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID}
     * {@link BotWAConstants#TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME} with a call
     * to {@link BotWAPluginUtils#sendPluginCommandErrorNotification(Context, String, CharSequence, String, String)}.
     *
     * @param context The {@link Context} to send the notification with.
     * @param error The {@link Error} generated.
     * @param localSocketRunConfig The {@link LocalSocketRunConfig} for {@link LocalSocketManager}.
     * @param clientSocket The optional {@link LocalClientSocket} for which the error was generated.
     */
    public static synchronized void showErrorNotification(@NonNull Context context, @NonNull Error error,
                                                          @NonNull LocalSocketRunConfig localSocketRunConfig,
                                                          @Nullable LocalClientSocket clientSocket) {
        BotWAPluginUtils.sendPluginCommandErrorNotification(context, LOG_TAG,
            localSocketRunConfig.getTitle() + " Socket Server Error", error.getMinimalErrorString(),
            LocalSocketManager.getErrorMarkdownString(error, localSocketRunConfig, clientSocket));
    }



    public static Boolean getBotWAAppAMSocketServerEnabled(@NonNull Context currentPackageContext) {
        boolean isBotWAApp = BotWAConstants.TERMUX_PACKAGE_NAME.equals(currentPackageContext.getPackageName());
        if (isBotWAApp) {
            return TERMUX_APP_AM_SOCKET_SERVER_ENABLED;
        } else {
            // Currently, unsupported since plugin app processes don't know that value is set in botwa
            // app process BotWAAmSocketServer class. A binder API or a way to check if server is actually
            // running needs to be used. Long checks would also not be possible on main application thread
            return null;
        }

    }





    /** Enhanced implementation for {@link AmSocketServer.AmSocketServerClient} for {@link BotWAAmSocketServer}. */
    public static class BotWAAmSocketServerClient extends AmSocketServer.AmSocketServerClient {

        public static final String LOG_TAG = "BotWAAmSocketServerClient";

        @Nullable
        @Override
        public Thread.UncaughtExceptionHandler getLocalSocketManagerClientThreadUEH(
            @NonNull LocalSocketManager localSocketManager) {
            // Use botwa crash handler for socket listener thread just like used for main app process thread.
            return BotWACrashUtils.getCrashHandler(localSocketManager.getContext());
        }

        @Override
        public void onError(@NonNull LocalSocketManager localSocketManager,
                            @Nullable LocalClientSocket clientSocket, @NonNull Error error) {
            // Don't show notification if server is not running since errors may be triggered
            // when server is stopped and server and client sockets are closed.
            if (localSocketManager.isRunning()) {
                BotWAAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                    localSocketManager.getLocalSocketRunConfig(), clientSocket);
            }

            // But log the exception
            super.onError(localSocketManager, clientSocket, error);
        }

        @Override
        public void onDisallowedClientConnected(@NonNull LocalSocketManager localSocketManager,
                                                @NonNull LocalClientSocket clientSocket, @NonNull Error error) {
            // Always show notification and log error regardless of if server is running or not
            BotWAAmSocketServer.showErrorNotification(localSocketManager.getContext(), error,
                localSocketManager.getLocalSocketRunConfig(), clientSocket);
            super.onDisallowedClientConnected(localSocketManager, clientSocket, error);
        }



        @Override
        protected String getLogTag() {
            return LOG_TAG;
        }

    }

}
