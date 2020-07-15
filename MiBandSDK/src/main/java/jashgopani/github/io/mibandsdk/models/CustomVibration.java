package jashgopani.github.io.mibandsdk.models;

import android.drm.DrmErrorEvent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.res.TypedArrayUtils;
import androidx.core.math.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

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
    public static final Integer[] ONCE = new Integer[]{z,x};
    public static final Integer[] DEFAULT = new Integer[]{z,x,z,x};
    public static final Integer[] LEFT_PULSE = new Integer[]{zzz,xx,z,xx,z,xx};
    public static final Integer[] RIGHT_PULSE = new Integer[]{z,xx,z,xx,zzz,xx};
    public static final Integer[] FROWN = new Integer[]{zzz,x,z,x,z,x,zzz,xx};
    public static final Integer[] SMILE = new Integer[]{z,x,zzz,x,zzz,x,z,x};
    public static final Integer[] ARC = new Integer[]{zz,x,zz,x,zz,x,zz,x};
    public static final Integer[] LONG = new Integer[]{zzzz,xx};

    private static int roundTo(int i, int r) {
        r = Math.max(1,r);
        return Math.max(r * (Math.round(i / r)),0);
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
            int patternLength = split.length%2==0?split.length:split.length+1;
            customPattern = new Integer[patternLength];
            for (int i = 0; i < split.length; i++) {
                customPattern[i] = roundTo(Integer.parseInt(split[i]),ROUNDER);
            }
            if(split.length!=patternLength)
            customPattern[patternLength-1] = x;
        }
        Log.d(TAG, "generatePattern: "+Arrays.toString(customPattern));
        return customPattern;
    }

    /**
     * Generate a pattern that vibrates {vibrationCount} times
     * @param vibrationCount The number of times you want to vibrate the band
     * @return Integer[] array which can be passed to miband.vibrate()
     */
    public static final Integer[] generatePattern(int vibrationCount){
        if(vibrationCount<=0)return new Integer[]{};

        Integer res[] = new Integer[vibrationCount*ONCE.length];
        for (int i = 0; i < res.length; i+=ONCE.length) {
            System.arraycopy(ONCE,0,res,i,ONCE.length);
        }
        return res;
    }

    public static final Integer[] generatePattern(int vibrationCount,int vibrationIntensity){
        if(vibrationCount<=0)return new Integer[]{};
        vibrationIntensity = MathUtils.clamp(vibrationIntensity,1,4);
        if(vibrationIntensity == 1)return generatePattern(vibrationCount);

        int delay = x;
        int vibration = z;
        switch (vibrationIntensity){
            case 2:
                vibration = zz;
                break;
            case 3:
                vibration =zzz;
                break;
            case 4:
                vibration = zzzz;
                break;
            default:
                vibration = z;
                delay = x;
        }

        if(vibrationIntensity > 2 ){
            //to overlap of vibrations
            delay = xx;
        }

        Integer res[] = new Integer[vibrationCount*2];
        for (int i = 0; i < res.length; i+=ONCE.length) {
            res[i] = vibration;
            res[i+1] = delay;
        }
        return res;
    }


}
