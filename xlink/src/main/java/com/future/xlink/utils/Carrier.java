package com.future.xlink.utils;

import io.reactivex.annotations.NonNull;

/**
 * 总线消息载体
 */
public class Carrier {
    private int type;
    private Object obj;

    public Carrier(int type, @NonNull Object obj) {
        this.type = type;
        this.obj = obj;
    }

    public Carrier(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }
}