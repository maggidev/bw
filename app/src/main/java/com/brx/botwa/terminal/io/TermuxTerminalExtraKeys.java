package com.brx.botwa.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.brx.botwa.app.BotWAActivity;
import com.brx.botwa.app.terminal.BotWATerminalSessionActivityClient;
import com.brx.botwa.app.terminal.BotWATerminalViewClient;
import com.brx.botwa.shared.logger.Logger;
import com.brx.botwa.shared.botwa.extrakeys.ExtraKeysConstants;
import com.brx.botwa.shared.botwa.extrakeys.ExtraKeysInfo;
import com.brx.botwa.shared.botwa.settings.properties.BotWAPropertyConstants;
import com.brx.botwa.shared.botwa.settings.properties.BotWASharedProperties;
import com.brx.botwa.shared.botwa.terminal.io.TerminalExtraKeys;
import com.brx.botwa.view.TerminalView;

import org.json.JSONException;

public class BotWATerminalExtraKeys extends TerminalExtraKeys {

    private ExtraKeysInfo mExtraKeysInfo;

    final BotWAActivity mActivity;
    final BotWATerminalViewClient mBotWATerminalViewClient;
    final BotWATerminalSessionActivityClient mBotWATerminalSessionActivityClient;

    private static final String LOG_TAG = "BotWATerminalExtraKeys";

    public BotWATerminalExtraKeys(BotWAActivity activity, @NonNull TerminalView terminalView,
                                   BotWATerminalViewClient botwaTerminalViewClient,
                                   BotWATerminalSessionActivityClient botwaTerminalSessionActivityClient) {
        super(terminalView);

        mActivity = activity;
        mBotWATerminalViewClient = botwaTerminalViewClient;
        mBotWATerminalSessionActivityClient = botwaTerminalSessionActivityClient;

        setExtraKeys();
    }


    /**
     * Set the terminal extra keys and style.
     */
    private void setExtraKeys() {
        mExtraKeysInfo = null;

        try {
            // The mMap stores the extra key and style string values while loading properties
            // Check {@link #getExtraKeysInternalPropertyValueFromValue(String)} and
            // {@link #getExtraKeysStyleInternalPropertyValueFromValue(String)}
            String extrakeys = (String) mActivity.getProperties().getInternalPropertyValue(BotWAPropertyConstants.KEY_EXTRA_KEYS, true);
            String extraKeysStyle = (String) mActivity.getProperties().getInternalPropertyValue(BotWAPropertyConstants.KEY_EXTRA_KEYS_STYLE, true);

            ExtraKeysConstants.ExtraKeyDisplayMap extraKeyDisplayMap = ExtraKeysInfo.getCharDisplayMapForStyle(extraKeysStyle);
            if (ExtraKeysConstants.EXTRA_KEY_DISPLAY_MAPS.DEFAULT_CHAR_DISPLAY.equals(extraKeyDisplayMap) && !BotWAPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE.equals(extraKeysStyle)) {
                Logger.logError(BotWASharedProperties.LOG_TAG, "The style \"" + extraKeysStyle + "\" for the key \"" + BotWAPropertyConstants.KEY_EXTRA_KEYS_STYLE + "\" is invalid. Using default style instead.");
                extraKeysStyle = BotWAPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE;
            }

            mExtraKeysInfo = new ExtraKeysInfo(extrakeys, extraKeysStyle, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
        } catch (JSONException e) {
            Logger.showToast(mActivity, "Could not load and set the \"" + BotWAPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: " + e.toString(), true);
            Logger.logStackTraceWithMessage(LOG_TAG, "Could not load and set the \"" + BotWAPropertyConstants.KEY_EXTRA_KEYS + "\" property from the properties file: ", e);

            try {
                mExtraKeysInfo = new ExtraKeysInfo(BotWAPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS, BotWAPropertyConstants.DEFAULT_IVALUE_EXTRA_KEYS_STYLE, ExtraKeysConstants.CONTROL_CHARS_ALIASES);
            } catch (JSONException e2) {
                Logger.showToast(mActivity, "Can't create default extra keys",true);
                Logger.logStackTraceWithMessage(LOG_TAG, "Could create default extra keys: ", e);
                mExtraKeysInfo = null;
            }
        }
    }

    public ExtraKeysInfo getExtraKeysInfo() {
        return mExtraKeysInfo;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mBotWATerminalViewClient != null)
                mBotWATerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mBotWATerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mBotWATerminalSessionActivityClient != null)
                mBotWATerminalSessionActivityClient.onPasteTextFromClipboard(null);
        }  else if ("SCROLL".equals(key)) {
            TerminalView terminalView = mBotWATerminalViewClient.getActivity().getTerminalView();
            if (terminalView != null && terminalView.mEmulator != null)
                terminalView.mEmulator.toggleAutoScrollDisabled();
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
