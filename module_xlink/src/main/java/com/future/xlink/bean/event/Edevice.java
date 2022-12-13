package com.future.xlink.bean.event;

import java.util.Map;

public class Edevice {
    public String did;
    public String event;
    public Map<String,Object> out;

    public Edevice(String did, String event, Map<String, Object> out) {
        this.did = did;
        this.event = event;
        this.out = out;
    }

    public Edevice() {
    }
}
