package com.future.xlink.bean.properties.upload;

import java.util.List;

public class Udevice {
    public String did;
    public List<Uservice> services;

    public Udevice(String did, List<Uservice> services) {
        this.did = did;
        this.services = services;
    }

    public Udevice() {
    }
}
