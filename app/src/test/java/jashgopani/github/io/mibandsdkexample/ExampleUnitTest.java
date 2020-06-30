package jashgopani.github.io.mibandsdkexample;

import android.os.SystemClock;
import android.util.Log;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.TestScheduler;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void rx(){
        System.out.println("rx test");
        Observable.defer(() -> Observable.just(1,2,3,4,5,6,7,8,9,0)
                .delay(10, TimeUnit.SECONDS))
                .doOnNext(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer ignore) throws Throwable {
                        System.out.println("Executed after some time");
                        Log.d("test", "accept: executed in doOnNext");
                    }
                }).observeOn(new TestScheduler())
                .subscribe(item -> Log.d("test", "rx: "+item));
    }
}