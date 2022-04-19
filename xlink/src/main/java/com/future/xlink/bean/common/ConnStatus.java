package com.future.xlink.bean.common;

public class ConnStatus {
    private long code;//0为正常 非0为其他错误吗
    private String describe="";//描述

    public ConnStatus() {
    }

    public ConnStatus(long code, String describe) {
        this.code = code;
        this.describe = describe;
    }
    public void setCode(long code) {
        this.code = code;
    }

    public long getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getDescribe() {
        return describe;
    }

    public void setDescribe(String describe) {
        this.describe = describe;
    }

    @Override
    public String toString() {
        return "Results{" +
                "code=" + code +
                ", describe='" + describe + '\'' +
                '}';
    }
}
