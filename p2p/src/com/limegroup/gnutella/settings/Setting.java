package com.limegroup.gnutella.settings;

public class Setting<T> {
	/**
	 * The constant key for this property, specified upon construction.
	 */
	protected String KEY;
    protected T data;
	public Setting(String key, T value) {
	    KEY = key;
	    data = value;
	}
    
    /**
     * Set new property value
     * @param value new property value 
     *
     * Note: This is the method used by SimmSettingsManager to load the setting
     * with the value specified by Simpp 
     */
    public void setValue(T value) {
        data = value;
    }

    public T getValue() {
        return data;
    }


}
