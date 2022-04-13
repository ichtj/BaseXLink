package com.future.xlink.bean.common;

/**
 * 连接状态
 */
public enum ConnectType {
    CONNECT_SUCCESS         (2000, "连接成功"),
    CONNECT_FAIL            (2001, "代理服务连接失败"),
    CONNECT_NO_NETWORK      (2002, "没有网络信号"),
    RECONNECT_SUCCESS       (2003, "重连成功"),
    CONNECT_RESPONSE_TIMEOUT(2004, "连接服务器无响应"),
    CONNECT_SESSION_ERR     (2005, "连接会话异常"),
    CONNECT_DISCONNECT      (2006, "连接断开"),
    CONNECT_UNINIT          (2007, "连接注销");

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
