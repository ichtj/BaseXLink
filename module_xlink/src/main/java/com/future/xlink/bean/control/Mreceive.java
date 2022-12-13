package com.future.xlink.bean.control;


import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.method.receive.Mpayload;

public class Mreceive extends Mbase {
    public Mpayload payload;

    public Mreceive(String iid, String act, Mpayload payload) {
        super(iid, act);
        this.payload = payload;
    }

    public Mreceive(Mpayload payload) {
        this.payload = payload;
    }
}
