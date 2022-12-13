package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.properties.set.Pinputs;

public class Srequest extends Mbase {
    public String ack;
    public Pinputs inputs;

    public Srequest(String iid, String act, String ack, Pinputs inputs) {
        super(iid, act);
        this.ack = ack;
        this.inputs = inputs;
    }

    public Srequest(String ack, Pinputs inputs) {
        this.ack = ack;
        this.inputs = inputs;
    }

    public Srequest(String iid, String act) {
        super(iid, act);
    }

    public Srequest() {
    }
}
