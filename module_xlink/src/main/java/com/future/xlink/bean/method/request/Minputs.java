package com.future.xlink.bean.method.request;

import java.util.List;

public class Minputs {
    public String intent;
    public List<Maction> action;

    public Minputs(String intent, List<Maction> action) {
        this.intent = intent;
        this.action = action;
    }

}
