package com.brx.botwa.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.brx.botwa.R;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAAppSharedPreferences;

@Keep
public class BotWAPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(BotWAPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.botwa_preferences, rootKey);
    }

}

class BotWAPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final BotWAAppSharedPreferences mPreferences;

    private static BotWAPreferencesDataStore mInstance;

    private BotWAPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = BotWAAppSharedPreferences.build(context, true);
    }

    public static synchronized BotWAPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BotWAPreferencesDataStore(context);
        }
        return mInstance;
    }

}
