package com.future.xlink.bean.properties.set;

import java.util.List;

public class Pinputs {
    public List<Sdevice> devices;
    public String intent;

    public Pinputs(List<Sdevice> devices, String intent) {
        this.devices = devices;
        this.intent = intent;
    }

    public Pinputs() {
    }
}
