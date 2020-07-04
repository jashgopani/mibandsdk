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
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
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
    private BluetoothIO bluetoothIo;

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
        bluetoothIo = new BluetoothIO(this);
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
        if(miBand==null){
            miBand = new MiBand(c);
            Log.d(TAG, "getInstance: New Band will be created for : "+c);
        }else{
            Log.d(TAG, "getInstance: Already Instantiated");
        }
        return miBand;
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
        return Observable.create(emitter -> {
            Log.d(TAG, "getBatteryInfo: Requested Log");
            batteryInfoSubject.subscribe(new ObserverWrapper(emitter));
            bluetoothIo.readCharacteristic(Profile.UUID_SERVICE_MILI,Profile.UUID_CHAR_BATTERY);
        });
    }

    /**
     * Get Vibration data to be written based on LED Requirements
     * @param mode
     * @return
     */
    private byte[] getVibrationProtocol(VibrationMode mode){
        if (mode==null)return Protocol.VIBRATION_WITH_LED;

        switch (mode){
            case VIBRATION_WITHOUT_LED:
                return Protocol.VIBRATION_WITHOUT_LED;
            case VIBRATION_10_TIMES_WITH_LED:
                return Protocol.VIBRATION_10_TIMES_WITH_LED;
            default:
                return Protocol.VIBRATION_WITH_LED;
        }
    }

    /**
     * Vibrates the MIBand with Default Vibration Pattern Once
     */
    public void vibrate(){
        vibrate(CustomVibration.DEFAULT,getVibrationProtocol(null));
    }

    /**
     * Vibrates the MiBand {@param} vibrationCount times
     * @param vibrationCount
     */
    public void vibrate(int vibrationCount){
        vibrate(CustomVibration.generatePattern(vibrationCount));
    }

    /**
     * Vibrates the MiBand using DEFAULT vibration with {@param} mode once
     * @param mode
     */
    public void vibrate(VibrationMode mode){
        vibrate(CustomVibration.DEFAULT,getVibrationProtocol(mode));
    }

    /**
     * Vibrates according to the custom pattern given by the user
     * @param vpattern
     */
    public void vibrate(Integer[] vpattern){
        vibrate(vpattern,getVibrationProtocol(null));
    }

    public void vibrate(Integer[] vpattern,VibrationMode mode){
        if(vpattern==null)vpattern = CustomVibration.DEFAULT;
        vibrate(vpattern,getVibrationProtocol(mode));
    }


    /**
     * Internal vibration method logic
     * @param vpattern
     * @param mode
     */
    private void vibrate(Integer[] vpattern, byte[] mode){
        if(vpattern.length %2 == 1){
            Integer[] temp = new Integer[vpattern.length+1];
            System.arraycopy(vpattern,0,temp,0,vpattern.length);
            temp[vpattern.length]=0;
            vpattern = temp;
        }
        // Delay pattern:
        Flowable<Integer> vibrationTimings = Flowable.fromArray(vpattern);    // off

        // Alternating true/false booleans
        Flowable<Boolean> decision = vibrationTimings.scan(true,( prevOnOff, currentValue ) -> !prevOnOff );   // subsequent values

        // Zip the two together
        vibrationTimings.zipWith( decision, ( delay, shouldVibrate ) -> Flowable.just(shouldVibrate)//Creating observables of individual (vibrationTime,decision) pair
                .doOnNext(shouldVibrateValue ->{
                    if(shouldVibrate)
                        Log.d(TAG, "VibrationPattern: "+delay);
                    if(shouldVibrateValue)
                        bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, mode);
                    else
                        bluetoothIo.writeCharacteristic(Profile.UUID_SERVICE_VIBRATION, Profile.UUID_CHAR_VIBRATION, Protocol.STOP_VIBRATION);
                })//Invoke function based on value
                .delay(delay, TimeUnit.MILLISECONDS)) // Delay the value downstream i.e delay calling of onNext Method by the observable
                //Till here we created multiple single observers, now to combine all in sequence, we use concat map
                //boolean value which we are emitting is of no use, we're just doing it for the sake of delaying and moving to next value
                .concatMap( (Flowable<Boolean> shouldVibrate) -> shouldVibrate)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.computation())
                .ignoreElements()//ignore all the emitted values
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        Log.d(TAG, "onSubscribe: MiBand >> Vibration Started on "+Thread.currentThread());
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: MiBand >> Vibration Complete");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: MiBand >> Vibration Error");
                        e.printStackTrace();
                    }
                });//wait for the observable to terminate
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
                        vibrate(CustomVibration.DEFAULT);
                        pairSubject.onNext(true);
                        pairSubject.onComplete();
                        Log.d(TAG, "onResult: Current Battery Data : "+getBatteryInfo());
                        pairSubject = PublishSubject.create();
                    }else{

                        pairSubject.onError(new Exception("Pairing Failed"));
                        pairSubject = PublishSubject.create();
                    }
                }

            }

            //handle battery info result
            if(Profile.UUID_CHAR_BATTERY.equals(characteristicUUID)){
                Log.d(TAG, "onResult: BATTERY RESULTS RECEIVED");
                if(true){
                    Log.d(TAG, "onResult: BatteryInfo : "+Arrays.toString(characteristicValue));
                    BatteryInfo batteryInfo = BatteryInfo.fromByteData(characteristicValue);
                    batteryInfoSubject.onNext(batteryInfo);
                    Log.d(TAG, "onResult: Battery Info : "+batteryInfo);
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
