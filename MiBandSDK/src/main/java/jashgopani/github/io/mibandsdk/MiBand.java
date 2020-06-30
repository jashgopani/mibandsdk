package jashgopani.github.io.mibandsdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.content.Context;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import jashgopani.github.io.mibandsdk.models.BatteryInfo;
import jashgopani.github.io.mibandsdk.models.CustomVibration;
import jashgopani.github.io.mibandsdk.models.Profile;
import jashgopani.github.io.mibandsdk.models.Protocol;
import jashgopani.github.io.mibandsdk.models.VibrationMode;


/**
 *  Created by https://github.com/jashgopani/ on 30-06-2020
 */


public class MiBand implements BluetoothListener {
    private static final String TAG = "MiBand";
    //singleton
    private static MiBand miBand = null;

    //for interacting with remote device
    private BluetoothIO bluetoothIo = new BluetoothIO(this);

    private PublishSubject<Boolean> connectionSubject;
    private PublishSubject<Integer> rssiSubject;
    private PublishSubject<BatteryInfo> batteryInfoSubject;
    private PublishSubject<Boolean> pairSubject;
    private boolean pairRequested = false;
    private PublishSubject<Void> startVibrationSubject;
    private PublishSubject<Void> stopVibrationSubject;
    private PublishSubject<Boolean> sensorNotificationSubject;
    private PublishSubject<Boolean> realtimeNotificationSubject;
    private PublishSubject<Void> userInfoSubject;
    private PublishSubject<Void> heartRateSubject;
    private Context context;

    private MiBand(Context c){
        this.context = c;
        connectionSubject = PublishSubject.create();
        rssiSubject = PublishSubject.create();
        batteryInfoSubject= PublishSubject.create();
        pairSubject = PublishSubject.create();
        startVibrationSubject = PublishSubject.create();
        stopVibrationSubject = PublishSubject.create();
        sensorNotificationSubject = PublishSubject.create();
        realtimeNotificationSubject = PublishSubject.create();
        userInfoSubject = PublishSubject.create();
        heartRateSubject = PublishSubject.create();
    }

    public static MiBand getInstance(Context c){
        return (miBand==null)?new MiBand(c):miBand;
    }

    public final BluetoothDevice getDevice() {
        return this.bluetoothIo.getConnectedDevice();
    }

    /**
     * This method returns an Observable of class ScanResult which can be subscribed in order to handle each each result
     * @return Observable<ScanResult>
     */
    public Observable<ScanResult> startScan(long SCAN_TIMEOUT){
        //emitter is responsible for emitting values from the observer
        //we call onNext method of emitter to emit a value ; hence we call it in the scanCallback method

        return Observable.create(emitter -> {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if(adapter!=null){
                final BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
                if(scanner != null){

                    //stop scanning after 6secs automatically
                    long t1 = System.nanoTime();//for debugging purposes
                    Log.d(TAG, "startScan: ");
                    //This observable is similaer to handler.postDelayed
                    Observable.defer(() -> Observable.just(1)//1 is just to avoid error / put anything except null
                            .delay(SCAN_TIMEOUT, TimeUnit.MILLISECONDS))
                            .doOnNext(ignore -> {
                                //stop emitting values and notify subscribers that emitting is complete
                                emitter.onComplete();
                                scanner.stopScan(MiBand.this.getScanCallback(emitter));

                                long t2 = System.nanoTime();
                                double tdiff = (t2-t1)/1e6;
                                Log.d(TAG, "Handle: Stopped BLE Scan after "+tdiff+"s");
                            })
                            .subscribe(item->{},throwable -> {},()->{});//ignore onNext,onError,onComplete

                    //start scanning for ble devices
                    scanner.startScan(MiBand.this.getScanCallback(emitter));
                }else{
                    Log.d(TAG, "startScan: Bluetooth Scanner is null");
                    emitter.onError(new NullPointerException("Bluetooth Scanner is null"));
                }
            }else{
                Log.d(TAG, "startScan: Bluetooth Adapter is null");
                emitter.onError(new NullPointerException("Bluetooth Adapter is null"));
            }
        });
    }

    /**
     * Stops scanning of devices
     * @return
     */
    public final Observable<ScanResult> stopScan(){
        return Observable.create(new ObservableOnSubscribe<ScanResult>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<ScanResult> emitter) throws Throwable {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if(adapter!=null){
                    BluetoothLeScanner scanner = adapter.getBluetoothLeScanner();
                    if(scanner != null){
                        scanner.stopScan(MiBand.this.getScanCallback(emitter));
                    }else{
                        Log.d(TAG, "startScan: Bluetooth Scanner is null");
                        emitter.onError(new NullPointerException("Bluetooth Scanner is null"));
                    }
                }else{
                    Log.d(TAG, "startScan: Bluetooth Adapter is null");
                    emitter.onError(new NullPointerException("Bluetooth Adapter is null"));
                }
            }
        });
    }

    /**
     * Returns a scan Callback for startScan and stopScan
     * @param subscriber
     * @return
     */
    private ScanCallback getScanCallback(final ObservableEmitter subscriber) {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, android.bluetooth.le.ScanResult result) {
                super.onScanResult(callbackType, result);
                subscriber.onNext(result);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                subscriber.onError(new Exception("Scan Failed : Error "+errorCode));
            }
        };
    }

    /**
     * Creates connection between app and BLE device(miband)
     * It is a boolean observable since it emits only boolean values : connection success(true) or failed(false)
     * @param device
     * @return
     */
    public Observable<Boolean> connect(final BluetoothDevice device){
        return Observable.create(new ObservableOnSubscribe<Boolean>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Boolean> emitter) throws Throwable {
                connectionSubject.subscribe(new ObserverWrapper(emitter));
                bluetoothIo.connect(context,device);
            }
        });
    }

    /**
     * Performs Pairing of device
     * @return
     */
    public Observable<Boolean> pair(){
        return Observable.create((ObservableOnSubscribe<Boolean>) emitter -> {
            pairRequested = true;
            pairSubject.subscribe(new ObserverWrapper(emitter));
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_MILI,Profile.UUID_CHAR_PAIR, Protocol.PAIR);
        });
    }

    public Observable<Boolean> disconnect(){
        return Observable.create(emitter -> {
            connectionSubject.subscribe(new ObserverWrapper(emitter));
            bluetoothIo.disconnect();
        });
    }

    public Observable<Integer> readRssi(){
        return Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Integer> emitter){
                rssiSubject.subscribe(new ObserverWrapper(emitter));
                bluetoothIo.readRssi();
            }
        });
    }

    public Observable<BatteryInfo> getBatteryInfo(){
        return Observable.create(new ObservableOnSubscribe<BatteryInfo>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<BatteryInfo> emitter) throws Throwable {
                batteryInfoSubject.subscribe(new ObserverWrapper(emitter));
                bluetoothIo.readCharacteristic(Profile.UUID_SERVICE_MILI,Profile.UUID_CHAR_BATTERY);
            }
        });
    }

    /**
     * Starts the vibration based on profile
     * @param mode
     * @return
     */
    public Observable<Void> startVibration(final VibrationMode mode){
        return Observable.create(emitter -> {
            byte[] protocol = null;
            switch (mode){
                case VIBRATION_WITH_LED:
                    protocol = Protocol.VIBRATION_WITH_LED;
                case VIBRATION_WITHOUT_LED:
                    protocol = Protocol.VIBRATION_WITHOUT_LED;
                case VIBRATION_10_TIMES_WITH_LED:
                    protocol = Protocol.VIBRATION_10_TIMES_WITH_LED;
                default:
                    Log.d(TAG, "vibrateBand: Mode doesn't exist");
            }
            startVibrationSubject.subscribe(new ObserverWrapper(emitter));
            bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION,Profile.UUID_CHAR_VIBRATION,protocol);
        });
    }

    public void startVibration(int[] p,int repeat){
        Log.d(TAG, "startVibration: Custom Vibration ");
        ArrayList<int[]> tuple = new ArrayList<>();
        for (int i = 0; i < repeat; i++) {
            tuple.add(p);
        }
        long mt1 = System.nanoTime();
        System.out.println("Started at "+mt1);
        Observable.fromIterable(tuple).observeOn(Schedulers.io()).subscribeOn(Schedulers.computation()).subscribe(
                t->{
                    System.out.println(Thread.currentThread());
                    bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.VIBRATION_WITH_LED);
                    Thread.sleep(t[0]);
                    bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION);
                    Thread.sleep(t[1]);
                    System.out.println();
                },
                e->e.printStackTrace(),
                ()->{
                    long mt2 = System.nanoTime();
                    double mtdiff = (mt2-mt1)/1e6;
                    System.out.println("Completed at "+mt2);
                    System.out.println("Completed in "+mtdiff+"s");
                }
        );
    }

    /**
     * Stop vibration
     * @return
     */
    public Observable<Void> stopVibration(){
        return Observable.create(new ObservableOnSubscribe<Void>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Void> emitter) throws Throwable {
                stopVibrationSubject.subscribe(new ObserverWrapper(emitter));
                bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION,
                        Protocol.STOP_VIBRATION);
            }
        });
    }

    private void notifyConnectionResult(Boolean result){
        connectionSubject.onNext(result);
        connectionSubject.onComplete();
        Log.d(TAG, "notifyConnectionResult: Emission complete ; Value was = "+result);

        //create a new connection subject
        connectionSubject = PublishSubject.create();
    }

    //Implementation of Interface methods
    /*
    * These methods will be called by BluetoothIO class for verifying and notifying the status of various tasks
    * like connection,read/write success or fail
    * */
    @Override
    public void onConnectionEstablished() {
        Log.d(TAG, "onConnectionEstablished: notifying connection success");
        notifyConnectionResult(true);
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "onDisconnected: notifying disconnection success");
        notifyConnectionResult(false);
    }


    @Override
    public void onResult(BluetoothGattCharacteristic data,boolean wasReadOperation) {
        UUID serviceUUID = data.getService().getUuid();
        UUID characteristicUUID = data.getUuid();
        byte[] characteristicValue = data.getValue();

        if(Profile.UUID_SERVICE_MILI.equals(serviceUUID)){

            //handle pairing result
            if(Profile.UUID_CHAR_PAIR.equals(characteristicUUID)){
                /**
                 * if result is from write operation
                 * if pairRequested is true, means that we have now completed pairing operation
                 * and we will read the characteristic value again and verify pair status
                 * so set pairRequested to false and based on the read characteristic value, we will notify success or failure
                 */
                if(pairRequested && !wasReadOperation){
                    //reading characteristic for verification
                    Log.d(TAG, "onResult: Attempt to read Pairing characteristic | current op = "+wasReadOperation);
                    bluetoothIo.readCharacteristic(Profile.UUID_SERVICE_MILI,Profile.UUID_CHAR_PAIR);
                    pairRequested = false;

                }else if(!pairRequested && wasReadOperation){
                    //if value read is correct i.e matches the pairing value, then emit onComplete
                    Log.d(TAG, "onResult: Read Attempt result Pairing characteristic | current op = "+wasReadOperation);
                    if(Arrays.equals(Protocol.PAIR,characteristicValue)){
                        Log.d(TAG, "onResult: Pairing success");
                        pairSubject.onNext(true);
                        pairSubject.onComplete();
                        pairSubject = PublishSubject.create();
                    }else{

                        pairSubject.onError(new Exception("Pairing Failed"));
                        pairSubject = PublishSubject.create();
                    }
                }

            }

            //handle battery info result
            if(Profile.UUID_CHAR_BATTERY.equals(characteristicUUID)){
                if(characteristicValue.length==10){
                    Log.d(TAG, "onResult: BatteryInfo : "+Arrays.toString(characteristicValue));
                }
            }


        }
        //Vibration service
        if(Profile.UUID_SERVICE_VIBRATION.equals(serviceUUID)){
            if(Profile.UUID_CHAR_VIBRATION.equals(characteristicUUID)){
                if(Arrays.equals(characteristicValue,Protocol.STOP_VIBRATION)){
                    Log.d(TAG, "onResult: Vibration Complete");
                    stopVibrationSubject.onComplete();
                    stopVibrationSubject = PublishSubject.create();
                }else{
                    Log.d(TAG, "onResult: Vibration Ongoing : "+Arrays.toString(characteristicValue));
                }
            }
        }
    }


    @Override
    public void onResultRssi(int rssi) {
        rssiSubject.onNext(rssi);
        rssiSubject.onComplete();
        rssiSubject = PublishSubject.create();
    }

    @Override
    public void onFail(UUID serviceUUID, UUID characteristicId, String msg) {
        if (serviceUUID == Profile.UUID_SERVICE_MILI) {

            // Battery info
            if (characteristicId == Profile.UUID_CHAR_BATTERY) {
                Log.d(TAG, "onFail: Get Battery Info Failed : "+msg);
                batteryInfoSubject.onError(new Exception("Wrong data format for battery info"));
                batteryInfoSubject = PublishSubject.create();
            }

            // Pair
            if (characteristicId == Profile.UUID_CHAR_PAIR) {
                Log.d(TAG, "onFail: Pair failed : "+msg);
                pairSubject.onError(new Exception("Pairing failed"));
                pairSubject = PublishSubject.create();
            }
        }
        // vibration service
        if (serviceUUID == Profile.UUID_SERVICE_VIBRATION) {
            if (characteristicId == Profile.UUID_CHAR_VIBRATION) {
                Log.d(TAG, "onFail: Enable/Disabled Vibration Failed : "+msg);
                stopVibrationSubject.onError(new Exception("Enable/disable vibration failed"));
                stopVibrationSubject = PublishSubject.create();
            }
        }
    }

    @Override
    public void onFail(int errorCode, String msg) {
        Log.d(TAG, "onFail: Error code "+errorCode+" | Message : "+msg);
        switch (errorCode){
            case BluetoothIO.ERROR_CONNECTION_FAILED:
                connectionSubject.onError(new Exception("Establishing connection failed"));
                connectionSubject = PublishSubject.create();
                break;
            case BluetoothIO.ERROR_READ_RSSI_FAILED:
                rssiSubject.onError(new Exception("Reading RSSI failed"));
                rssiSubject = PublishSubject.create();
                break;
            default:
                Log.d(TAG, "onFail: Unknown error Code "+errorCode);
        }
    }
}
