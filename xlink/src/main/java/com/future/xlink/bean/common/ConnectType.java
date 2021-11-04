package com.future.xlink.bean.common;

/**
 * 连接状态
 */
public enum ConnectType {
    CONNECT_SUCCESS(2000, "连接成功"),
    CONNECT_FAIL(2002, "代理服务连接失败"),
    CONNECT_NO_NETWORK(2003, "没有网络信号"),
    RECONNECT_SUCCESS(2004, "重连成功"),
    CONNECT_RESPONSE_TIMEOUT(2005, "连接服务器无响应"),
    CONNECT_NO_PERMISSION(2006, "无权在此产品连接"),
    CONNECT_DISCONNECT(2007, "连接主动断开");

    int type;
    String value;

    ConnectType(int type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getTye() {
        return type;
    }

}
