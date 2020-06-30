package jashgopani.github.io.mibandsdk;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.schedulers.TestScheduler;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    ArrayList<int[]> tuple = new ArrayList<>();

    @Before
    public void init() {
        for(int i=0;i<05;i++){
            int on = new Random().nextInt(1000);
            int off = new Random().nextInt(1000);
            tuple.add(new int[]{on,off});
            System.out.println(on+","+off);
        }
        System.out.println();
    }
    @Test
    public void rxTime() throws Exception {

        final TestScheduler scheduler = new TestScheduler();

        long mt1 = System.nanoTime();
        System.out.println("Started at "+mt1);
        Observable.fromIterable(tuple).observeOn(scheduler).subscribeOn(scheduler).subscribe(
                t->{
                    System.out.println(Thread.currentThread());
                    long t1 = System.nanoTime();
                    System.out.println("Vibrating");
                    Thread.sleep(t[0]);
                    long t2 = System.nanoTime();
                    double tdiff = (t2-t1)/1e6;
                    System.out.println("Stopped at : "+tdiff);
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


        scheduler.advanceTimeBy(1, TimeUnit.HOURS);
    }

    @Test
    public void loopTime() throws InterruptedException {

        long t1 = System.nanoTime();
        for (int[] t : tuple) {
            System.out.println(Thread.currentThread());
            long xt1 = System.nanoTime();
            System.out.println("Vibrating");
            Thread.sleep(t[0]);
            long xt2 = System.nanoTime();
            double tdiff = (xt2-xt1)/1e6;
            System.out.println("Stopped at : "+tdiff);
            Thread.sleep(t[1]);
            System.out.println();
        }
        long t2 = System.nanoTime();
        double tdiff = (t2-t1)/1e6;
        System.out.println("Completed in "+tdiff);
    }

    @Test
    public void ltrep1() throws InterruptedException {loopTime();}

    @Test
    public void rtep2() throws Exception {rxTime();}

    @Test
    public void ltrep3() throws InterruptedException {loopTime();}

    @Test
    public void rtep4() throws Exception {rxTime();}

    @Test
    public void ltrep5() throws InterruptedException {loopTime();}

    @Test
    public void rtep6() throws Exception {rxTime();}
}