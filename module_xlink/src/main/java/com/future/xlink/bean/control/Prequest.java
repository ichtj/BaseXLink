package com.future.xlink.bean.control;

import com.future.xlink.bean.base.Mbase;
import com.future.xlink.bean.properties.get.Ginputs;

public class Prequest extends Mbase {
    public String ack;
    public Ginputs inputs;

    public Prequest(String iid, String act, String ack, Ginputs inputs) {
        super(iid, act);
        this.ack = ack;
        this.inputs = inputs;
    }

    public Prequest(String ack, Ginputs inputs) {
        this.ack = ack;
        this.inputs = inputs;
    }

    public Prequest(String iid, String act) {
        super(iid, act);
    }

    public Prequest() {
    }
}
