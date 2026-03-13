package com.brx.botwa.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.brx.botwa.R;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAFloatAppSharedPreferences;

@Keep
public class BotWAFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(BotWAFloatPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.botwa_float_preferences, rootKey);
    }

}

class BotWAFloatPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final BotWAFloatAppSharedPreferences mPreferences;

    private static BotWAFloatPreferencesDataStore mInstance;

    private BotWAFloatPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = BotWAFloatAppSharedPreferences.build(context, true);
    }

    public static synchronized BotWAFloatPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BotWAFloatPreferencesDataStore(context);
        }
        return mInstance;
    }

}
