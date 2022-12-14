package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.properties.get.Gpayload;

public class Preceive extends Mbase {
    public Gpayload payload;

    public Preceive(String iid, String act, Gpayload payload) {
        super(iid, act);
        this.payload = payload;
    }

    public Preceive(Gpayload payload) {
        this.payload = payload;
    }
}
