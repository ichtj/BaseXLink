package com.future.xlink.request.retrofit;

import com.elvishew.xlog.XLog;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author lee
 */
public class SelfObserver<T> implements Observer <T> {
    private Disposable disposable;

    public SelfObserver() {
    }

    @Override
    public void onSubscribe(Disposable d) {
        disposable = d;
    }

    @Override
    public void onNext(T t) {
        dispose();
    }

    @Override
    public void onError(Throwable e) {
        dispose();
    }

    @Override
    public void onComplete() {
        dispose();
    }

    private void dispose() {
        if (disposable != null && disposable.isDisposed()) {
            XLog.d("dispose: dispose()");
            disposable.dispose();
            disposable = null;
        }
    }
}