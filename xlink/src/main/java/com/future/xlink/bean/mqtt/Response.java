package com.future.xlink.bean.mqtt;

/**
 * 响应数据结构体
 * */
public class Response<T> {
    /**
     * 请求端生成的消息事物ID，用于分辨响应结果消息
     * */
    private  String iid;
    /**
     * 说明该消息的动作是命令事物消息
     * */
    private String act;
    /**
     * 响应数据的结果回复
     * */
    private T payload;

    public Response(String iid, String act, T payload) {
        this.iid = iid;
        this.act = act;
        this.payload = payload;
    }

    public Response() {
    }

    public String getIid() {
        return iid;
    }

    public void setIid(String iid) {
        this.iid = iid;
    }

    public String getAct() {
        return act;
    }

    public void setAct(String act) {
        this.act = act;
    }

    public T getPayload() {
        return payload;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
