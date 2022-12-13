package com.future.xlink.bean.method.request;

import java.util.Map;

public class Maction extends Mstruct {
    public Map<String,Object> in;
    public Maction(String did, String method, Map<String, Object> in) {
        super(did, method);
        this.in = in;
    }
}
