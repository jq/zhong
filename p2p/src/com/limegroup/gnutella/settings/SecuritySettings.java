package com.limegroup.gnutella.settings;

import java.io.File;

import com.limegroup.gnutella.util.CommonUtils;

/**
 * Settings for security
 */
public class SecuritySettings {
    /**
     * Name of the file that stores cookies
     */
    public static final String COOKIES_FILE =  CommonUtils.getUserSettingsDir() + File.separator + "Cookies.dat";
}
