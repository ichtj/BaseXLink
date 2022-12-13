package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.properties.set.Spayload;

public class Sreceive extends Mbase {
    public Spayload payload;

    public Sreceive(String iid, String act, Spayload payload) {
        super(iid, act);
        this.payload = payload;
    }

    public Sreceive(Spayload payload) {
        this.payload = payload;
    }

    public Sreceive(String iid, String act) {
        super(iid, act);
    }

    public Sreceive() {
    }
}
