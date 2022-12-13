package com.future.xlink.bean.event;

import java.util.List;

public class Epayload {
    public List<Edevice> devices;

    public Epayload(List<Edevice> devices) {
        this.devices = devices;
    }

    public Epayload() {
    }
}
