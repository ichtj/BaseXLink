package com.future.xlink.bean;

import android.text.TextUtils;

/**
 * 用于设备初始化连接前的配置参数
 */
public class InitParams {
    public String clientId;//
    public String mqttBroker;//
    public String mqttUsername;//
    public String mqttPassword;//
    public String mqttSsid;//
    public String httpUrl;//
    public String httpPort;//

    public boolean autoReConnect=true;

    public String pdid;
    public String appKey;
    public String appSecret;

    public String uniqueKey;
    public String uniqueScret;

    public InitParams(String clientId, String mqttBroker, String mqttUsername, String mqttPassword, String mqttSsid, String httpUrl, String httpPort, boolean autoReConnect, String pdid, String appKey, String appSecret, String uniqueKey, String uniqueScret) {
        this.clientId = clientId;
        this.mqttBroker = mqttBroker;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;
        this.mqttSsid = mqttSsid;
        this.httpUrl = httpUrl;
        this.httpPort = httpPort;
        this.autoReConnect = autoReConnect;
        this.pdid = pdid;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.uniqueKey = uniqueKey;
        this.uniqueScret = uniqueScret;
    }

    public InitParams() {
    }

    public boolean checkMqttNotNull(){
        if(TextUtils.isEmpty(mqttBroker)||TextUtils.isEmpty(mqttPassword)||TextUtils.isEmpty(mqttUsername)){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public String toString() {
        return "InitParams{" +
                "clientId='" + clientId + '\'' +
                ", mqttBroker='" + mqttBroker + '\'' +
                ", mqttUsername='" + mqttUsername + '\'' +
                ", mqttPassword='" + mqttPassword + '\'' +
                ", mqttSsid='" + mqttSsid + '\'' +
                ", httpUrl='" + httpUrl + '\'' +
                ", httpPort='" + httpPort + '\'' +
                ", autoReConnect=" + autoReConnect +
                ", pdid='" + pdid + '\'' +
                ", appKey='" + appKey + '\'' +
                ", appSecret='" + appSecret + '\'' +
                ", uniqueKey='" + uniqueKey + '\'' +
                ", uniqueScret='" + uniqueScret + '\'' +
                '}';
    }
}
