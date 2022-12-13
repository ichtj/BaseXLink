package com.future.xlink.bean.properties.get;

public class Gdevice {
    public String did;
    public String intent;
    public String[] serv_pros;

    public Gdevice(String did, String intent, String[] serv_pros) {
        this.did = did;
        this.intent = intent;
        this.serv_pros = serv_pros;
    }

    public Gdevice() {
    }
}
