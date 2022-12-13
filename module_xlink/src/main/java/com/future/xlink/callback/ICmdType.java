package com.future.xlink.callback;

public interface ICmdType {
    /**
     * 平台事件回复
     */
    String PLATFORM_EVENT = "event-resp";
    /**
     * 平台上报属性回复
     */
    String PLATFORM_UPLOAD = "upload-resp";
    /**
     * 平台指令回复
     */
    String PLATFORM_CMD = "cmd-resp";

    /**
     * 平台下发指令--方法
     */
    String PLATFORM_METHOD = "action";
    /**
     * 平台下发指令--获取属性
     */
    String PLATFORM_GETPROPERTIES = "getProperties";
    /**
     * 平台下发指令--设置属性
     */
    String PLATFORM_SETPROPERTIES = "setProperties";
    /**
     * 平台下发指令--固件升级
     */
    String PLATFORM_UPGRADE = "upgrade";
}
