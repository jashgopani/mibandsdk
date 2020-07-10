package jashgopani.github.io.mibandsdk;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/*
* This interface must be implemented by those class which are r
* */
public interface BluetoothListener {
    /**
     * Called on established connection
     */
    void onConnectionEstablished();

    /**
     * Called on disconnection
     */
    void onDisconnected();

    /**
     * Called on getting successful result
     * @param data Gatt Characterictic which was the target
     * @param isReadOperation Read Operation - true,  Write Operation False
     */
    void onResult(BluetoothGattCharacteristic data,boolean isReadOperation);


    /**
     * Called on getting successful result of RSSI strength
     *
     * @param rssi RSSI strength
     */
    void onResultRssi(int rssi);

    /**
     * Called on fail from service
     *
     * @param serviceUUID      Service UUID
     * @param characteristicId Characteristic ID
     * @param msg              Error message
     */
    void onFail(UUID serviceUUID,UUID characteristicId,String msg);

    /**
     * Called on fail from Bluetooth IO
     *
     * @param errorCode Error code
     * @param msg       Error message
     */
    void onFail(int errorCode,String msg);
}
