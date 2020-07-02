package jashgopani.github.io.mibandsdkexample;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jashgopani.github.io.mibandsdk.MiBand;
import jashgopani.github.io.mibandsdk.models.CustomVibration;
import jashgopani.github.io.mibandsdk.models.Protocol;
import jashgopani.github.io.mibandsdk.models.VibrationMode;
import jashgopani.github.io.mibandsdkexample.adapters.ScanResultsAdapter;

public class MainActivity extends AppCompatActivity implements ScanResultsAdapter.OnScannedDeviceListener{

    private static final long SCAN_PERIOD = 6000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static String TAG = "MainActivity";
    private Context context = MainActivity.this;
    private BluetoothAdapter bluetoothAdapter;
    private ToggleButton scanBtn, connectBtn;
    private Button vibrateBtn,customVibrateBtn,patternVibrateBtn;
    private TextView statusTv;
    private Handler handler;
    private boolean isScanning;
    private HashSet<String> addressHashSet;
    private ArrayList<BluetoothDevice> deviceArrayList;
    private ProgressBar progressBar;
    private RecyclerView scanRv;
    private ScanResultsAdapter scanResultsAdapter;
    private BluetoothDevice currentDevice;
    private boolean connected,paired;
    private HashMap<UUID,HashMap<UUID,BluetoothGattCharacteristic>> servicesMap;
    private SeekBar vonSb,voffSb,vrepeatSb;
    private Switch vledSw;
    CustomVibration customVibration;
    private EditText vpatternEt;
    private MiBand miBand;
    CompositeDisposable disposables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        initializeClassFields();
        findViews();
        configureViews();
        setEventListeners();
        checkDeviceCompatibility();
        getPermissions();
        updateUIControls();
    }

    private void getPermissions() {
        String permissions[] ={
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,

        };
        ActivityCompat.requestPermissions(MainActivity.this, permissions, 0);

    }

    //methods used by init
    private void checkDeviceCompatibility() {
        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        //check bluetooth enabled or not
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent =
                    new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private void initializeClassFields() {
        addressHashSet = new HashSet<>();
        deviceArrayList = new ArrayList<>();
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        scanResultsAdapter = new ScanResultsAdapter(deviceArrayList, this);
        connected = false;
        paired = false;
        servicesMap = new HashMap<>();
        customVibration = new CustomVibration();
        miBand = MiBand.getInstance(context);
        disposables = new CompositeDisposable();
    }

    private void findViews() {
        scanBtn = findViewById(R.id.scan_btn);
        connectBtn = findViewById(R.id.connect_btn);
        statusTv = findViewById(R.id.status_tv);
        scanRv = findViewById(R.id.scan_rv);
        progressBar = findViewById(R.id.progressBar);
        vibrateBtn = findViewById(R.id.vibrate_btn);
        customVibrateBtn = findViewById(R.id.customVibrate_btn);
        patternVibrateBtn = findViewById(R.id.vpatternBtn);

        //seek bars
        vonSb = findViewById(R.id.vonSb);
        voffSb = findViewById(R.id.voffSb);
        vrepeatSb = findViewById(R.id.vrepeatSb);

        //switch
        vledSw = findViewById(R.id.vledSw);

        //edittext
        vpatternEt = findViewById(R.id.vpatternEt);
    }

    private void configureViews() {
        scanRv.setAdapter(scanResultsAdapter);
        scanRv.setLayoutManager(new LinearLayoutManager(context));

        //set min max values of seekbars
        vonSb.setMax(CustomVibration.MAX_ON);
        voffSb.setMax(CustomVibration.MAX_OFF);
        vrepeatSb.setMax(CustomVibration.MAX_REPEAT);

        //set switch
        vledSw.setChecked(customVibration.isLedEnabled());
    }


    private void setEventListeners() {
        //scans nearby BLE devices and stops scanning in SCAN_PERIOD time
        scanBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(TAG, "Find Device Btn : "+isChecked);
            //change the scanning status
            isScanning = isChecked;

            if(isChecked){
                //subscribe to scanCallbacks observer
                disposables.add(miBand.startScan(SCAN_PERIOD)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleScanResult(),handleScanError(), handleScanComplete()));
            }else {
                disposables.add(miBand.stopScan()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleScanResult(),handleScanError()));
            }
            updateUIControls();
        });

        //connects to the selected device
        connectBtn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(isChecked)
                connectAndPair();
            else
                disconnectAndUnpair();
        });

        vibrateBtn.setOnClickListener(v -> vibrateBand(VibrationMode.VIBRATION_WITH_LED));

        customVibrateBtn.setOnClickListener(v -> vibrateBand(customVibration));

        vonSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                customVibration.setOnTime(progress);
                updateUIControls();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        voffSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                customVibration.setOffTime(progress);
                updateUIControls();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        vrepeatSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                customVibration.setRepeat(progress);
                updateUIControls();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        vledSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                customVibration.setProtocol(isChecked? Protocol.VIBRATION_WITH_LED:Protocol.VIBRATION_WITHOUT_LED);
                updateUIControls();
            }
        });

        patternVibrateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pattern = vpatternEt.getText().toString();
                Log.d(TAG, "onClick: Pattern Vibrate : "+pattern);
                vibrateBand(pattern==null?"":pattern.trim());
            }
        });

    }



    private void connectAndPair() {
        toast("Connecting...");
        updateUIControls();
        updateStatustv("Connecting...");
        deviceArrayList.clear();
        scanResultsAdapter.updateList(deviceArrayList);
        disposables.add(miBand.connect(currentDevice)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(handleConnectionNext(), handleConnectionError(),handleConnectionComplete()));
        updateUIControls();
    }

    private void disconnectAndUnpair() {
        updateUIControls();
        updateStatustv("Disconnecting...");
        deviceArrayList.clear();
        scanResultsAdapter.updateList(deviceArrayList);
        disposables.add(miBand.disconnect()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(handleConnectionNext(), handleConnectionError(),handleConnectionComplete()));
        updateUIControls();
    }

    private void pair(){
        Log.d(TAG, "connectAndPair: Connection Complete, Pairing start");

        //start pairing inside onComplete of connect
        toast("Pairing...");
        paired = false;
        updateStatustv("Pairing...");
        updateUIControls();

        disposables.add(miBand.pair()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        pairResult -> {
                            //returns void
                            paired = pairResult;
                        },
                        pairError -> {
                            //pairing failed
                            paired = false;
                            toast(pairError.getMessage());
                            Log.d(TAG, "connectAndPair: Pairing Error : ");
                            pairError.printStackTrace();
                            currentDevice = null;
                        },
                        () -> {
                            toast(paired?"Paired Successfully":"Device not Paired");
                            updateUIControls();
                        }
                )
        );
    }


    private void vibrateBand(CustomVibration customVibration) {
        int repeat = vrepeatSb.getProgress();
        int onTime = vonSb.getProgress();
        int offTime = voffSb.getProgress();
        int plen = (repeat*2);
        Integer pattern[] = new Integer[plen];
        for (int i = 0; i < plen-1; i+=2) {
            Log.d(TAG, "vibrateBand: @"+i);
            pattern[i] = onTime;
            if(i==plen-1)
            pattern[i+1] = 0;
            else
            pattern[i+1] = offTime;
        }
        Log.d(TAG, "vibrateBandFromSeekbars: "+Arrays.toString(pattern));
        miBand.startVibration(pattern);
    }

    private void vibrateBand(String pattern){
        Integer []customPattern;
        if (pattern == null || pattern.length() == 0) {
            customPattern = new Integer[]{350,100,200};
        } else {
            String[] split = pattern.split(",");
            //if last value is off-time then it is useless
            int patternLength = split.length % 2 == 0 ? split.length : (split.length - 1);
            customPattern = new Integer[patternLength];
            for (int i = 0; i < patternLength; i++) {
                customPattern[i] = Integer.parseInt(split[i]);
            }
        }

        miBand.startVibration(customPattern);
    }

    private void vibrateBand(VibrationMode mode) {
        disposables.add(miBand.startVibration(mode)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }


    //UI related methods
    private void updateUIControls() {
        //update button state and textviews
        progressBar.setVisibility(isScanning ? View.VISIBLE : View.INVISIBLE);

        //only toggle the find device button if any device is not connected
        scanBtn.setClickable(!paired);
        scanBtn.setAlpha(scanBtn.isClickable() ? 1f : 0.2f);
        if(!paired)scanBtn.setChecked(isScanning);

        statusTv.setTextColor(isScanning ? Color.LTGRAY : deviceArrayList.size() > 0 ? Color.GREEN : Color.RED);

        connectBtn.setClickable(!isScanning && currentDevice!=null);
        connectBtn.setAlpha(connectBtn.isClickable() ? 1f : 0.2f);

        vibrateBtn.setClickable(paired);
        vibrateBtn.setAlpha((paired)?1f:0.2f);

        customVibrateBtn.setClickable(paired);
        customVibrateBtn.setAlpha((paired)?1f:0.2f);

        customVibrateBtn.setText("Custom Vibration\n"+customVibration);
        updateStatustv();
        updateSeekbars();
    }

    private void updateStatustv() {
        if (currentDevice == null) {
            statusTv.setText(R.string.status_doScan);
        } else {
            statusTv.setText(currentDevice.getAddress());
        }
    }

    private void updateStatustv(String statusText){
        statusTv.setText(statusText);
    }

    private void updateSeekbars() {
        vonSb.setProgress(customVibration.getOnTime());
        voffSb.setProgress(customVibration.getOffTime());
        vrepeatSb.setProgress(customVibration.getRepeat());
    }

    /**
     * Handles click event on Recycler View
     * @param position
     */
    @Override
    public void onDeviceClick(int position) {
        //onclick listener for recycler view item
        if (!isScanning) {//click works only if scanning is complete
            BluetoothDevice device = deviceArrayList.get(position);
            currentDevice = device;
            Log.d(TAG, "onDeviceClick: " + position + " | " + device);
        }
        updateUIControls();
    }

    /**
     * Utility toast method
     * @param msg
     */
    private void toast(final String msg){
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    //methods for handling observable results

    /**
     * Handle onComplete of scanning method
     * @return
     */
    private Action handleScanComplete() {
        return new Action() {
            @Override
            public void run() throws Throwable {
                isScanning = false;
                updateUIControls();
            }
        };
    }

    /**
     * Handle errors from Scanning method
     * @return
     */
    private Consumer<? super Throwable> handleScanError() {
        return new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Throwable {
                Log.d(TAG, "Scanning Error Received: \n");
                throwable.printStackTrace();
            }
        };
    }

    /**
     * Handle each device detected while scanning ble devices
     * @return ScanResult : It is the result given by BLE ScanCallback Use getDevice method to handle
     */
    private Consumer<? super ScanResult> handleScanResult() {
        return new Consumer<ScanResult>() {
            @Override
            public void accept(ScanResult result) throws Throwable {
                //this method handles the scan results
                BluetoothDevice device = result.getDevice();
                if (addressHashSet.add(device.getAddress())) {
                    deviceArrayList.add(device);
                    String st = "Found " + deviceArrayList.size() + " devices";
                    statusTv.setText(st);
                    Log.d(TAG, "leScanCallBack: New device added : " + device.getAddress());
                    scanResultsAdapter.updateList(deviceArrayList);
                }
            }
        };
    }
    /**
     * Handle onComplete result of connectionSubject
     * @return Action to be performed. onComplete does emit any value
     */
    private Action handleConnectionComplete() {
        return () -> {
            //onComplete Method
            Log.d(TAG, "handleConnectionComplete: "+paired);
            updateUIControls();
        };
    }

    /**
     * Handle onError result of connectionSubject
     * @return Action to be performed on receiving any error
     */
    private Consumer<? super Throwable> handleConnectionError() {
        return (Consumer<Throwable>)error -> {
            toast(error.getMessage());
            updateUIControls();
        };
    }

    /**
     * Handle onNext result of connectionSubject
     * it emits true when connected and false when disconnected
     * @return handling result value
     */
    private Consumer<? super Boolean> handleConnectionNext() {
        return (Consumer<Boolean>) result->{
            //if result is true = connection successful
            //else disconnect successful
            Log.d(TAG, "handleConnectionNext: "+currentDevice.getAddress()+(result?" Connected":" Disconnected"));
            if(result){
                pair();
            }
            paired = result;

            updateUIControls();
        };
    }


    //Other Activity Lifecycle methods
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectAndUnpair();
        disposables.clear();
    }


}