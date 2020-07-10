package jashgopani.github.io.mibandsdkexample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jashgopani.github.io.mibandsdk.MiBand;
import jashgopani.github.io.mibandsdk.models.CustomVibration;
import jashgopani.github.io.mibandsdk.models.VibrationMode;
import jashgopani.github.io.mibandsdkexample.adapters.ScanResultsAdapter;

public class MainActivity extends AppCompatActivity implements ScanResultsAdapter.OnScannedDeviceListener{

    private static final long SCAN_PERIOD = 6000;
    private static final int REQUEST_ENABLE_BT = 1;
    private static String TAG = "MainActivity";
    private Context context = MainActivity.this;
    private BluetoothAdapter bluetoothAdapter;
    private ToggleButton scanBtn, connectBtn;
    private Button vibrateBtn,customVibrateBtn, patternTextVibrateBtn,nextActivityBtn;
    private TextView statusTv,batteryLevelTv,batteryStatusTv,batteryLastUpdatedTv;
    private boolean isScanning;
    private HashSet<BluetoothDevice> addressHashSet;
    private ArrayList<BluetoothDevice> deviceArrayList;
    private ProgressBar progressBar;
    private RecyclerView scanRv;
    private ScanResultsAdapter scanResultsAdapter;
    private BluetoothDevice currentDevice;
    private boolean paired;
    private SeekBar vonSb,voffSb,vrepeatSb;
    private Switch vledSw;
    private EditText vpatternEt;
    private MiBand miBand;
    private Disposable batteryDisposable;
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
        miBand = MiBand.getInstance(context);
        currentDevice = miBand.getDevice();
        paired = currentDevice != null;
        disposables = new CompositeDisposable();
    }

    private void findViews() {
        scanBtn = findViewById(R.id.scan_btn);
        connectBtn = findViewById(R.id.connect_btn);
        statusTv = findViewById(R.id.status_tv);
        batteryLevelTv = findViewById(R.id.batteryLevelTv);
        batteryStatusTv = findViewById(R.id.batteryStatusTv);
        batteryLastUpdatedTv = findViewById(R.id.batteryLastUpdatedTv);
        scanRv = findViewById(R.id.scan_rv);
        progressBar = findViewById(R.id.progressBar);
        vibrateBtn = findViewById(R.id.vibrate_btn);
        customVibrateBtn = findViewById(R.id.customVibrate_btn);
        patternTextVibrateBtn = findViewById(R.id.vpatternBtn);
        nextActivityBtn = findViewById(R.id.nextBtn);

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
        updateUIControls();
    }

    private void setEventListeners() {
        //buttons
        scanBtn.setOnClickListener((buttonView) -> {
            boolean isChecked = scanBtn.isChecked();
            Log.d(TAG, "Find Device Btn : "+isChecked);
            //change the scanning status
            isScanning = isChecked;
            if(isChecked && !paired){
                resetAdapterData();
                //subscribe to scanCallbacks observer
                disposables.add(miBand.startScan(SCAN_PERIOD)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleScanNext(),handleScanError(), handleScanComplete()));
            }else {
                disposables.add(miBand.stopScan()
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(handleScanNext(),handleScanError()));
            }
            updateUIControls();
        });

        connectBtn.setOnClickListener((buttonView) -> {
            boolean isChecked = connectBtn.isChecked();
            updateUIControls();
            if(isChecked)
                connectAndPair();
            else{
                disconnectAndUnpair();
            }
        });

        vibrateBtn.setOnClickListener(v -> {
            miBand.vibrate(VibrationMode.VIBRATION_WITH_LED);
        });

        patternTextVibrateBtn.setOnClickListener(v -> {
            String pattern = vpatternEt.getText().toString();
            Log.d(TAG, "onClick: Pattern Vibrate : "+pattern);
            miBand.vibrate(CustomVibration.generatePattern(pattern,","));
        });

        customVibrateBtn.setOnClickListener(v->{
            miBand.vibrate(CustomVibration.generatePattern(vonSb.getProgress(),voffSb.getProgress(),vrepeatSb.getProgress()));
        });

        nextActivityBtn.setOnClickListener(v->{
            startActivity(new Intent(MainActivity.this,NextActivity.class));
        });

        //seekbars
        vonSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
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
                updateUIControls();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //switch
        vledSw.setOnCheckedChangeListener((buttonView, isChecked) -> updateUIControls());

    }

    private void connectAndPair() {
        toast("Connecting...");
        updateUIControls();
        updateStatustv("Connecting...");
        resetAdapterData();
        disposables.add(miBand.connect(currentDevice)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d->{
                    Log.d(TAG, "connectAndPair: "+context+" Subscribed to connectionSubject");
                })
                .subscribe(handleConnectionNext(), handleConnectionError(),handleConnectionComplete()));
        updateUIControls();
    }

    /**
     * Retrieve Battery Info and update UI
     */
    private void refreshBatteryInfo(boolean onlyOnce){
        if(paired)
        batteryDisposable = miBand.getBatteryInfo(15, TimeUnit.SECONDS,onlyOnce)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(info->{
                    Log.d(TAG, "refreshBatteryInfo: "+info);
                    batteryLevelTv.setText(String.valueOf(info.getLevel()));
                    batteryStatusTv.setText(info.getStatus());
                    Calendar cal = Calendar.getInstance();
                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
                    batteryLastUpdatedTv.setText(getResources().getString(R.string.lastUpdated)+sdf.format(cal.getTime()));
                },err->{
                    toast(err.getMessage());
                    err.printStackTrace();
                },()->{
                    Log.d(TAG, "getBatteryInfo: onComplete");
                });

    }

    private void disconnectAndUnpair() {
        updateUIControls();
        updateStatustv("Disconnecting...");
        miBand.disconnect(true);
        updateUIControls();
    }


    //UI related methods
    private void updateUIControls() {
        //update button state and textviews
        progressBar.setVisibility(isScanning ? View.VISIBLE : View.INVISIBLE);

        //only toggle the find device button if any device is not connected
        scanBtn.setClickable(!isScanning && !paired);
        scanBtn.setAlpha(scanBtn.isClickable() ? 1f : 0.2f);
        if(!paired)scanBtn.setChecked(isScanning);

        statusTv.setTextColor(isScanning ? Color.LTGRAY : deviceArrayList.size() > 0 ? Color.BLUE :paired?Color.GREEN:Color.RED);
        connectBtn.setClickable(!isScanning && currentDevice!=null);
        connectBtn.setAlpha(connectBtn.isClickable() ? 1f : 0.2f);
        connectBtn.setChecked(paired);

        vibrateBtn.setClickable(connectBtn.isChecked() && paired);
        vibrateBtn.setAlpha((vibrateBtn.isClickable())?1f:0.2f);

        customVibrateBtn.setClickable(vibrateBtn.isClickable());
        customVibrateBtn.setAlpha((customVibrateBtn.isClickable())?1f:0.2f);

        customVibrateBtn.setText("Seekbar Vibration\n"+seekbarsString());

        updateStatustv();
        clearBatteryDetails();
    }

    private String seekbarsString(){
        return "("+vonSb.getProgress()+","+voffSb.getProgress()+","+vrepeatSb.getProgress()+")";
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

    private void clearBatteryDetails(){
        if(!paired){
            batteryLevelTv.setText("---");
            batteryStatusTv.setText("UNKNOWN");
            batteryLastUpdatedTv.setText(getResources().getString(R.string.lastUpdated));
        }
    }

    private void resetAdapterData() {
        addressHashSet.clear();
        deviceArrayList.clear();
        scanResultsAdapter.updateList(deviceArrayList);
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
        return () -> {
            isScanning = false;
            updateUIControls();
        };
    }

    /**
     * Handle errors from Scanning method
     * @return
     */
    private Consumer<? super Throwable> handleScanError() {
        return (Consumer<Throwable>) throwable -> {
            Log.d(TAG, "Scanning Error Received: \n");
            throwable.printStackTrace();
        };
    }

    /**
     * Handle each device detected while scanning ble devices
     * @return ScanResult : It is the result given by BLE ScanCallback Use getDevice method to handle
     */
    private Consumer<? super ScanResult> handleScanNext() {
        return (Consumer<ScanResult>) result -> {
            //this method handles the scan results
            BluetoothDevice device = result.getDevice();
            if (addressHashSet.add(device)) {
                System.out.println("dbz "+device+" is new");
                deviceArrayList.add(device);
                String st = "Found " + deviceArrayList.size() + " devices";
                statusTv.setText(st);
                scanResultsAdapter.updateList(deviceArrayList);
            }
        };
    }

    /**
     * Handle onNext result of connectionSubject
     * it emits true when connected and false when disconnected
     * @return handling result value
     */
    private Consumer<? super Integer> handleConnectionNext() {
        return (Consumer<Integer>) result->{
            //if result is true = connection successful
            //else disconnect successful
            Log.d(TAG, "handleConnectionNext: From connectionSubject : "+MiBand.getStatus(result));
            if(result==MiBand.PAIRED){
                paired = true;
                refreshBatteryInfo(true);
                miBand.enableIdleDisconnect(10,TimeUnit.MINUTES);
            }else{
                paired=false;
            }
            updateUIControls();
        };
    }

    /**
     * Handle onError result of connectionSubject
     * @return Action to be performed on receiving any error
     */
    private Consumer<? super Throwable> handleConnectionError() {
        return (Consumer<Throwable>)error -> {
            paired = false;
            if(batteryDisposable!=null)batteryDisposable.dispose();
            toast(error.getMessage());
            updateUIControls();
        };
    }

    /**
     * Handle onComplete result of connectionSubject
     * @return Action to be performed. onComplete does emit any value
     */
    private Action handleConnectionComplete() {
        return () -> {
            //onComplete Method
            Log.d(TAG, "handleConnectionComplete: Band Disconnected");
            paired = false;
            if(batteryDisposable!=null)batteryDisposable.dispose();
            updateUIControls();
        };
    }

    //Other Activity Lifecycle methods

    @Override
    protected void onResume() {
        super.onResume();
        if(miBand!=null && miBand.isPaired()){
            connectAndPair();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}