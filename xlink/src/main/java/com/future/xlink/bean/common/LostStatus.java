package com.future.xlink.bean.common;

public class LostStatus {
    private boolean isPing;//网络是否正常
    private String describe="";//描述

    public LostStatus(boolean isPing, String describe) {
        this.isPing = isPing;
        this.describe = describe;
    }

    public boolean isPing() {
        return isPing;
    }

    public void setPing(boolean ping) {
        isPing = ping;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }
}
