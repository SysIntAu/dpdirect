package org.dpdirect.dpmgmt;

public class Defaults {

    /**
     * Default firmware level - determines SOMA and AMP version.
     */
    public static int DEFAULT_FIRMWARE_LEVEL = 5;

    /**
     * Default waitTime - determines wait time when polling for a result.
     */
    public static final int DEFAULT_WAIT_TIME_SECONDS = 30;
    /**
     * A default value for the log poll interval as a value in milliseconds.
     */
    public static final int DEFAULT_POLL_INT_MILLIS = 2000;

    private Defaults() { }
}
