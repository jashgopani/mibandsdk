package jashgopani.github.io.mibandsdkexample;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import jashgopani.github.io.mibandsdk.MiBand;

public class NextActivity extends AppCompatActivity {
    private static final String TAG = "NextActivity";
    MiBand miBand;
    Disposable subscribe;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_next);

        miBand = MiBand.getInstance(NextActivity.this);
        Log.d(TAG, "onCreate: Miband is Paired : "+miBand.isPaired());
        subscribe = miBand.connect(null).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).doOnSubscribe(d -> {
            Log.d(TAG, "onCreate: Subscriber is " + NextActivity.this);
        }).subscribe(i -> {
            Log.d(TAG, "onCreate: " + MiBand.getStatus(i));
        }, e -> {
        }, () -> {
            Log.d(TAG, "onCreate: Disconnected");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        subscribe.dispose();
    }
}