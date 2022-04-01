package com.future.xlink.bean;

import java.io.Serializable;

import io.reactivex.annotations.NonNull;

/**
 * 初始化参数
 */
public class InitParams implements Serializable {
    /**
     * 服务器地址
     */
    private String httpServer;
    /**
     * 根据后台申请参数匹配生成
     */
    private String token = "";
    /**
     * 是否开启缓存
     */
    private boolean bufferEnable = false;
    /**
     * 是否开启自动重连
     */
    private boolean automaticReconnect = false;
    /**
     * 自动重连模式下超时响应时间，单位：分钟
     * */
    private  int reconnectTime=3;
    /**
     * 是否开启清除缓存
     */
    private boolean cleanSession = true;
    /**
     * 缓存大小
     */
    private int bufferSize = 100;
    /**
     * 超时时间,sdk默认发送超时时间,单位s
     */
    private int outTime = 30;
    /**
     * 信令包发送间隔时长,单位s
     */
    private int keepAliveTime = 60;
    /**
     * 设备唯一编码,可以为工控的IMEI编号，不可为空
     */
    private String sn = "";
    /**
     * 动态分配加密参数，设备后台申请分配，不可为空
     */
    @NonNull
    private String secret = "";
    /**
     * 预留加密算法key，设备后台申请分配，不可为空
     */
    @NonNull
    private String key = "";
    /**
     * 预留加密算法iv_key，设备后台申请分配，不可为空
     */
    private String iv_key = "";
    /**
     * 产品id，设备后台申请分配，不可为空
     */
    @NonNull
    private String pdid = "";

    @NonNull
    private Register register;

    public String getHttpServer() {
        return httpServer;
    }

    public void setHttpServer(String httpServer) {
        this.httpServer = httpServer;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public boolean isBufferEnable() {
        return bufferEnable;
    }

    public void setBufferEnable(boolean bufferEnable) {
        this.bufferEnable = bufferEnable;
    }

    public boolean isAutomaticReconnect() {
        return automaticReconnect;
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public int getReconnectTime() {
        return reconnectTime;
    }

    public void setReconnectTime(int reconnectTime) {
        this.reconnectTime = reconnectTime;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    public int getOutTime() {
        return outTime;
    }

    public void setOutTime(int outTime) {
        this.outTime = outTime;
    }

    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getIv_key() {
        return iv_key;
    }

    public void setIv_key(String iv_key) {
        this.iv_key = iv_key;
    }

    public String getPdid() {
        return pdid;
    }

    public void setPdid(String pdid) {
        this.pdid = pdid;
    }

    public Register getRegister() {
        return register;
    }

    public void setRegister(Register register) {
        this.register = register;
    }
}
