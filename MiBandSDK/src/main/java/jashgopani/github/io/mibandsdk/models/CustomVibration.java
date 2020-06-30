package jashgopani.github.io.mibandsdk.models;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;


public class CustomVibration implements Runnable {
    final public static int MAX_ON = 1000;
    final public static int MAX_OFF = 1000;
    final public static int MIN_ON = 100;
    final public static int MIN_OFF = 25;
    final public static int MAX_REPEAT = 10;
    final public static int MIN_REPEAT = 2;
    private static final String TAG = "CustomVibration";
    private int onTime, offTime, repeat;
    private byte[] protocol;
    private int ROUND = 5;
    private boolean isCustomPattern;
    private String defaultPattern;
    private int[] customPattern;

    public CustomVibration(int on, int off, int repeat) {
        setOnTime(onTime);
        setOffTime(offTime);
        setRepeat(repeat);
        setCustomPattern(false);
    }

    public CustomVibration() {
        onTime = 350;
        offTime = 250;
        repeat = 2;
        protocol = Protocol.VIBRATION_WITHOUT_LED;
        setCustomPattern(false);
    }

    public CustomVibration(String pattern) {

        protocol = Protocol.VIBRATION_WITHOUT_LED;
        setCustomPattern(true);

        if (pattern == null || pattern.length() == 0) {
            customPattern = new int[]{350, 250, 150, 150, 350, 100, 250, 100, 250, 100, 95};
//                customPattern = new int[]{100,150,100,150,100,150,100};
        } else {
            String[] split = pattern.split(",");
            //if last value is off-time then it is useless
            int patternLength = split.length % 2 == 0 ? split.length : (split.length - 1);
            customPattern = new int[patternLength];
            for (int i = 0; i < patternLength; i++) {
                customPattern[i] = Integer.parseInt(split[i]);
            }
        }
    }

    public CustomVibration(int onTime, int offTime, int repeat, byte[] protocol) {
        this.onTime = onTime;
        this.offTime = offTime;
        this.repeat = repeat;
        this.protocol = protocol;
    }

    public int getOnTime() {
        return onTime;
    }

    public void setOnTime(int onTime) {
        onTime = roundTo(onTime, ROUND);
        //between minOFF and maxOFF
        this.onTime = Math.min(Math.max(onTime, MIN_ON), MAX_ON);
    }

    public int getOffTime() {
        return offTime;
    }

    public void setOffTime(int offTime) {
        offTime = roundTo(offTime, ROUND);
        //between minOFF and maxOFF
        this.offTime = Math.min(Math.max(offTime, MIN_OFF), MAX_OFF);
    }

    public int getRepeat() {
        return repeat;
    }

    public void setRepeat(int repeat) {
        //between minOFF and maxOFF
        this.repeat = Math.min(Math.max(repeat, MIN_REPEAT), MAX_REPEAT);
    }

    public byte[] getProtocol() {
        return protocol;
    }

    public void setProtocol(byte[] protocol) {
        this.protocol = protocol;
    }


    public boolean isCustomPattern() {
        return isCustomPattern;
    }

    private void setCustomPattern(boolean customPattern) {
        this.isCustomPattern = customPattern;
    }


    public boolean isLedEnabled() {
        return !Protocol.VIBRATION_WITHOUT_LED.equals(this.getProtocol());
    }

    private int roundTo(int i, int r) {
        if (r == 0) return 0;
        return r * (Math.round(i / r));
    }

    private void normalVibrate() {
        Log.d(TAG, "Custom Normal Vibration : " + this);
        for (int i = 0; i < this.getRepeat(); i++) {
            Log.d(TAG, "run: Custom Vibration no : " + i);
            try {
//                writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, this.getProtocol());
                Thread.sleep(this.getOnTime());
//                writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION);
                Thread.sleep(this.getOffTime());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void patternVibrate() {
        Log.d(TAG, "Custom Pattern Vibration : " + this);
        Log.d(TAG, "patternVibrate: " + Arrays.toString(customPattern));
        for (int i = 0; i < customPattern.length - 1; i++) {
            Log.d(TAG, "run: Vibration ON Time : " + customPattern[i]);
            try {
//                writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, this.getProtocol());
                Thread.sleep(customPattern[i]);
//                writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION);
                Thread.sleep(customPattern[i + 1]);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @NonNull
    @Override
    public String toString() {
        String l = this.isLedEnabled() ? "LED_ON" : "LED_OFF";
        return "(" + getOnTime() + "," + getOffTime() + "," + getRepeat() + "," + l + ")";
    }

    @Override
    public void run() {
        if (isCustomPattern()) patternVibrate();
        else normalVibrate();
    }

}
