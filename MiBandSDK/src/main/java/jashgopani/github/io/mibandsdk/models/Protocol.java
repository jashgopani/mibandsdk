package jashgopani.github.io.mibandsdk.models;

/**
 *These bytes are written/read to/from the band based on the action we want to perform
 * Writing a correct combination of Service UUID, Characteristic UUID and bytes can perform specific task
 * All UUIDs are mentioned in Profile class
 */

public class Protocol {
    public static final byte[] PAIR = new byte[]{2};
    public static final byte[] VIBRATION_WITH_LED = new byte[]{1};
    public static final byte[] VIBRATION_10_TIMES_WITH_LED = new byte[]{2};
    public static final byte[] VIBRATION_WITHOUT_LED = new byte[]{4};
    public static final byte[] STOP_VIBRATION = new byte[]{0};
    public static final byte[] ENABLE_REALTIME_STEPS_NOTIFY = new byte[]{3, 1};
    public static final byte[] DISABLE_REALTIME_STEPS_NOTIFY = new byte[]{3, 0};
    public static final byte[] ENABLE_SENSOR_DATA_NOTIFY = new byte[]{18, 1};
    public static final byte[] DISABLE_SENSOR_DATA_NOTIFY = new byte[]{18, 0};
    public static final byte[] SET_COLOR_RED = new byte[]{14, 6, 1, 2, 1};
    public static final byte[] SET_COLOR_BLUE = new byte[]{14, 0, 6, 6, 1};
    public static final byte[] SET_COLOR_ORANGE = new byte[]{14, 6, 2, 0, 1};
    public static final byte[] SET_COLOR_GREEN = new byte[]{14, 4, 5, 0, 1};
    public static final byte[] START_HEART_RATE_SCAN = new byte[]{21, 2, 1};
    public static final byte[] REBOOT = new byte[]{12};
    public static final byte[] REMOTE_DISCONNECT = new byte[]{1};
    public static final byte[] FACTORY_RESET = new byte[]{9};
    public static final byte[] SELF_TEST = new byte[]{2};
}

