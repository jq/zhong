package com.limegroup.gnutella.settings;

/**
 * Settings for LimeWire application
 */
public class ApplicationSettings {
    /**
	 * The language to use for the application. // Locale.getDefault() in android, we use fix string for test
	 */
    public static final String LANGUAGE = "en_US";
    
    /**
	 * The country to use for the application.
	 */
    public static final String COUNTRY = "US";
    
    /**
     * the default locale to use if not specified
     * used to set the locale for connections which don't have X_LOCALE_PREF
     * header or pings and pongs that don't advertise locale preferences.
     */
    public static final String DEFAULT_LOCALE = "en";
        

    
    /**
     * Gets the current language setting.
     */
    public static String getLanguage() {
        return LANGUAGE;
    }
}
