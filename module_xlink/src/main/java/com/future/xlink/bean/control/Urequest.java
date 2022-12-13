package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.properties.upload.Upayload;

public class Urequest extends Mbase {
    public Upayload payload;

    public Urequest(String iid, String act, Upayload payload) {
        super(iid, act);
        this.payload = payload;
    }

    public Urequest(Upayload payload) {
        this.payload = payload;
    }

    public Urequest(String iid, String act) {
        super(iid, act);
    }

    public Urequest() {
    }
}
