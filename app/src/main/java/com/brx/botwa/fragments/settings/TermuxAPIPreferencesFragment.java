package com.brx.botwa.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.brx.botwa.R;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAAPIAppSharedPreferences;

@Keep
public class BotWAAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(BotWAAPIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.botwa_api_preferences, rootKey);
    }

}

class BotWAAPIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final BotWAAPIAppSharedPreferences mPreferences;

    private static BotWAAPIPreferencesDataStore mInstance;

    private BotWAAPIPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = BotWAAPIAppSharedPreferences.build(context, true);
    }

    public static synchronized BotWAAPIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BotWAAPIPreferencesDataStore(context);
        }
        return mInstance;
    }

}
