package com.future.xlink.bean.common;

public class LostStatus {
    private int type;
    private boolean isPing;//网络是否正常
    private String describe="";//描述

    public LostStatus(int type, boolean isPing, String describe) {
        this.type = type;
        this.isPing = isPing;
        this.describe = describe;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
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
