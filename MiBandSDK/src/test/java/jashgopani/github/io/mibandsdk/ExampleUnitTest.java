package jashgopani.github.io.mibandsdk;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.util.Log;

import androidx.core.math.MathUtils;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import jashgopani.github.io.mibandsdk.models.CustomVibration;

import static java.lang.System.in;
import static java.lang.System.nanoTime;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    ArrayList<int[]> tuple = new ArrayList<>();
    ArrayList<Integer> cp = new ArrayList<>();
    final TestScheduler scheduler = new TestScheduler();
    private static final String TAG = "ExampleUnitTest";

//    @Before
    public void init(){
        for(int i=0;i<05;i++){
            int on = (i+2)*250;
            int off = (i+1)*250;
//            int on = new Random().nextInt(1000);
//            int off = new Random().nextInt(1000);
            tuple.add(new int[]{on,off});
            cp.add(on);
            cp.add(off);
            System.out.println(on+","+off);
        }
        System.out.println();
    }

    @Test
    public void stackOverflow()
    {
        long t1 = nanoTime();

        // Delay pattern:
        Flowable<Integer> vibrationTimings = Flowable.fromIterable(cp);    // off

        // Alternating true/false booleans
        Flowable<Boolean> decision = vibrationTimings.scan(true,( prevOnOff, currentValue ) -> !prevOnOff );   // subsequent values

        // Zip the two together
        vibrationTimings.zipWith( decision, ( delay, shouldVibrate ) -> Flowable.just(shouldVibrate)//Creating observables of individual (vibrationTime,decision) pair
                .doOnNext(__ ->System.out.println("onNext : "+shouldVibrate+" | "+Thread.currentThread()))//Invoke function based on value
                .delay(delay, TimeUnit.MILLISECONDS)) // Delay the value downstream i.e delay calling of onNext Method by the observable
                //Till here we created multiple single observers, now to combine all in sequence, we use concat map
                //boolean value which we are emitting is of no use, we're just doing it for the sake of delaying and moving to next value
                .concatMap( (Flowable<Boolean> shouldVibrate) -> shouldVibrate)
                .subscribeOn(Schedulers.computation())
                .ignoreElements()//ignore all the emitted values
                .blockingAwait();//wait for the observable to terminate

        long t2 = nanoTime();
        double tdiff = (t2-t1)/1e6;
        System.out.println(tdiff);

    }

    @Test
    public void repeatPattern() throws Exception {
        int r = 2;
        int clamp = MathUtils.clamp(21, 0, 2);
        System.out.println(clamp);
        Integer[] pattern = CustomVibration.generatePattern(r);
        Assert.assertEquals(pattern.length,CustomVibration.DEFAULT.length*r);
    }


    @Test
    public void rxUPTime() throws Exception {


        long mt1 = nanoTime();
        System.out.println("Started at "+mt1);
        Observable.fromIterable(tuple).observeOn(scheduler).subscribeOn(scheduler).subscribe(
                t->{
                    System.out.println(Thread.currentThread());
                    long t1 = nanoTime();
                    System.out.println("Vibrating");
                    Thread.sleep(t[0]);
                    long t2 = nanoTime();
                    double tdiff = (t2-t1)/1e6;
                    System.out.println("Stopped at : "+tdiff);
                    Thread.sleep(t[1]);
                    System.out.println();
                },
                e->e.printStackTrace(),
                ()->{

                    long mt2 = nanoTime();
                    double mtdiff = (mt2-mt1)/1e6;
                    System.out.println("Completed at "+mt2);
                    System.out.println("Completed in "+mtdiff+"s");
                }
        );


        scheduler.advanceTimeBy(1, TimeUnit.HOURS);
    }

    @Test
    public void loopTime() throws InterruptedException {

        long t1 = nanoTime();
        for (int[] t : tuple) {
            System.out.println(Thread.currentThread());
            long xt1 = nanoTime();
            System.out.println("Vibrating");
            Thread.sleep(t[0]);
            long xt2 = nanoTime();
            double tdiff = (xt2-xt1)/1e6;
            System.out.println("Stopped at : "+tdiff);
            Thread.sleep(t[1]);
            System.out.println();
        }
        long t2 = nanoTime();
        double tdiff = (t2-t1)/1e6;
        System.out.println("Completed in "+tdiff);
    }

    @Test
    public void handleDelay(){
        System.out.println("handle");
        long SCAN_TIMEOUT = 6000;
        long t1 = nanoTime();
        Observable.defer(() -> Observable.empty()//1 is just to avoid error / put anything except null
                .delay(SCAN_TIMEOUT, TimeUnit.MILLISECONDS))
                .doOnNext(ignore -> {
                   System.out.println("Wait for delay...");
                })
                .blockingSubscribe(item->{
                    System.out.println("item = "+item);
                },throwable -> {},()->{
                    System.out.println("onComplete : ");
                    //stop emitting values and notify subscribers that emitting is complete
                    long t2 = nanoTime();
                    double tdiff = (t2-t1)/1e6;
                    System.out.println("Handle: Stopped BLE Scan after "+tdiff+"s");
                });//ignore onNext,onError,onComplete

    }


    @Test
    public void vibrations(){
        Integer[] integers = CustomVibration.generatePattern(2, 2);
        Assert.assertNotNull(integers);
        System.out.println(Arrays.toString(integers));

    }

}