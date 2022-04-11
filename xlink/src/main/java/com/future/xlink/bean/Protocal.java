package com.future.xlink.bean;


import io.reactivex.annotations.NonNull;

public  class Protocal <T>{

    /**
     * 消息id
     * */
    @NonNull
    private String iid; //消息id
    /**
     * 客户端发送给代理服务器的消息json消息串
     * */
    private T tx;
    /**
     * 代理服务器发送给客户端json消息串
     * */
    private String rx;
    /**
     * 超时响应时间,xlink默认最小超时时间为10s,单位 s
     * */
    private  int overtime=0;

    public Protocal(String iid, T tx, String rx, int overtime) {
        this.iid = iid;
        this.tx = tx;
        this.rx = rx;
        this.overtime = overtime;
    }

    public Protocal() {
    }

    public String getIid() {
        return iid;
    }

    public void setIid(String iid) {
        this.iid = iid;
    }

    public T getTx() {
        return tx;
    }

    public void setTx(T tx) {
        this.tx = tx;
    }

    public String getRx() {
        return rx;
    }

    public void setRx(String rx) {
        this.rx = rx;
    }

    public int getOvertime() {
        return overtime;
    }

    public void setOvertime(int overtime) {
        this.overtime = overtime;
    }
}
