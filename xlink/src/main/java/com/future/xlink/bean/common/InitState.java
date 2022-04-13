package com.future.xlink.bean.common;

/**
 * 初始化结果状态值枚举类型
 **/
public enum InitState {
    INIT_SUCCESS             (3000, "初始化成功"),
    INIT_PARAMS_ERR          (3001, "注册参数有误"),
    INIT_GETAGENT_FAIL       (3002, "获取代理服务器列表失败"),
    INIT_GETAGENT_ERR        (3003, "获取代理服务器列表异常"),
    INIT_REGISTER_AGENT_FAIL (3004, "注册代理服务器失败"),
    INIT_REGISTER_AGENT_ERR  (3005, "注册代理服务器异常"),
    INIT_CACHE_NOEXIST       (3006, "注册成功,但缓存写入失败"),
    INIT_SERVICE_ERR         (3007, "初始化注册服务异常"),
    INIT_DEVICE_NOT_EIXST_ERR(3008, "设备未在平台注册");

    int type;
    String value;


    InitState(int type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public int getTye() {
        return type;
    }


    @Override
    public String toString() {
        return value;
    }
}
