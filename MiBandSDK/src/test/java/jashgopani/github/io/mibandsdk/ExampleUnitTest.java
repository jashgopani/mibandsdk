package jashgopani.github.io.mibandsdk;

import android.util.Log;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subjects.BehaviorSubject;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private static final String TAG = "ExampleUnitTest";
    TestScheduler scheduler = new TestScheduler();
    @Test
    public void bs() {
        BehaviorSubject<Integer> behaviorSubject = BehaviorSubject.create();

        behaviorSubject.onNext(0);
        behaviorSubject.onNext(1);
        behaviorSubject.onNext(2);
        behaviorSubject.onNext(3);

        Observable.create(emitter -> {
            behaviorSubject.subscribe(new ObserverWrapper(emitter));
        }).doOnSubscribe(d->{
            System.out.println("subscriber 1 subscribed");
        }).subscribe(it->{
            System.out.println( "subscriber 1 : "+it);
        },er->{},()->{
            System.out.println( "subscriber 1 : complete");
        });
        behaviorSubject.onNext(4);
        behaviorSubject.onNext(5);
        behaviorSubject.onNext(6);
        behaviorSubject.onNext(7);

        Observable.create(emitter -> {
            behaviorSubject.subscribe(new ObserverWrapper(emitter));
        }).doOnSubscribe(d->{
            System.out.println( "subscriber 2 subscribed");
        }).subscribe(it->{
            System.out.println( "subscriber 2 : "+it);
        },er->{},()->{
            System.out.println( "subscriber 2 : complete");
        });
        behaviorSubject.onNext(69);

        scheduler.advanceTimeBy(10, TimeUnit.SECONDS);
    }
}