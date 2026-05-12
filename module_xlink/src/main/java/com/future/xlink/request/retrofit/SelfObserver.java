package com.future.xlink.request.retrofit;

import com.future.xlink.xlog.XLogTools;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author lee
 */
public class SelfObserver<T> implements Observer <T> {
    private static final String TAG = "SelfObserver";
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
        if (disposable != null && !disposable.isDisposed()) {
            XLogTools.d(TAG,"HTTP_DISPOSE");
            disposable.dispose();
            disposable = null;
        }
    }
}
