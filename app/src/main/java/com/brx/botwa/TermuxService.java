package com.brx.botwa.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.R;
import com.brx.botwa.app.event.SystemEventReceiver;
import com.brx.botwa.app.terminal.BotWATerminalSessionActivityClient;
import com.brx.botwa.app.terminal.BotWATerminalSessionServiceClient;
import com.brx.botwa.shared.botwa.plugins.BotWAPluginUtils;
import com.brx.botwa.shared.data.IntentUtils;
import com.brx.botwa.shared.net.uri.UriUtils;
import com.brx.botwa.shared.errors.Errno;
import com.brx.botwa.shared.shell.ShellUtils;
import com.brx.botwa.shared.shell.command.runner.app.AppShell;
import com.brx.botwa.shared.botwa.settings.properties.BotWAAppSharedProperties;
import com.brx.botwa.shared.botwa.shell.command.environment.BotWAShellEnvironment;
import com.brx.botwa.shared.botwa.shell.BotWAShellUtils;
import com.brx.botwa.shared.botwa.BotWAConstants;
import com.brx.botwa.shared.botwa.BotWAConstants.TERMUX_APP.TERMUX_ACTIVITY;
import com.brx.botwa.shared.botwa.BotWAConstants.TERMUX_APP.TERMUX_SERVICE;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAAppSharedPreferences;
import com.brx.botwa.shared.botwa.shell.BotWAShellManager;
import com.brx.botwa.shared.botwa.shell.command.runner.terminal.BotWASession;
import com.brx.botwa.shared.botwa.terminal.BotWATerminalSessionClientBase;
import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.notification.NotificationUtils;
import com.brx.botwa.shared.android.PermissionUtils;
import com.brx.botwa.shared.data.DataUtils;
import com.brx.botwa.shared.shell.command.ExecutionCommand;
import com.brx.botwa.shared.shell.command.ExecutionCommand.Runner;
import com.brx.botwa.shared.shell.command.ExecutionCommand.ShellCreateMode;
import com.brx.botwa.terminal.TerminalEmulator;
import com.brx.botwa.terminal.TerminalSession;
import com.brx.botwa.terminal.TerminalSessionClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A service holding a list of {@link BotWASession} in {@link BotWAShellManager#mBotWASessions} and background {@link AppShell}
 * in {@link BotWAShellManager#mBotWATasks}, showing a foreground notification while running so that it is not terminated.
 * The user interacts with the session through {@link BotWAActivity}, but this service may outlive
 * the activity when the user or the system disposes of the activity. In that case the user may
 * restart {@link BotWAActivity} later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 * {@link #buildNotification()}.
 */
public final class BotWAService extends Service implements AppShell.AppShellClient, BotWASession.BotWASessionClient {

    /** This service is only bound from inside the same process and never uses IPC. */
    class LocalBinder extends Binder {
        public final BotWAService service = BotWAService.this;
    }

    private final IBinder mBinder = new LocalBinder();

    private final Handler mHandler = new Handler();


    /** The full implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that holds activity references for activity related functions.
     * Note that the service may often outlive the activity, so need to clear this reference.
     */
    private BotWATerminalSessionActivityClient mBotWATerminalSessionActivityClient;

    /** The basic implementation of the {@link TerminalSessionClient} interface to be used by {@link TerminalSession}
     * that does not hold activity references and only a service reference.
     */
    private final BotWATerminalSessionServiceClient mBotWATerminalSessionServiceClient = new BotWATerminalSessionServiceClient(this);

    /**
     * BotWA app shared properties manager, loaded from botwa.properties
     */
    private BotWAAppSharedProperties mProperties;

    /**
     * BotWA app shell manager
     */
    private BotWAShellManager mShellManager;

    /** The wake lock and wifi lock are always acquired and released together. */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    /** If the user has executed the {@link TERMUX_SERVICE#ACTION_STOP_SERVICE} intent. */
    boolean mWantsToStop = false;

    private static final String LOG_TAG = "BotWAService";

    @Override
    public void onCreate() {
        Logger.logVerbose(LOG_TAG, "onCreate");

        // Get BotWA app SharedProperties without loading from disk since BotWAApplication handles
        // load and BotWAActivity handles reloads
        mProperties = BotWAAppSharedProperties.getProperties();

        mShellManager = BotWAShellManager.getShellManager();

        runStartForeground();

        SystemEventReceiver.registerPackageUpdateEvents(this);
    }

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        String action = null;
        if (intent != null) {
            Logger.logVerboseExtended(LOG_TAG, "Intent Received:\n" + IntentUtils.getIntentString(intent));
            action = intent.getAction();
        }

        if (action != null) {
            switch (action) {
                case TERMUX_SERVICE.ACTION_STOP_SERVICE:
                    Logger.logDebug(LOG_TAG, "ACTION_STOP_SERVICE intent received");
                    actionStopService();
                    break;
                case TERMUX_SERVICE.ACTION_WAKE_LOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_LOCK intent received");
                    actionAcquireWakeLock();
                    break;
                case TERMUX_SERVICE.ACTION_WAKE_UNLOCK:
                    Logger.logDebug(LOG_TAG, "ACTION_WAKE_UNLOCK intent received");
                    actionReleaseWakeLock(true);
                    break;
                case TERMUX_SERVICE.ACTION_SERVICE_EXECUTE:
                    Logger.logDebug(LOG_TAG, "ACTION_SERVICE_EXECUTE intent received");
                    actionServiceExecute(intent);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.logVerbose(LOG_TAG, "onDestroy");

        BotWAShellUtils.clearBotWATMPDIR(true);

        actionReleaseWakeLock(false);
        if (!mWantsToStop)
            killAllBotWAExecutionCommands();

        BotWAShellManager.onAppExit(this);

        SystemEventReceiver.unregisterPackageUpdateEvents(this);

        runStopForeground();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Logger.logVerbose(LOG_TAG, "onUnbind");

        // Since we cannot rely on {@link BotWAActivity.onDestroy()} to always complete,
        // we unset clients here as well if it failed, so that we do not leave service and session
        // clients with references to the activity.
        if (mBotWATerminalSessionActivityClient != null)
            unsetBotWATerminalSessionClient();
        return false;
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(BotWAConstants.TERMUX_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }

    /** Request to stop service. */
    private void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Process action to stop service. */
    private void actionStopService() {
        mWantsToStop = true;
        killAllBotWAExecutionCommands();
        requestStopService();
    }

    /** Kill all BotWASessions and BotWATasks by sending SIGKILL to their processes.
     *
     * For BotWASessions, all sessions will be killed, whether user manually exited BotWA or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will only be done if user manually exited botwa or if the session was started by a plugin
     * which **expects** the result back via a pending intent.
     *
     * For BotWATasks, only tasks that were started by a plugin which **expects** the result
     * back via a pending intent will be killed, whether user manually exited BotWA or if
     * onDestroy() was directly called because of unintended shutdown. The processing of results
     * will always be done for the tasks that are killed. The remaining processes will keep on
     * running until the botwa app process is killed by android, like by OOM, so we let them run
     * as long as they can.
     *
     * Some plugin execution commands may not have been processed and added to mBotWASessions and
     * mBotWATasks lists before the service is killed, so we maintain a separate
     * mPendingPluginExecutionCommands list for those, so that we can notify the pending intent
     * creators that execution was cancelled.
     *
     * Note that if user didn't manually exit BotWA and if onDestroy() was directly called because
     * of unintended shutdown, like android deciding to kill the service, then there will be no
     * guarantee that onDestroy() will be allowed to finish and botwa app process may be killed before
     * it has finished. This means that in those cases some results may not be sent back to their
     * creators for plugin commands but we still try to process whatever results can be processed
     * despite the unreliable behaviour of onDestroy().
     *
     * Note that if don't kill the processes started by plugins which **expect** the result back
     * and notify their creators that they have been killed, then they may get stuck waiting for
     * the results forever like in case of commands started by BotWA:Tasker or RUN_COMMAND intent,
     * since once BotWAService has been killed, no result will be sent back. They may still get
     * stuck if botwa app process gets killed, so for this case reasonable timeout values should
     * be used, like in Tasker for the BotWA:Tasker actions.
     *
     * We make copies of each list since items are removed inside the loop.
     */
    private synchronized void killAllBotWAExecutionCommands() {
        boolean processResult;

        Logger.logDebug(LOG_TAG, "Killing BotWASessions=" + mShellManager.mBotWASessions.size() +
            ", BotWATasks=" + mShellManager.mBotWATasks.size() +
            ", PendingPluginExecutionCommands=" + mShellManager.mPendingPluginExecutionCommands.size());

        List<BotWASession> botwaSessions = new ArrayList<>(mShellManager.mBotWASessions);
        List<AppShell> botwaTasks = new ArrayList<>(mShellManager.mBotWATasks);
        List<ExecutionCommand> pendingPluginExecutionCommands = new ArrayList<>(mShellManager.mPendingPluginExecutionCommands);

        for (int i = 0; i < botwaSessions.size(); i++) {
            ExecutionCommand executionCommand = botwaSessions.get(i).getExecutionCommand();
            processResult = mWantsToStop || executionCommand.isPluginExecutionCommandWithPendingResult();
            botwaSessions.get(i).killIfExecuting(this, processResult);
            if (!processResult)
                mShellManager.mBotWASessions.remove(botwaSessions.get(i));
        }


        for (int i = 0; i < botwaTasks.size(); i++) {
            ExecutionCommand executionCommand = botwaTasks.get(i).getExecutionCommand();
            if (executionCommand.isPluginExecutionCommandWithPendingResult())
                botwaTasks.get(i).killIfExecuting(this, true);
            else
                mShellManager.mBotWATasks.remove(botwaTasks.get(i));
        }

        for (int i = 0; i < pendingPluginExecutionCommands.size(); i++) {
            ExecutionCommand executionCommand = pendingPluginExecutionCommands.get(i);
            if (!executionCommand.shouldNotProcessResults() && executionCommand.isPluginExecutionCommandWithPendingResult()) {
                if (executionCommand.setStateFailed(Errno.ERRNO_CANCELLED.getCode(), this.getString(com.brx.botwa.shared.R.string.error_execution_cancelled))) {
                    BotWAPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);
                }
            }
        }
    }



    /** Process action to acquire Power and Wi-Fi WakeLocks. */
    @SuppressLint({"WakelockTimeout", "BatteryLife"})
    private void actionAcquireWakeLock() {
        if (mWakeLock != null) {
            Logger.logDebug(LOG_TAG, "Ignoring acquiring WakeLocks since they are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Acquiring WakeLocks");

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, BotWAConstants.TERMUX_APP_NAME.toLowerCase() + ":service-wakelock");
        mWakeLock.acquire();

        // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, BotWAConstants.TERMUX_APP_NAME.toLowerCase());
        mWifiLock.acquire();

        if (!PermissionUtils.checkIfBatteryOptimizationsDisabled(this)) {
            PermissionUtils.requestDisableBatteryOptimizations(this);
        }

        updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks acquired successfully");

    }

    /** Process action to release Power and Wi-Fi WakeLocks. */
    private void actionReleaseWakeLock(boolean updateNotification) {
        if (mWakeLock == null && mWifiLock == null) {
            Logger.logDebug(LOG_TAG, "Ignoring releasing WakeLocks since none are already held");
            return;
        }

        Logger.logDebug(LOG_TAG, "Releasing WakeLocks");

        if (mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        if (mWifiLock != null) {
            mWifiLock.release();
            mWifiLock = null;
        }

        if (updateNotification)
            updateNotification();

        Logger.logDebug(LOG_TAG, "WakeLocks released successfully");
    }

    /** Process {@link TERMUX_SERVICE#ACTION_SERVICE_EXECUTE} intent to execute a shell command in
     * a foreground BotWASession or in a background BotWATask. */
    private void actionServiceExecute(Intent intent) {
        if (intent == null) {
            Logger.logError(LOG_TAG, "Ignoring null intent to actionServiceExecute");
            return;
        }

        ExecutionCommand executionCommand = new ExecutionCommand(BotWAShellManager.getNextShellId());

        executionCommand.executableUri = intent.getData();
        executionCommand.isPluginExecutionCommand = true;

        // If EXTRA_RUNNER is passed, use that, otherwise check EXTRA_BACKGROUND and default to Runner.TERMINAL_SESSION
        executionCommand.runner = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_RUNNER,
            (intent.getBooleanExtra(TERMUX_SERVICE.EXTRA_BACKGROUND, false) ? Runner.APP_SHELL.getName() : Runner.TERMINAL_SESSION.getName()));
        if (Runner.runnerOf(executionCommand.runner) == null) {
            String errmsg = this.getString(R.string.error_botwa_service_invalid_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            BotWAPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            return;
        }

        if (executionCommand.executableUri != null) {
            Logger.logVerbose(LOG_TAG, "uri: \"" + executionCommand.executableUri + "\", path: \"" + executionCommand.executableUri.getPath() + "\", fragment: \"" + executionCommand.executableUri.getFragment() + "\"");

            // Get full path including fragment (anything after last "#")
            executionCommand.executable = UriUtils.getUriFilePathWithFragment(executionCommand.executableUri);
            executionCommand.arguments = IntentUtils.getStringArrayExtraIfSet(intent, TERMUX_SERVICE.EXTRA_ARGUMENTS, null);
            if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
                executionCommand.stdin = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_STDIN, null);
            executionCommand.backgroundCustomLogLevel = IntentUtils.getIntegerExtraIfSet(intent, TERMUX_SERVICE.EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL, null);
        }

        executionCommand.workingDirectory = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_WORKDIR, null);
        executionCommand.isFailsafe = intent.getBooleanExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, false);
        executionCommand.sessionAction = intent.getStringExtra(TERMUX_SERVICE.EXTRA_SESSION_ACTION);
        executionCommand.shellName = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_SHELL_NAME, null);
        executionCommand.shellCreateMode = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_SHELL_CREATE_MODE, null);
        executionCommand.commandLabel = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_COMMAND_LABEL, "Execution Intent Command");
        executionCommand.commandDescription = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_COMMAND_DESCRIPTION, null);
        executionCommand.commandHelp = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_COMMAND_HELP, null);
        executionCommand.pluginAPIHelp = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_PLUGIN_API_HELP, null);
        executionCommand.resultConfig.resultPendingIntent = intent.getParcelableExtra(TERMUX_SERVICE.EXTRA_PENDING_INTENT);
        executionCommand.resultConfig.resultDirectoryPath = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_RESULT_DIRECTORY, null);
        if (executionCommand.resultConfig.resultDirectoryPath != null) {
            executionCommand.resultConfig.resultSingleFile = intent.getBooleanExtra(TERMUX_SERVICE.EXTRA_RESULT_SINGLE_FILE, false);
            executionCommand.resultConfig.resultFileBasename = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_RESULT_FILE_BASENAME, null);
            executionCommand.resultConfig.resultFileOutputFormat = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_RESULT_FILE_OUTPUT_FORMAT, null);
            executionCommand.resultConfig.resultFileErrorFormat = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_RESULT_FILE_ERROR_FORMAT, null);
            executionCommand.resultConfig.resultFilesSuffix = IntentUtils.getStringExtraIfSet(intent, TERMUX_SERVICE.EXTRA_RESULT_FILES_SUFFIX, null);
        }

        if (executionCommand.shellCreateMode == null)
            executionCommand.shellCreateMode = ShellCreateMode.ALWAYS.getMode();

        // Add the execution command to pending plugin execution commands list
        mShellManager.mPendingPluginExecutionCommands.add(executionCommand);

        if (Runner.APP_SHELL.equalsRunner(executionCommand.runner))
            executeBotWATaskCommand(executionCommand);
        else if (Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner))
            executeBotWASessionCommand(executionCommand);
        else {
            String errmsg = getString(R.string.error_botwa_service_unsupported_execution_command_runner, executionCommand.runner);
            executionCommand.setStateFailed(Errno.ERRNO_FAILED.getCode(), errmsg);
            BotWAPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
        }
    }





    /** Execute a shell command in background BotWATask. */
    private void executeBotWATaskCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing background \"" + executionCommand.getCommandIdAndLabelLogString() + "\" BotWATask command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        AppShell newBotWATask = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newBotWATask = getBotWATaskForShellName(executionCommand.shellName);
            if (newBotWATask != null)
                Logger.logVerbose(LOG_TAG, "Existing BotWATask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing BotWATask with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newBotWATask == null)
            newBotWATask = createBotWATask(executionCommand);
    }

    /** Create a BotWATask. */
    @Nullable
    public AppShell createBotWATask(String executablePath, String[] arguments, String stdin, String workingDirectory) {
        return createBotWATask(new ExecutionCommand(BotWAShellManager.getNextShellId(), executablePath,
            arguments, stdin, workingDirectory, Runner.APP_SHELL.getName(), false));
    }

    /** Create a BotWATask. */
    @Nullable
    public synchronized AppShell createBotWATask(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" BotWATask");

        if (!Runner.APP_SHELL.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createBotWATask()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        AppShell newBotWATask = AppShell.execute(this, executionCommand, this,
            new BotWAShellEnvironment(), null,false);
        if (newBotWATask == null) {
            Logger.logError(LOG_TAG, "Failed to execute new BotWATask command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                BotWAPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mBotWATasks.add(newBotWATask);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        updateNotification();

        return newBotWATask;
    }

    /** Callback received when a BotWATask finishes. */
    @Override
    public void onAppShellExited(final AppShell botwaTask) {
        mHandler.post(() -> {
            if (botwaTask != null) {
                ExecutionCommand executionCommand = botwaTask.getExecutionCommand();

                Logger.logVerbose(LOG_TAG, "The onBotWATaskExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" BotWATask command");

                // If the execution command was started for a plugin, then process the results
                if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                    BotWAPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

                mShellManager.mBotWATasks.remove(botwaTask);
            }

            updateNotification();
        });
    }





    /** Execute a shell command in a foreground {@link BotWASession}. */
    private void executeBotWASessionCommand(ExecutionCommand executionCommand) {
        if (executionCommand == null) return;

        Logger.logDebug(LOG_TAG, "Executing foreground \"" + executionCommand.getCommandIdAndLabelLogString() + "\" BotWASession command");

        // Transform executable path to shell/session name, e.g. "/bin/do-something.sh" => "do-something.sh".
        if (executionCommand.shellName == null && executionCommand.executable != null)
            executionCommand.shellName = ShellUtils.getExecutableBasename(executionCommand.executable);

        BotWASession newBotWASession = null;
        ShellCreateMode shellCreateMode = processShellCreateMode(executionCommand);
        if (shellCreateMode == null) return;
        if (ShellCreateMode.NO_SHELL_WITH_NAME.equals(shellCreateMode)) {
            newBotWASession = getBotWASessionForShellName(executionCommand.shellName);
            if (newBotWASession != null)
                Logger.logVerbose(LOG_TAG, "Existing BotWASession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
            else
                Logger.logVerbose(LOG_TAG, "No existing BotWASession with \"" + executionCommand.shellName + "\" shell name found for shell create mode \"" + shellCreateMode.getMode() + "\"");
        }

        if (newBotWASession == null)
            newBotWASession = createBotWASession(executionCommand);
        if (newBotWASession == null) return;

        handleSessionAction(DataUtils.getIntFromString(executionCommand.sessionAction,
            TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY),
            newBotWASession.getTerminalSession());
    }

    /**
     * Create a {@link BotWASession}.
     * Currently called by {@link BotWATerminalSessionActivityClient#addNewSession(boolean, String)} to add a new {@link BotWASession}.
     */
    @Nullable
    public BotWASession createBotWASession(String executablePath, String[] arguments, String stdin,
                                             String workingDirectory, boolean isFailSafe, String sessionName) {
        ExecutionCommand executionCommand = new ExecutionCommand(BotWAShellManager.getNextShellId(),
            executablePath, arguments, stdin, workingDirectory, Runner.TERMINAL_SESSION.getName(), isFailSafe);
        executionCommand.shellName = sessionName;
        return createBotWASession(executionCommand);
    }

    /** Create a {@link BotWASession}. */
    @Nullable
    public synchronized BotWASession createBotWASession(ExecutionCommand executionCommand) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" BotWASession");

        if (!Runner.TERMINAL_SESSION.equalsRunner(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignoring wrong runner \"" + executionCommand.runner + "\" command passed to createBotWASession()");
            return null;
        }

        executionCommand.setShellCommandShellEnvironment = true;
        executionCommand.terminalTranscriptRows = mProperties.getTerminalTranscriptRows();

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        // If the execution command was started for a plugin, only then will the stdout be set
        // Otherwise if command was manually started by the user like by adding a new terminal session,
        // then no need to set stdout
        BotWASession newBotWASession = BotWASession.execute(this, executionCommand, getBotWATerminalSessionClient(),
            this, new BotWAShellEnvironment(), null, executionCommand.isPluginExecutionCommand);
        if (newBotWASession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new BotWASession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            // If the execution command was started for a plugin, then process the error
            if (executionCommand.isPluginExecutionCommand)
                BotWAPluginUtils.processPluginExecutionCommandError(this, LOG_TAG, executionCommand, false);
            else {
                Logger.logError(LOG_TAG, "Set log level to debug or higher to see error in logs");
                Logger.logErrorPrivateExtended(LOG_TAG, executionCommand.toString());
            }
            return null;
        }

        mShellManager.mBotWASessions.add(newBotWASession);

        // Remove the execution command from the pending plugin execution commands list since it has
        // now been processed
        if (executionCommand.isPluginExecutionCommand)
            mShellManager.mPendingPluginExecutionCommands.remove(executionCommand);

        // Notify {@link BotWASessionsListViewController} that sessions list has been updated if
        // activity in is foreground
        if (mBotWATerminalSessionActivityClient != null)
            mBotWATerminalSessionActivityClient.botwaSessionListNotifyUpdated();

        updateNotification();

        // No need to recreate the activity since it likely just started and theme should already have applied
        BotWAActivity.updateBotWAActivityStyling(this, false);

        return newBotWASession;
    }

    /** Remove a BotWASession. */
    public synchronized int removeBotWASession(TerminalSession sessionToRemove) {
        int index = getIndexOfSession(sessionToRemove);

        if (index >= 0)
            mShellManager.mBotWASessions.get(index).finish();

        return index;
    }

    /** Callback received when a {@link BotWASession} finishes. */
    @Override
    public void onBotWASessionExited(final BotWASession botwaSession) {
        if (botwaSession != null) {
            ExecutionCommand executionCommand = botwaSession.getExecutionCommand();

            Logger.logVerbose(LOG_TAG, "The onBotWASessionExited() callback called for \"" + executionCommand.getCommandIdAndLabelLogString() + "\" BotWASession command");

            // If the execution command was started for a plugin, then process the results
            if (executionCommand != null && executionCommand.isPluginExecutionCommand)
                BotWAPluginUtils.processPluginExecutionCommandResult(this, LOG_TAG, executionCommand);

            mShellManager.mBotWASessions.remove(botwaSession);

            // Notify {@link BotWASessionsListViewController} that sessions list has been updated if
            // activity in is foreground
            if (mBotWATerminalSessionActivityClient != null)
                mBotWATerminalSessionActivityClient.botwaSessionListNotifyUpdated();
        }

        updateNotification();
    }





    private ShellCreateMode processShellCreateMode(@NonNull ExecutionCommand executionCommand) {
        if (ShellCreateMode.ALWAYS.equalsMode(executionCommand.shellCreateMode))
            return ShellCreateMode.ALWAYS; // Default
        else if (ShellCreateMode.NO_SHELL_WITH_NAME.equalsMode(executionCommand.shellCreateMode))
            if (DataUtils.isNullOrEmpty(executionCommand.shellName)) {
                BotWAPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                    getString(R.string.error_botwa_service_execution_command_shell_name_unset, executionCommand.shellCreateMode));
                return null;
            } else {
               return ShellCreateMode.NO_SHELL_WITH_NAME;
            }
        else {
            BotWAPluginUtils.setAndProcessPluginExecutionCommandError(this, LOG_TAG, executionCommand, false,
                getString(R.string.error_botwa_service_unsupported_execution_command_shell_create_mode, executionCommand.shellCreateMode));
            return null;
        }
    }

    /** Process session action for new session. */
    private void handleSessionAction(int sessionAction, TerminalSession newTerminalSession) {
        Logger.logDebug(LOG_TAG, "Processing sessionAction \"" + sessionAction + "\" for session \"" + newTerminalSession.mSessionName + "\"");

        switch (sessionAction) {
            case TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mBotWATerminalSessionActivityClient != null)
                    mBotWATerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                startBotWAActivity();
                break;
            case TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY:
                if (getBotWASessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                startBotWAActivity();
                break;
            case TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY:
                setCurrentStoredTerminalSession(newTerminalSession);
                if (mBotWATerminalSessionActivityClient != null)
                    mBotWATerminalSessionActivityClient.setCurrentSession(newTerminalSession);
                break;
            case TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY:
                if (getBotWASessionsSize() == 1)
                    setCurrentStoredTerminalSession(newTerminalSession);
                break;
            default:
                Logger.logError(LOG_TAG, "Invalid sessionAction: \"" + sessionAction + "\". Force using default sessionAction.");
                handleSessionAction(TERMUX_SERVICE.VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY, newTerminalSession);
                break;
        }
    }

    /** Launch the {@link }BotWAActivity} to bring it to foreground. */
    private void startBotWAActivity() {
        // For android >= 10, apps require Display over other apps permission to start foreground activities
        // from background (services). If it is not granted, then BotWASessions that are started will
        // show in BotWA notification but will not run until user manually clicks the notification.
        if (PermissionUtils.validateDisplayOverOtherAppsPermissionForPostAndroid10(this, true)) {
            BotWAActivity.startBotWAActivity(this);
        } else {
            BotWAAppSharedPreferences preferences = BotWAAppSharedPreferences.build(this);
            if (preferences == null) return;
            if (preferences.arePluginErrorNotificationsEnabled(false))
                Logger.showToast(this, this.getString(R.string.error_display_over_other_apps_permission_not_granted_to_start_terminal), true);
        }
    }





    /** If {@link BotWAActivity} has not bound to the {@link BotWAService} yet or is destroyed, then
     * interface functions requiring the activity should not be available to the terminal sessions,
     * so we just return the {@link #mBotWATerminalSessionServiceClient}. Once {@link BotWAActivity} bind
     * callback is received, it should call {@link #setBotWATerminalSessionClient} to set the
     * {@link BotWAService#mBotWATerminalSessionActivityClient} so that further terminal sessions are directly
     * passed the {@link BotWATerminalSessionActivityClient} object which fully implements the
     * {@link TerminalSessionClient} interface.
     *
     * @return Returns the {@link BotWATerminalSessionActivityClient} if {@link BotWAActivity} has bound with
     * {@link BotWAService}, otherwise {@link BotWATerminalSessionServiceClient}.
     */
    public synchronized BotWATerminalSessionClientBase getBotWATerminalSessionClient() {
        if (mBotWATerminalSessionActivityClient != null)
            return mBotWATerminalSessionActivityClient;
        else
            return mBotWATerminalSessionServiceClient;
    }

    /** This should be called when {@link BotWAActivity#onServiceConnected} is called to set the
     * {@link BotWAService#mBotWATerminalSessionActivityClient} variable and update the {@link TerminalSession}
     * and {@link TerminalEmulator} clients in case they were passed {@link BotWATerminalSessionServiceClient}
     * earlier.
     *
     * @param botwaTerminalSessionActivityClient The {@link BotWATerminalSessionActivityClient} object that fully
     * implements the {@link TerminalSessionClient} interface.
     */
    public synchronized void setBotWATerminalSessionClient(BotWATerminalSessionActivityClient botwaTerminalSessionActivityClient) {
        mBotWATerminalSessionActivityClient = botwaTerminalSessionActivityClient;

        for (int i = 0; i < mShellManager.mBotWASessions.size(); i++)
            mShellManager.mBotWASessions.get(i).getTerminalSession().updateTerminalSessionClient(mBotWATerminalSessionActivityClient);
    }

    /** This should be called when {@link BotWAActivity} has been destroyed and in {@link #onUnbind(Intent)}
     * so that the {@link BotWAService} and {@link TerminalSession} and {@link TerminalEmulator}
     * clients do not hold an activity references.
     */
    public synchronized void unsetBotWATerminalSessionClient() {
        for (int i = 0; i < mShellManager.mBotWASessions.size(); i++)
            mShellManager.mBotWASessions.get(i).getTerminalSession().updateTerminalSessionClient(mBotWATerminalSessionServiceClient);

        mBotWATerminalSessionActivityClient = null;
    }





    private Notification buildNotification() {
        Resources res = getResources();

        // Set pending intent to be launched when notification is clicked
        Intent notificationIntent = BotWAActivity.newInstance(this);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);


        // Set notification text
        int sessionCount = getBotWASessionsSize();
        int taskCount = mShellManager.mBotWATasks.size();
        String notificationText = sessionCount + " session" + (sessionCount == 1 ? "" : "s");
        if (taskCount > 0) {
            notificationText += ", " + taskCount + " task" + (taskCount == 1 ? "" : "s");
        }

        final boolean wakeLockHeld = mWakeLock != null;
        if (wakeLockHeld) notificationText += " (wake lock held)";


        // Set notification priority
        // If holding a wake or wifi lock consider the notification of high priority since it's using power,
        // otherwise use a low priority
        int priority = (wakeLockHeld) ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW;


        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
            BotWAConstants.TERMUX_APP_NOTIFICATION_CHANNEL_ID, priority,
            BotWAConstants.TERMUX_APP_NAME, notificationText, null,
            contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.drawable.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF607D8B);

        // BotWASessions are always ongoing
        builder.setOngoing(true);


        // Set Exit button action
        Intent exitIntent = new Intent(this, BotWAService.class).setAction(TERMUX_SERVICE.ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.notification_action_exit), PendingIntent.getService(this, 0, exitIntent, 0));


        // Set Wakelock button actions
        String newWakeAction = wakeLockHeld ? TERMUX_SERVICE.ACTION_WAKE_UNLOCK : TERMUX_SERVICE.ACTION_WAKE_LOCK;
        Intent toggleWakeLockIntent = new Intent(this, BotWAService.class).setAction(newWakeAction);
        String actionTitle = res.getString(wakeLockHeld ? R.string.notification_action_wake_unlock : R.string.notification_action_wake_lock);
        int actionIcon = wakeLockHeld ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_lock_lock;
        builder.addAction(actionIcon, actionTitle, PendingIntent.getService(this, 0, toggleWakeLockIntent, 0));


        return builder.build();
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, BotWAConstants.TERMUX_APP_NOTIFICATION_CHANNEL_ID,
            BotWAConstants.TERMUX_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    /** Update the shown foreground service notification after making any changes that affect it. */
    private synchronized void updateNotification() {
        if (mWakeLock == null && mShellManager.mBotWASessions.isEmpty() && mShellManager.mBotWATasks.isEmpty()) {
            // Exit if we are updating after the user disabled all locks with no sessions or tasks running.
            requestStopService();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(BotWAConstants.TERMUX_APP_NOTIFICATION_ID, buildNotification());
        }
    }





    private void setCurrentStoredTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return;
        // Make the newly created session the current one to be displayed
        BotWAAppSharedPreferences preferences = BotWAAppSharedPreferences.build(this);
        if (preferences == null) return;
        preferences.setCurrentSession(terminalSession.mHandle);
    }

    public synchronized boolean isBotWASessionsEmpty() {
        return mShellManager.mBotWASessions.isEmpty();
    }

    public synchronized int getBotWASessionsSize() {
        return mShellManager.mBotWASessions.size();
    }

    public synchronized List<BotWASession> getBotWASessions() {
        return mShellManager.mBotWASessions;
    }

    @Nullable
    public synchronized BotWASession getBotWASession(int index) {
        if (index >= 0 && index < mShellManager.mBotWASessions.size())
            return mShellManager.mBotWASessions.get(index);
        else
            return null;
    }

    @Nullable
    public synchronized BotWASession getBotWASessionForTerminalSession(TerminalSession terminalSession) {
        if (terminalSession == null) return null;

        for (int i = 0; i < mShellManager.mBotWASessions.size(); i++) {
            if (mShellManager.mBotWASessions.get(i).getTerminalSession().equals(terminalSession))
                return mShellManager.mBotWASessions.get(i);
        }

        return null;
    }

    public synchronized BotWASession getLastBotWASession() {
        return mShellManager.mBotWASessions.isEmpty() ? null : mShellManager.mBotWASessions.get(mShellManager.mBotWASessions.size() - 1);
    }

    public synchronized int getIndexOfSession(TerminalSession terminalSession) {
        if (terminalSession == null) return -1;

        for (int i = 0; i < mShellManager.mBotWASessions.size(); i++) {
            if (mShellManager.mBotWASessions.get(i).getTerminalSession().equals(terminalSession))
                return i;
        }
        return -1;
    }

    public synchronized TerminalSession getTerminalSessionForHandle(String sessionHandle) {
        TerminalSession terminalSession;
        for (int i = 0, len = mShellManager.mBotWASessions.size(); i < len; i++) {
            terminalSession = mShellManager.mBotWASessions.get(i).getTerminalSession();
            if (terminalSession.mHandle.equals(sessionHandle))
                return terminalSession;
        }
        return null;
    }

    public synchronized AppShell getBotWATaskForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        AppShell appShell;
        for (int i = 0, len = mShellManager.mBotWATasks.size(); i < len; i++) {
            appShell = mShellManager.mBotWATasks.get(i);
            String shellName = appShell.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return appShell;
        }
        return null;
    }

    public synchronized BotWASession getBotWASessionForShellName(String name) {
        if (DataUtils.isNullOrEmpty(name)) return null;
        BotWASession botwaSession;
        for (int i = 0, len = mShellManager.mBotWASessions.size(); i < len; i++) {
            botwaSession = mShellManager.mBotWASessions.get(i);
            String shellName = botwaSession.getExecutionCommand().shellName;
            if (shellName != null && shellName.equals(name))
                return botwaSession;
        }
        return null;
    }



    public boolean wantsToStop() {
        return mWantsToStop;
    }

}
