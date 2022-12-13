package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.properties.get.Gpayload;

public class Preceive extends Mbase {
    public Gpayload gpayload;

    public Preceive(String iid, String act, Gpayload gpayload) {
        super(iid, act);
        this.gpayload = gpayload;
    }

    public Preceive(Gpayload gpayload) {
        this.gpayload = gpayload;
    }
}
