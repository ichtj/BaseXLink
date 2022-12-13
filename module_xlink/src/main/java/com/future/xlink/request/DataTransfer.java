package com.future.xlink.request;

import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.base.BaseData;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.control.Erequest;
import com.future.xlink.bean.control.Mreceive;
import com.future.xlink.bean.control.Urequest;
import com.future.xlink.bean.event.Edevice;
import com.future.xlink.bean.event.Epayload;
import com.future.xlink.bean.method.receive.MactionResult;
import com.future.xlink.bean.method.receive.Mpayload;
import com.future.xlink.bean.method.request.Maction;
import com.future.xlink.bean.method.request.Minputs;
import com.future.xlink.bean.properties.get.Gdevice;
import com.future.xlink.bean.properties.set.Sdevice;
import com.future.xlink.bean.properties.upload.Udevice;
import com.future.xlink.bean.properties.upload.Upayload;
import com.future.xlink.bean.properties.upload.Uproperties;
import com.future.xlink.bean.properties.upload.Uservice;
import com.future.xlink.callback.ICmdType;
import com.future.xlink.bean.PutType;
import com.future.xlink.utils.AesTools;
import com.future.xlink.utils.GsonTools;
import com.google.gson.reflect.TypeToken;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class DataTransfer {
    /**
     * 获取推送的内容
     *
     * @param clientId 设备SN
     * @param baseData 数据内容
     * @return 需要推送的真实数据
     */
    public static String getPushData(String clientId, BaseData baseData) {
        String requestCmd = null;
        switch (baseData.iPutType) {
            case PutType.EVENT:
                List<Edevice> edevices = new ArrayList<>();
                edevices.add(new Edevice(clientId, baseData.operation, baseData.maps));
                requestCmd = GsonTools.toJsonWtihNullField(new Erequest(baseData.iid, "event", new Epayload(edevices)));
                break;
            case PutType.METHOD:
                List<MactionResult> action = new ArrayList<>();
                action.add(new MactionResult(clientId, baseData.operation, "0", "", baseData.maps));
                requestCmd = GsonTools.toJsonWtihNullField(new Mreceive(baseData.iid, "cmd-resp", new Mpayload(action)));
                break;
            case PutType.GETPERTIES:
                break;
            case PutType.SETPERTIES:
                break;
            case PutType.UPLOAD:
                List<Udevice> udevices = new ArrayList<>();
                List<Uservice> uservices = new ArrayList<>();
                List<Uproperties> properties = new ArrayList<>();
                //遍历 baseData map
                Set<String> s1 = baseData.maps.keySet();
                //开始根据键找值
                for (String key : s1) {
                    Object value = baseData.maps.get(key);
                    properties.add(new Uproperties(key, value));
                }
                uservices.add(new Uservice(baseData.operation, properties));
                udevices.add(new Udevice(clientId, uservices));
                Upayload payload = new Upayload(udevices);
                Urequest urequest = new Urequest(baseData.iid, "upload", payload);
                requestCmd = GsonTools.toJsonWtihNullField(urequest);
                break;
        }
        return requestCmd;
    }

    /**
     * 获取不同类型的topic
     *
     * @param pushDataJson 推送内容
     * @param mqttSsid     推送ID
     * @return 具体的主题类型
     */
    public static String getDiffTopic(String pushDataJson, String cliendId, String mqttSsid) throws JSONException {
        String topic;
        JSONObject jsonObject = new JSONObject(pushDataJson);
        String act = jsonObject.getString("act");
        switch (act) {
            case "event":
                topic = "evt/" + mqttSsid;
                break;
            case "upload":
                topic = "up/" + mqttSsid;
                break;
            default:
                topic = "svr/" + cliendId;
                break;
        }
        return topic;
    }


    /**
     * 交付结果处理
     */
    public static BaseData deliveryHandle(String cliendId, MqttMessage msg) throws JSONException {
        JSONObject jsonObject = new JSONObject(msg.toString());
        String act = jsonObject.getString("act");
        String iid = jsonObject.getString("iid");
        switch (act) {
            case "event":
                Epayload epayload = GsonTools.fromJson(jsonObject.getString("payload"), Epayload.class);
                List<Edevice> edevices = epayload.devices;
                for (int i = 0; i < edevices.size(); i++) {
                    String did = edevices.get(i).did;
                    String operation = edevices.get(i).event;
                    Map<String, Object> maps = edevices.get(i).out;
                    if (did.equals(cliendId)) {
                        return new BaseData(PutType.EVENT, iid, operation, maps);
                    }
                }
                break;
            case "upload":
                Upayload upayload = GsonTools.fromJson(jsonObject.getString("payload"), Upayload.class);
                List<Udevice> udevices = upayload.devices;
                for (int i = 0; i < udevices.size(); i++) {
                    if (cliendId.equals(udevices.get(i).did)) {
                        List<Uservice> uservices = udevices.get(i).services;
                        Map<String, Object> uValue = new HashMap<>();
                        for (int j = 0; j < uservices.size(); j++) {
                            List<Uproperties> uproperties = uservices.get(j).properties;
                            for (int k = 0; k < uproperties.size(); k++) {
                                uValue.put(uproperties.get(k).prid, uproperties.get(k).value);
                            }
                        }
                        return new BaseData(PutType.UPLOAD, iid, uservices.get(i).sid, uValue);
                    }
                }
                break;
            case "cmd-resp":
                Mpayload mpayload = GsonTools.fromJson(jsonObject.getString("payload"), Mpayload.class);
                List<MactionResult> action = mpayload.action;
                for (int i = 0; i < action.size(); i++) {
                    String did = action.get(i).did;
                    String operation = action.get(i).method;
                    Map<String, Object> maps = action.get(i).out;
                    if (did.equals(cliendId)) {
                        return new BaseData(PutType.METHOD, iid, operation, maps);
                    }
                }
                break;
        }
        return null;
    }

    /**
     * 平台请求
     */
    public static BaseData IotRequest(String cliendId, JSONObject jHandle, String iid) throws JSONException {
        //String ack = jHandle.has("ack") ? jHandle.getString("ack") : "";
        String inputs = jHandle.getString("inputs");
        JSONObject inHandle = new JSONObject(inputs);
        String intent = inHandle.getString("intent");
        switch (intent) {
            case ICmdType.PLATFORM_METHOD:
                Minputs minputs = GsonTools.fromJson(inputs, Minputs.class);
                List<Maction> mActions = minputs.action;
                for (int i = 0; i < mActions.size(); i++) {
                    String did = mActions.get(i).did;
                    if (did.equals(cliendId)) {
                        String operation = mActions.get(i).method;
                        return new BaseData(PutType.METHOD, iid, operation, mActions.get(i).in);
                    }
                }
                break;
            case ICmdType.PLATFORM_GETPROPERTIES:
                List<Gdevice> gdevList = GsonTools.fromJson(inHandle.getString("devices"),
                        new TypeToken<List<Gdevice>>() {
                        }.getType());
                for (int i = 0; i < gdevList.size(); i++) {
                    String did = gdevList.get(i).did;
                    String[] serv_pros = gdevList.get(i).serv_pros;
                    for (int j = 0; j < serv_pros.length; j++) {
                        String[] operationInfo = serv_pros[j].split("-");
                        String sid = operationInfo[0];
                        String operation = operationInfo[1];
                        Map<String, Object> maps = new HashMap<>();
                        maps.put("prid", operation);
                        if (did.equals(cliendId)) {
                            return new BaseData(PutType.GETPERTIES, iid, sid, maps);
                        }
                    }
                }
                break;
            case ICmdType.PLATFORM_SETPROPERTIES:
                JSONObject jset = new JSONObject(jHandle.getString("inputs"));
                JSONArray jdevices = jset.getJSONArray("devices");
                for (int i = 0; i < jdevices.length(); i++) {
                    JSONObject opera= (JSONObject) jdevices.get(i);
                    String did=opera.getString("did");
                    JSONObject propts=opera.getJSONObject("propts");
                    if (did.equals(cliendId)) {
                        Iterator<String> keys = propts.keys();// jsonObject.keys();
                        Map<String,Object> maps=new HashMap<>();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String[] operationsetInfo=key.split("-");
                            String sid=operationsetInfo[0];
                            String prid=operationsetInfo[1];
                            String value = propts.getString(key);
                            maps.put("prid", prid);
                            maps.put("value", value);
                            return new BaseData(PutType.SETPERTIES, iid,sid , maps);
                        }
                    }
                }
                break;
            case ICmdType.PLATFORM_UPGRADE:
                JSONArray jArray = inHandle.getJSONArray(ICmdType.PLATFORM_UPGRADE);
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject jsonObject = (JSONObject) jArray.get(i);
                    String jsonDid = jsonObject.getString("did");
                    if (jsonDid.equals(cliendId)) {
                        Iterator<String> keys = jsonObject.keys();// jsonObject.keys();
                        Map maps = new HashMap();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String value = jsonObject.getString(key);
                            maps.put(key, value);
                        }
                        return new BaseData(PutType.UPGRADE, iid, ICmdType.PLATFORM_UPGRADE, maps);
                    }
                }
                break;
        }
        return null;
    }

    /**
     * 配置连接参数
     */
    public static MqttConnectOptions getConOption(InitParams params) {
        MqttConnectOptions conOpt = new MqttConnectOptions();
        conOpt.setMaxInflight(1000);
        // 清除缓存
        conOpt.setCleanSession(true);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(30);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(60);
        conOpt.setAutomaticReconnect(params.autoReConnect);
        // 用户名
        XLog.d("createConnect: start>>> key:" + params.appKey + ",mqttUsername=" + params.mqttUsername + ",mqttPassword=" + params.mqttPassword);
        String userName = AesTools.decrypt(params.appKey, params.mqttUsername);
        String password = AesTools.decrypt(params.appKey, params.mqttPassword);
        //解码凭证是否正常

        XLog.d("createConnect: userName >>> " + userName + ",password >> " + password);
        char[] pwd = password.toCharArray();
        conOpt.setUserName(userName);
        conOpt.setPassword(pwd);
        conOpt.setServerURIs(new String[]{params.mqttBroker});
        conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

        return conOpt;
    }


    /**
     * 获取解密后令牌
     */
    public static String getToken(String appKey, String appSecret, String clientId, String time) {
        String tokenMsg = AesTools.encrypt(appKey, clientId + ":" + appSecret + ":" + time);
        return TextUtils.isEmpty(tokenMsg) ? null : "Basic " + tokenMsg;
    }

    /**
     * 创建消息数据唯一编号
     */
    public static String createIID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
