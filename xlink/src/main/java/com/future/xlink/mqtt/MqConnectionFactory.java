package com.future.xlink.mqtt;


import android.util.Log;

import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Register;
import com.future.xlink.utils.AESUtils;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class MqConnectionFactory {
    private static final String TAG = "MqConnectionFactory";

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
            //conOpt.setUserName(AESEncrypter.decrypt(register.mqttUsername,null));
            //conOpt.setPassword(AESEncrypter.decrypt(register.mqttPassword,null).toCharArray());     //将字符串转换为字符串数组
            Log.d(TAG, "getMqttConnectOptions: start>>> key:" + params.key + ",mqttUsername=" + register.mqttUsername + ",mqttPassword=" + register.mqttPassword);
            String userName = AESUtils.decrypt(params.key, register.mqttUsername);
            char[] password = AESUtils.decrypt(params.key, register.mqttPassword).toCharArray();
            Log.d(TAG, "getMqttConnectOptions: decrypt>>> userName:" + userName + ",password=" + password);
            conOpt.setUserName(userName);
            conOpt.setPassword(password);
            conOpt.setServerURIs(new String[]{register.mqttBroker});
            conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        } catch (Exception e) {
            Log.d(TAG, "getMqttConnectOptions:", e);
        }
        return conOpt;
    }

    /**
     * @param text      要签名的文本
     * @param secretKey 阿里云 MQ SecretKey
     * @return 加密后的字符串
     * @throws InvalidKeyException
     * @throws NoSuchAlgorithmException
     */
    public static String macSignature(String text, String secretKey) throws InvalidKeyException, NoSuchAlgorithmException {
        //Charset charset = Charset.forName("UTF-8");
        //String algorithm = "HmacSHA1";
        //Mac mac = Mac.getInstance(algorithm);
        //mac.init(new SecretKeySpec(secretKey.getBytes(charset), algorithm));
        //byte[] bytes = mac.doFinal(text.getBytes(charset));
        //String s = new String(Base64.encodeBase64(bytes), charset);
        return null;
    }

}