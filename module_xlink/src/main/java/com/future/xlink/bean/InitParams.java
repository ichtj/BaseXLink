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
    public int qos=0;//0最多送达一次，可能丢失，不持久化，最快，适合非关键数据。重连无旧消息。 QoS 1: 至少送达一次，可能重复，需持久化+确认，默认级别。重连可能有旧消息。QoS 2: 仅送达一次，需持久化+两阶段确认，最可靠但最慢。重连可能有旧消息。

    public boolean autoReConnect=true;
    public boolean cleanSession=false;

    public String pdid;
    public String appKey;
    public String appSecret;

    public String uniqueKey;
    public String uniqueScret;

    public InitParams(String clientId, String mqttBroker, String mqttUsername, String mqttPassword, String mqttSsid, String httpUrl, String httpPort, int qos, boolean autoReConnect, boolean cleanSession, String pdid, String appKey, String appSecret, String uniqueKey, String uniqueScret) {
        this.clientId = clientId;
        this.mqttBroker = mqttBroker;
        this.mqttUsername = mqttUsername;
        this.mqttPassword = mqttPassword;
        this.mqttSsid = mqttSsid;
        this.httpUrl = httpUrl;
        this.httpPort = httpPort;
        this.qos = qos;
        this.autoReConnect = autoReConnect;
        this.cleanSession = cleanSession;
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
