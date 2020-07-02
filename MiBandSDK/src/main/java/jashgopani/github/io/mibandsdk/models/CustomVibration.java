package jashgopani.github.io.mibandsdk.models;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Arrays;

import jashgopani.github.io.mibandsdk.BluetoothIO;
import jashgopani.github.io.mibandsdk.MiBand;

/**
 * This class is a Utility class for generating Custom Vibration Patterns for passing to {#Miband.startVibration()}
 */
public class CustomVibration {
    private static final String TAG = "CustomVibration";
    private static final int ROUNDER = 5;

    //for vibration
    private static final int zzzz = 600;
    private static final int zzz = 500;
    private static final int zz = 200;
    private static final int z = 100;
    //for silent or pause
    private static final int X = 1000;
    private static final int xxxx = 600;
    private static final int xxx = 500;
    private static final int xx = 200;
    private static final int x = 100;

    /**
     * Vibration Patterns
     */
    public static final Integer[] DEFAULT = new Integer[]{z,x,z,x};
    public static final Integer[] LEFT_PULSE = new Integer[]{zzz,xx,z,xx,z,xx};
    public static final Integer[] RIGHT_PULSE = new Integer[]{z,xx,z,xx,zzz,xx};
    public static final Integer[] FROWN = new Integer[]{zzz,x,z,x,z,zzz,x};
    public static final Integer[] SMILE = new Integer[]{z,x,zzz,x,zzz,x,z,x};

    private static int roundTo(int i, int r) {
        if (r == 0) return 0;
        return r * (Math.round(i / r));
    }

    /**
     * Generate a repetative vibration pattern
     * @param vibrationOnDuration The duration in milliseconds for which the band vibrates
     * @param vibrationOffDuration The duration in milliseconds between two consecutive vibrations
     * @param repeat Number of times the vibration repeats
     * @return Integer Array representaing vibration pattern
     */
    public static final Integer[] generatePattern(int vibrationOnDuration,int vibrationOffDuration,int repeat){
        int plen = repeat*2;
        Integer customPattern[] = new Integer[plen];
        for (int i = 0; i < plen-1; i+=2) {
            Log.d(TAG, "vibrateBand: @"+i);
            customPattern[i] = roundTo(vibrationOnDuration,ROUNDER);
            if(i==plen-1)
                customPattern[i+1] = 0;
            else
                customPattern[i+1] = roundTo(vibrationOffDuration,ROUNDER);
        }
        Log.d(TAG, "generatePattern: "+Arrays.toString(customPattern));
        return customPattern;
    }

    /**
     * Generate a CustomVibration pattern {vibrateOn,vibrateOff,...} from String where vibrateOn and vibrateOff are in milliseconds
     * The band vibrates for "vibrateOn" milliseconds and waits for "vibrateOff" miliseconds and then repeats until the pattern lasts
     * @param pattern The pattern which consists of on and off values
     * @param delimiter Regex of the delimiter used to split the string. Default is ","
     * @return Integer Array representing vibration pattern
     */
    public static final Integer[] generatePattern(String pattern,String delimiter){
        Integer []customPattern;
        pattern = pattern.trim();

        if (pattern == null || pattern.length() == 0) {
            customPattern = new Integer[]{};
        } else {
            String[] split = pattern.split(delimiter==null?",":delimiter);
            int patternLength = split.length;
            customPattern = new Integer[patternLength];
            for (int i = 0; i < patternLength; i++) {
                customPattern[i] = roundTo(Integer.parseInt(split[i]),ROUNDER);
            }
        }
        Log.d(TAG, "generatePattern: "+Arrays.toString(customPattern));
        return customPattern;
    }

    public static final Integer[] generatePattern(int repeat){
        //TODO : implement
        return null;
    }

}
