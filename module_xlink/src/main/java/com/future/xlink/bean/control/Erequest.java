package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.event.Epayload;

public class Erequest extends Mbase {
    public Epayload payload;

    public Erequest(String iid, String act, Epayload payload) {
        super(iid, act);
        this.payload = payload;
    }

    public Erequest(Epayload payload) {
        this.payload = payload;
    }

    public Erequest(String iid, String act) {
        super(iid, act);
    }

    public Erequest() {
    }
}
