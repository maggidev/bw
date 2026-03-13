package com.brx.botwa.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.brx.botwa.R;
import com.brx.botwa.shared.botwa.settings.preferences.BotWAWidgetAppSharedPreferences;

@Keep
public class BotWAWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(BotWAWidgetPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.botwa_widget_preferences, rootKey);
    }

}

class BotWAWidgetPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final BotWAWidgetAppSharedPreferences mPreferences;

    private static BotWAWidgetPreferencesDataStore mInstance;

    private BotWAWidgetPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = BotWAWidgetAppSharedPreferences.build(context, true);
    }

    public static synchronized BotWAWidgetPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new BotWAWidgetPreferencesDataStore(context);
        }
        return mInstance;
    }

}
