package jashgopani.github.io.mibandsdk;

import io.reactivex.rxjava3.core.Emitter;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;

public class ObserverWrapper implements Observer {
    private final Emitter emitter;

    public void onSubscribe(Disposable d) {

    }

    public void onNext(Object value) {
        this.emitter.onNext(value);
    }

    public void onError(Throwable e) {
        this.emitter.onError(e);
    }

    public void onComplete() {
        this.emitter.onComplete();
    }

    public ObserverWrapper(Emitter emitter) {
        super();
        this.emitter = emitter;
    }
}
