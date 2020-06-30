package jashgopani.github.io.mibandsdk;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import jashgopani.github.io.mibandsdk.models.Profile;


public class BluetoothIO extends BluetoothGattCallback {
    //constants
    public static final int ERROR_CONNECTION_FAILED = 178;
    public static final int ERROR_READ_RSSI_FAILED = 922;
    private static final String TAG = "BluetoothIO";
    /**
     * to call the implementation of these methods in the associated class(the one which implements this interface)
     */
    private BluetoothListener listener;
    private BluetoothGatt bluetoothGatt;

    //this is a list of all the active characteristic-listeners
    private HashMap<UUID, String> notifyListeners;

    public BluetoothIO(BluetoothListener listener) {
        this.listener = listener;
        this.notifyListeners = new HashMap<>();
    }

    /**
     * Used to connect with the bluetooth device
     *
     * @param context
     * @param device
     */
    public void connect(Context context, BluetoothDevice device) {
        device.connectGatt(context, false, this);
    }

    /**
     * Disconnect remote BLE Device
     */
    public void disconnect() {
        checkConnectionState();
        bluetoothGatt.disconnect();
        Log.d(TAG, "disconnect: Called Disconnect");
    }


    /**
     * Gets connected device
     *
     * @return A device which is connected else null
     */
    public BluetoothDevice getConnectedDevice() {
        return bluetoothGatt.getDevice();
    }

    /**
     * Throws an exception if device is not connected
     */
    private void checkConnectionState() {
        if (bluetoothGatt == null) {
            Log.d(TAG, "checkConnectionState: Connect Device First");
            throw new IllegalStateException("No Device Connected");
        }
    }

    /**
     * Write the value bytes to the provided characteristc of the provided Service
     *
     * @param serviceUUID        UUID of the service
     * @param characteristicUUID UUID of characterictic under serviceUUID
     * @param value              Value to write
     */
    public void writeCharacteristic(UUID serviceUUID, UUID characteristicUUID, byte[] value) {
        checkConnectionState();

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (characteristic != null) {
                characteristic.setValue(value);
                boolean writeResult = bluetoothGatt.writeCharacteristic(characteristic);
                if (!writeResult) {
                    notifyWithFail(serviceUUID, characteristicUUID, "BluetoothGatt write operation FAILED");
                }
            } else {
                Log.d(TAG, "writeCharacteristic: Characteristic " + characteristicUUID + " does not exist");
                notifyWithFail(serviceUUID, characteristicUUID, "Characteristic doesn't exist");
            }
        } else {
            Log.d(TAG, "writeCharacteristic: BluetoothGattService " + serviceUUID + " does not exist");
            notifyWithFail(serviceUUID, characteristicUUID, "Service doesn't exist");
        }

    }


    /**
     * Read charateristic value from the device
     *
     * @param serviceUUID
     * @param characteristicUUID
     */
    public void readCharacteristic(UUID serviceUUID, UUID characteristicUUID) {
        checkConnectionState();
        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (characteristic != null) {
                boolean readResult = bluetoothGatt.readCharacteristic(characteristic);
                if (!readResult) {
                    notifyWithFail(serviceUUID, characteristicUUID, "BluetoothGatt read operation FAILED");
                }
            } else {
                Log.d(TAG, "readCharacteristic: Characteristic " + characteristicUUID + " does not exist");
                notifyWithFail(serviceUUID, characteristicUUID, "Characteristic doesn't exist");
            }
        } else {
            Log.d(TAG, "readCharacteristic: BluetoothGattService " + serviceUUID + " does not exist");
            notifyWithFail(serviceUUID, characteristicUUID, "Service doesn't exist");
        }
    }

    /**
     * Read Rssi of remote device
     */
    public void readRssi() {
        checkConnectionState();
        boolean result = bluetoothGatt.readRemoteRssi();
        if (!result) {
            notifyWithFail(ERROR_READ_RSSI_FAILED, "Request RSSI Value Failed");
        }
    }

    /**
     * Setup notification Listener for a specific characteristic of the specific service device
     *
     * @param serviceUUID
     * @param characteristicUUID
     * @param listener           the byte[] data of characterictic which we want to listen to..
     */
    public void setNotifyListeners(UUID serviceUUID, UUID characteristicUUID, String listener) {
        checkConnectionState();

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (characteristic != null) {
                bluetoothGatt.setCharacteristicNotification(characteristic, true);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
                notifyListeners.put(characteristicUUID,listener);
            }else {
                Log.d(TAG, "setNotifyListeners: Characteristic " + characteristicUUID + " does not exist");
                notifyWithFail(serviceUUID, characteristicUUID, "Characteristic doesn't exist");
            }
        } else {
            Log.d(TAG, "setNotifyListeners: BluetoothGattService " + serviceUUID + " does not exist");
            notifyWithFail(serviceUUID, characteristicUUID, "Service doesn't exist");
        }
    }

    /**
     * Remove notify listener for the specific characteristic that was enabled
     * @param serviceUUID
     * @param characteristicUUID
     * @param listener
     */
    public void removeNotifyListener(UUID serviceUUID, UUID characteristicUUID, String listener){
        checkConnectionState();

        BluetoothGattService service = bluetoothGatt.getService(serviceUUID);
        if (service != null) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUUID);
            if (characteristic != null) {
                bluetoothGatt.setCharacteristicNotification(characteristic, false);
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(Profile.UUID_DESCRIPTOR_UPDATE_NOTIFICATION);
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);
                notifyListeners.remove(characteristicUUID);
            }else {
                Log.d(TAG, "setNotifyListeners: Characteristic " + characteristicUUID + " does not exist");
                notifyWithFail(serviceUUID, characteristicUUID, "Characteristic doesn't exist");
            }
        } else {
            Log.d(TAG, "setNotifyListeners: BluetoothGattService " + serviceUUID + " does not exist");
            notifyWithFail(serviceUUID, characteristicUUID, "Service doesn't exist");
        }
    }

    private void checkAvailableServices(){
        Log.d(TAG, "\ncheckAvailableServices: START *******************************************************\n");
        for(BluetoothGattService s : bluetoothGatt.getServices()){
            System.out.println("Service UUID : "+s.getUuid());
            HashMap<UUID,BluetoothGattCharacteristic> tempChars = new HashMap<>();
            for(BluetoothGattCharacteristic c:s.getCharacteristics()){
                System.out.println("\tCharacteristic UUID : "+c.getUuid());
                tempChars.put(c.getUuid(),c);
                for(BluetoothGattDescriptor d:c.getDescriptors()){
                    System.out.println("\t\tDescriptor UUID : "+c.getUuid());
                }
            }
        }
        Log.d(TAG, "\ncheckAvailableServices: END *********************************************************\n");
    }

    //Notifying the listeners by calling the BluetoothListeners' implemented methods
    //Basically , sending the response of the methods called using these methods

    /**
     * Returns the result data read from the device
     *
     * @param data
     * @param isReadOperation
     */
    private void notifyWithResult(BluetoothGattCharacteristic data, boolean isReadOperation) {
        if (data != null) {
            listener.onResult(data,isReadOperation);
        }
    }

    /**
     * Returns the result data read from the device
     *
     * @param data
     */
    private void notifyWithResult(int data) {
        listener.onResultRssi(data);
    }

    /**
     * Notify the listeners that operation has failed
     *
     * @param serviceUUID
     * @param characteristicUUID
     * @param msg
     */
    private void notifyWithFail(UUID serviceUUID, UUID characteristicUUID, String msg) {
        listener.onFail(serviceUUID, characteristicUUID, msg);
    }

    /**
     * Notify the listeners that operation has failed
     *
     * @param errorCode
     * @param msg
     */
    private void notifyWithFail(int errorCode, String msg) {
        listener.onFail(errorCode, msg);
    }


    //Handling of GattCallbacks


    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        super.onConnectionStateChange(gatt, status, newState);
        if(newState == BluetoothProfile.STATE_CONNECTED){
            gatt.discoverServices();
        }else {
            Log.d(TAG, "onConnectionStateChange: Disconnecting and closing connection");
            gatt.close();
            listener.onDisconnected();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        if(status == BluetoothGatt.GATT_SUCCESS){
            bluetoothGatt = gatt;
            checkAvailableServices();
            listener.onConnectionEstablished();
        }else{
            notifyWithFail(ERROR_CONNECTION_FAILED,"Services discovered failed : "+status);
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        if(BluetoothGatt.GATT_SUCCESS == status){
            notifyWithResult(characteristic,true);
        }else{
            UUID serviceUUID = characteristic.getService().getUuid();
            UUID characteristicUUID = characteristic.getUuid();
            notifyWithFail(serviceUUID,characteristicUUID,"onCharactericticRead Failed");
        }
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if(BluetoothGatt.GATT_SUCCESS == status){
            notifyWithResult(characteristic,false);
        }else{
            UUID serviceUUID = characteristic.getService().getUuid();
            UUID characteristicUUID = characteristic.getUuid();
            notifyWithFail(serviceUUID,characteristicUUID,"onCharactericticWrite Failed");
        }
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if(notifyListeners.containsKey(characteristic.getUuid())){
            notifyListeners.put(characteristic.getUuid(), Arrays.toString(characteristic.getValue()));
        }
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
        if(BluetoothGatt.GATT_SUCCESS==status){
            Log.d(TAG, "onReadRemoteRssi: rssi");
            notifyWithResult(rssi);
        }else{
            notifyWithFail(ERROR_READ_RSSI_FAILED,"onCharacteristicRead fail : "+status);
        }
    }

}
