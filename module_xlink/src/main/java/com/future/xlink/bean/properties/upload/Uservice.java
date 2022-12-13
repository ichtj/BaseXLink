package com.future.xlink.bean.properties.upload;

import java.util.List;

public class Uservice {
    public String  sid;
    public List<Uproperties> properties;

    public Uservice(String sid, List<Uproperties> properties) {
        this.sid = sid;
        this.properties = properties;
    }

    public Uservice() {
    }
}
