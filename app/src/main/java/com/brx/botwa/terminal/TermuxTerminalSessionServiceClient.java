package com.brx.botwa.app.terminal;

import android.app.Service;

import androidx.annotation.NonNull;

import com.brx.botwa.app.BotWAService;
import com.brx.botwa.shared.botwa.shell.command.runner.terminal.BotWASession;
import com.brx.botwa.shared.botwa.terminal.BotWATerminalSessionClientBase;
import com.brx.botwa.terminal.TerminalSession;
import com.brx.botwa.terminal.TerminalSessionClient;

/** The {@link TerminalSessionClient} implementation that may require a {@link Service} for its interface methods. */
public class BotWATerminalSessionServiceClient extends BotWATerminalSessionClientBase {

    private static final String LOG_TAG = "BotWATerminalSessionServiceClient";

    private final BotWAService mService;

    public BotWATerminalSessionServiceClient(BotWAService service) {
        this.mService = service;
    }

    @Override
    public void setTerminalShellPid(@NonNull TerminalSession terminalSession, int pid) {
        BotWASession botwaSession = mService.getBotWASessionForTerminalSession(terminalSession);
        if (botwaSession != null)
            botwaSession.getExecutionCommand().mPid = pid;
    }

}
