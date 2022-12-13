package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.method.request.Minputs;

public class Mrequest extends Mbase {
    public String ack;
    public Minputs minputs;

    public Mrequest(String iid, String act, String ack, Minputs minputs) {
        super(iid, act);
        this.ack = ack;
        this.minputs = minputs;
    }

    public Mrequest(String ack, Minputs minputs) {
        this.ack = ack;
        this.minputs = minputs;
    }

    public Mrequest(String iid, String act) {
        super(iid, act);
    }

    public Mrequest() {
    }
}

