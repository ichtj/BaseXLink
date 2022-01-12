package com.future.xlink.mqtt;


import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Register;
import com.future.xlink.utils.AESUtils;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

/**
 * @author chtj
 */
public class MqConnectionFactory {
    public static MqttConnectOptions getMqttConnectOptions(InitParams params, Register register) {
        MqttConnectOptions conOpt = new MqttConnectOptions();
        try {
            // 清除缓存
            conOpt.setCleanSession(params.cleanSession);
            // 设置超时时间，单位：秒
            conOpt.setConnectionTimeout(params.outTime);
            // 心跳包发送间隔，单位：秒
            conOpt.setKeepAliveInterval(params.keepAliveTime);
            conOpt.setAutomaticReconnect(params.automaticReconnect);
            // 用户名
            XLog.d("getMqttConnectOptions: start>>> key:" + params.key + ",mqttUsername=" + register.mqttUsername + ",mqttPassword=" + register.mqttPassword);
            String userName = AESUtils.decrypt(params.key, register.mqttUsername);
            char[] password = AESUtils.decrypt(params.key, register.mqttPassword).toCharArray();
            XLog.d("getMqttConnectOptions: decrypt>>> userName:" + userName + ",password=" + password);
            conOpt.setUserName(userName);
            conOpt.setPassword(password);
            conOpt.setServerURIs(new String[]{register.mqttBroker});
            conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        } catch (Throwable e) {
            XLog.e(e);
        }
        return conOpt;
    }
}