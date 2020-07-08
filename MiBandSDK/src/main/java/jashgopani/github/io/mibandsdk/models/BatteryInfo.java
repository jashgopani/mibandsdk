package jashgopani.github.io.mibandsdk.models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class BatteryInfo {
    private final int level;
    private final int cycles;
    private final BatteryInfo.Status status;
    private final Calendar lastChargedDate;

    /**
     * Private Constructor
     * @param level Current Battery level
     * @param cycles Cycles of battery
     * @param status Current status (refer to enum)
     * @param lastChargedDate Last charged date
     */
    private BatteryInfo(int level, int cycles, BatteryInfo.Status status, Calendar lastChargedDate) {
        this.level = level;
        this.cycles = cycles;
        this.status = status;
        this.lastChargedDate = lastChargedDate;
    }

    /**
     * Get current battery level of the Miband(0-100)
     * @return
     */
    public int getLevel() {
        return level;
    }

    /**
     * Get the number of battery cycles of the Miband connected
     * @return
     */
    public int getCycles() {
        return cycles;
    }

    /**
     * Get the current status of battery i.e Charging,Discharging,Full,Low etc
     * @return
     */
    public String getStatus() {
        return status.toString();
    }

    /**
     * Get Last charged date of your Miband.
     * @return
     */
    public Calendar getLastChargedDate() {
        return lastChargedDate;
    }

    public String getLastChargedDate(String pattern){
        if(pattern==null || pattern.trim().length()==0){
            pattern = new String("yyyy-MM-dd HH:mm:SS");
        }
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault());
        String formattedDate = formatter.format(this.lastChargedDate.getTime());
        return formattedDate;
    }

    /**
     * Get a battery Info object to retrieve details from
     * @param data
     * @return
     */
    public static BatteryInfo fromByteData(byte[] data){
        int level = data[0];
        Status status = fromBytes(data[9]);
        int cycles = '\uffff' & (255 & data[7] | (255 & data[8]) << 8);
        Calendar lastChargeDay = Calendar.getInstance();
        lastChargeDay.set(1, data[1] + 2000);
        lastChargeDay.set(2, data[2]);
        lastChargeDay.set(5, data[3]);
        lastChargeDay.set(11, data[4]);
        lastChargeDay.set(12, data[5]);
        lastChargeDay.set(13, data[6]);
        return new BatteryInfo(level, cycles, status, lastChargeDay);
    }

    /**
     * For printing in strings
     * @return
     */
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS", Locale.getDefault());
        String formattedDate = formatter.format(this.lastChargedDate.getTime());
        return "cycles:" + this.cycles + ",level:" + this.level + ",status:" + this.status + ",last:" + formattedDate;
    }

    /**
     * For various states of battery
     */
    enum Status{
        UNKNOWN,LOW,FULL,CHARGING,NOT_CHARGING;
    }

    private static Status fromBytes(byte b){
        switch (b){
            case 1:
                return Status.LOW;
            case 2:
                return Status.CHARGING;
            case 3:
                return Status.FULL;
            case 4:
                return Status.NOT_CHARGING;
            default:
                return Status.UNKNOWN;
        }
    }

}
