package com.brx.botwa.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.brx.botwa.R;
import com.brx.botwa.shared.botwa.settings.preferences.BotWATaskerAppSharedPreferences;

@Keep
public class BotWATaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(BotWATaskerPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.botwa_tasker_preferences, rootKey);
    }

}

class BotWATaskerPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final BotWATaskerAppSharedPreferences mPreferences;

    private static BotWATaskerPreferencesDataStore mInstance;

    private BotWATaskerPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = BotWATaskerAppSharedPreferences.build(context, true);
    }

    public static synchronized BotWATaskerPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BotWATaskerPreferencesDataStore(context);
        }
        return mInstance;
    }

}
