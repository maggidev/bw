package com.brx.botwa.shared.botwa.theme;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.brx.botwa.shared.botwa.settings.properties.BotWAPropertyConstants;
import com.brx.botwa.shared.botwa.settings.properties.BotWASharedProperties;
import com.brx.botwa.shared.theme.NightMode;

public class BotWAThemeUtils {

    /** Get the {@link BotWAPropertyConstants#KEY_NIGHT_MODE} value from the properties file on disk
     * and set it to app wide night mode value. */
    public static void setAppNightMode(@NonNull Context context) {
        NightMode.setAppNightMode(BotWASharedProperties.getNightMode(context));
    }

    /** Set name as app wide night mode value. */
    public static void setAppNightMode(@Nullable String name) {
        NightMode.setAppNightMode(name);
    }

}
