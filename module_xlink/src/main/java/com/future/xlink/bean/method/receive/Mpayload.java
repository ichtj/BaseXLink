package com.future.xlink.bean.method.receive;

import java.util.List;

public class Mpayload {
    public List<MactionResult> action;

    public Mpayload(List<MactionResult> action) {
        this.action = action;
    }

    public Mpayload() {
    }
}
