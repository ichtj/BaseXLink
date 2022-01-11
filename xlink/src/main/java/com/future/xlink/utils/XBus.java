package com.future.xlink.utils;

import android.util.Log;

import com.elvishew.xlog.XLog;

import org.greenrobot.eventbus.EventBus;

public final class XBus {
    private static final String TAG = "XBus";
    public static void post(Carrier event) {
        EventBus.getDefault().post(event);
    }

    public static void register(Object subscriber) {
        XLog.d("register xbus");
        EventBus.getDefault().register(subscriber);
    }

    public static void unregister(Object subscriber) {
        XLog.d("unregister xbus");
        EventBus.getDefault().unregister(subscriber);
    }
}