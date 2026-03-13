package com.brx.botwa.shared.botwa.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.brx.botwa.shared.botwa.BotWAConstants;

public class BotWAAppSharedProperties extends BotWASharedProperties {

    private static BotWAAppSharedProperties properties;


    private BotWAAppSharedProperties(@NonNull Context context) {
        super(context, BotWAConstants.TERMUX_APP_NAME,
            BotWAConstants.TERMUX_PROPERTIES_FILE_PATHS_LIST, BotWAPropertyConstants.TERMUX_APP_PROPERTIES_LIST,
            new BotWASharedProperties.SharedPropertiesParserClient());
    }

    /**
     * Initialize the {@link #properties} and load properties from disk.
     *
     * @param context The {@link Context} for operations.
     * @return Returns the {@link BotWAAppSharedProperties}.
     */
    public static BotWAAppSharedProperties init(@NonNull Context context) {
        if (properties == null)
            properties = new BotWAAppSharedProperties(context);

        return properties;
    }

    /**
     * Get the {@link #properties}.
     *
     * @return Returns the {@link BotWAAppSharedProperties}.
     */
    public static BotWAAppSharedProperties getProperties() {
        return properties;
    }

}
