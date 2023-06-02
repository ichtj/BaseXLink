package com.future.xlink.request;

import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.base.BaseData;
import com.future.xlink.bean.InitParams;
import com.future.xlink.callback.ICmdType;
import com.future.xlink.bean.PutType;
import com.future.xlink.callback.IPutType;
import com.future.xlink.utils.AesTools;
import com.google.gson.Gson;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTransfer {
    /**
     * 获取推送的内容
     *
     * @param clientId 设备SN
     * @param baseData 数据内容
     * @return 需要推送的真实数据
     */
    public static String getPushData(String clientId, BaseData baseData) throws Throwable {
        String requestCmd = null;
        switch (baseData.iPutType) {
            case PutType.EVENT:
                JSONArray eDevices = new JSONArray();
                JSONObject eDevice = new JSONObject();
                eDevice.put("did", clientId);
                eDevice.put("event", baseData.operation);
                if (baseData.maps != null && baseData.maps.size() > 0) {
                    JSONObject eOut = new JSONObject();
                    for (String key : baseData.maps.keySet()) {
                        eOut.put(key, baseData.maps.get(key));
                    }
                    eDevice.put("out", eOut);
                } else {
                    eDevice.put("out", null);
                }
                eDevices.put(eDevice);

                JSONObject ePayload = new JSONObject();
                ePayload.put("devices", eDevices);

                JSONObject eDatas = new JSONObject();
                eDatas.put("payload", ePayload);
                eDatas.put("act", "event");
                eDatas.put("iid", baseData.iid);

                requestCmd = eDatas.toString();
                break;
            case PutType.METHOD:
                JSONArray mActions = new JSONArray();
                JSONObject mAction = new JSONObject();
                mAction.put("_description", "");
                mAction.put("_status", 0);
                if (baseData.maps != null && baseData.maps.size() > 0) {
                    JSONObject mOut = new JSONObject();
                    for (String key : baseData.maps.keySet()) {
                        mOut.put(key, baseData.maps.get(key));
                    }
                    mAction.put("out", mOut);
                } else {
                    mAction.put("out", null);
                }
                mAction.put("did", clientId);
                mAction.put("method", baseData.operation);
                mActions.put(mAction);

                JSONObject mPayload = new JSONObject();
                mPayload.put("action", mActions);

                JSONObject mDatas = new JSONObject();
                mDatas.put("payload", mPayload);
                mDatas.put("act", "cmd-resp");
                mDatas.put("iid", baseData.iid);

                requestCmd = mDatas.toString();
                break;
            case PutType.GETPERTIES:
                if (baseData.maps != null && baseData.maps.size() > 0) {
                    String prid = baseData.maps.get("prid").toString();
                    String sidPrid = baseData.operation + "-" + prid;

                    JSONArray devMsgList = new JSONArray();
                    JSONObject dev = new JSONObject();
                    dev.put("_status", 0);
                    dev.put("_description", "");
                    dev.put(sidPrid + "", baseData.maps.get("value"));
                    devMsgList.put(0, dev);

                    JSONObject devMsg = new JSONObject();
                    devMsg.put(clientId, devMsgList);

                    JSONObject devices_get = new JSONObject();
                    devices_get.put("devices_get", devMsg);

                    JSONObject gDatas = new JSONObject();
                    gDatas.put("act", "cmd-resp");
                    gDatas.put("iid", baseData.iid);
                    gDatas.put("payload", devices_get);
                    requestCmd = gDatas.toString();
                }
                break;
            case PutType.SETPERTIES:
                if (baseData.maps != null && baseData.maps.size() > 0) {
                    String prid = baseData.maps.get("prid").toString();
                    String sidPrid = baseData.operation + "-" + prid;

                    JSONArray setDevList = new JSONArray();
                    JSONObject setDdev = new JSONObject();
                    setDdev.put("_status", 0);
                    setDdev.put("_description", "");
                    setDdev.put(sidPrid + "", baseData.maps.get("value"));
                    setDevList.put(0, setDdev);

                    JSONObject setDdevMsg = new JSONObject();
                    setDdevMsg.put(clientId, setDevList);

                    JSONObject devices_set = new JSONObject();
                    devices_set.put("devices_set", setDdevMsg);

                    JSONObject sDatas = new JSONObject();
                    sDatas.put("act", "cmd-resp");
                    sDatas.put("iid", baseData.iid);
                    sDatas.put("payload", devices_set);
                    requestCmd = sDatas.toString();
                }
                break;
            case PutType.UPLOAD:
                JSONArray uDevices = new JSONArray();
                JSONArray uServices = new JSONArray();
                JSONObject uService = new JSONObject();
                JSONArray uProperties = new JSONArray();
                //开始根据键找值
                JSONObject uPropertie = new JSONObject();
                uPropertie.put("prid", baseData.maps.get("prid"));
                uPropertie.put("value", baseData.maps.get("value"));
                uProperties.put(uPropertie);

                uService.put("properties", uProperties);
                uService.put("sid", baseData.operation);
                uServices.put(uService);

                JSONObject uDevice = new JSONObject();
                uDevice.put("did", clientId);
                uDevice.put("services", uServices);
                uDevices.put(uDevice);

                JSONObject uPayload = new JSONObject();
                uPayload.put("devices", uDevices);

                JSONObject uDatas = new JSONObject();
                uDatas.put("act", "upload");
                uDatas.put("iid", baseData.iid);
                uDatas.put("payload", uPayload);
                requestCmd = uDatas.toString();
                break;
            case PutType.UPGRADE:
                JSONObject upgradeResult = new JSONObject();
                JSONArray upgradeDatas=new JSONArray();
                JSONObject upgrade=new JSONObject();
                upgrade.put("did",clientId);
                upgrade.put("_status",0);
                upgrade.put("_description","");
                upgradeDatas.put(upgrade);

                JSONObject upgradeList=new JSONObject();
                upgradeList.put("upgrade",upgradeDatas);

                upgradeResult.put("payload",upgradeList);
                upgradeResult.put("act","cmd-resp");
                upgradeResult.put("act_alternative","upgrade");
                upgradeResult.put("iid",baseData.iid);
                requestCmd = upgradeResult.toString();
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
        String act = jsonObject.optString("act");
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
    public static BaseData deliveryHandle(String cliendId, MqttMessage msg) throws Throwable {
        JSONObject jsonObject = new JSONObject(msg.toString());
        String act = jsonObject.optString("act");
        String iid = jsonObject.optString("iid");
        if(TextUtils.isEmpty(act)&&TextUtils.isEmpty(iid)){
            act=jsonObject.optString("act_alternative");
        }
        switch (act) {
            case "event":
                JSONObject eventPayload = new JSONObject(jsonObject.getString("payload"));
                JSONArray eventArray = eventPayload.getJSONArray("devices");
                for (int i = 0; i < eventArray.length(); i++) {
                    JSONObject jEvent = eventArray.getJSONObject(i);
                    String event = jEvent.getString("event");
                    String did = jEvent.getString("did");
                    if (did.equals(cliendId)) {
                        Map<String, Object> outMaps = new HashMap<>();
                        if (jEvent.has("out")) {
                            JSONObject jsonOut = jEvent.getJSONObject("out");
                            Iterator<String> outSets = jsonOut.keys();
                            while (outSets.hasNext()) {
                                String key = outSets.next();
                                String value = jsonOut.getString(key);
                                outMaps.put(key, value);
                            }
                        }
                        return new BaseData(PutType.EVENT, iid, event, outMaps);
                    }
                }
                break;
            case "upload":
                JSONObject uploadJson = new JSONObject(jsonObject.getString("payload"));
                JSONArray devicesArray = uploadJson.getJSONArray("devices");
                for (int i = 0; i < devicesArray.length(); i++) {
                    JSONObject jdevice = devicesArray.getJSONObject(i);
                    JSONArray serviceArray = jdevice.getJSONArray("services");
                    String did = jdevice.getString("did");
                    if (did.equals(cliendId)) {
                        for (int j = 0; j < serviceArray.length(); j++) {
                            JSONObject propertiesJson = serviceArray.getJSONObject(j);
                            String sid = propertiesJson.getString("sid");
                            JSONArray valueArray = propertiesJson.getJSONArray("properties");
                            Map<String, Object> maps = new HashMap<>();
                            for (int k = 0; k < valueArray.length(); k++) {
                                JSONObject valueJson = valueArray.getJSONObject(k);
                                String prid = valueJson.optString("prid");
                                String value = valueJson.optString("value");
                                maps.put(prid, value);
                            }
                            return new BaseData(PutType.UPLOAD, iid, sid, maps);
                        }
                    }
                }
                break;
            case "cmd-resp":
                JSONObject payloadJson = jsonObject.getJSONObject("payload");
                if (payloadJson.has("devices_get")) {
                    return propertiesHandle(payloadJson, iid, cliendId, PutType.GETPERTIES);
                } else if (payloadJson.has("devices_set")) {
                    return propertiesHandle(payloadJson, iid, cliendId, PutType.SETPERTIES);
                } else {
                    JSONObject mPayload = jsonObject.getJSONObject("payload");
                    JSONArray mActions = mPayload.getJSONArray("action");
                    for (int i = 0; i < mActions.length(); i++) {
                        JSONObject mAction = mActions.getJSONObject(i);
                        String did = mAction.getString("did");
                        if (did.equals(cliendId)) {
                            String method = mAction.getString("method");
                            Map maps = new HashMap();
                            if (mAction.has("out")) {
                                JSONObject mOut = mAction.getJSONObject("out");
                                Iterator<String> outSets = mOut.keys();
                                while (outSets.hasNext()) {
                                    String key = outSets.next();
                                    String value = mOut.get(key).toString();
                                    maps.put(key, value);
                                }
                            }
                            return new BaseData(PutType.METHOD, iid, method, maps);
                        }
                    }
                }
                break;
            case "upgrade":
                return new BaseData(PutType.UPGRADE,iid,"upgrade",null);
        }
        return null;
    }

    /**
     * 属性 get set处理
     *
     * @param payloadJson 消息内容
     * @param iid         消息标识
     * @param cliendId    唯一id
     * @param iPutType    消息类别
     * @return 封装后的baseData
     * @throws JSONException
     */
    private static BaseData propertiesHandle(JSONObject payloadJson, String iid, String cliendId,
                                             @IPutType int iPutType) throws JSONException {
        JSONObject deviceJson = new JSONObject(
                payloadJson.getString(iPutType == PutType.GETPERTIES ? "devices_get" :
                        "devices_set"));
        Iterator<String> deviceSet = deviceJson.keys();
        while (deviceSet.hasNext()) {
            String clientIdkey = deviceSet.next();
            if (clientIdkey.equals(cliendId)) {
                JSONArray clientIdArray = deviceJson.getJSONArray(cliendId);
                for (int i = 0; i < clientIdArray.length(); i++) {
                    JSONObject jInfo = (JSONObject) clientIdArray.get(i);
                    String sid = "";
                    String prid;
                    Iterator<String> jInfoSet = jInfo.keys();
                    Map<String, Object> maps = new HashMap<>();
                    while (jInfoSet.hasNext()) {
                        String deviceKey = jInfoSet.next();
                        String sidPrid = regular("[0-9]*-[0-9]*", deviceKey);
                        if (!TextUtils.isEmpty(sidPrid)) {
                            String[] sidPridArray = sidPrid.split("-");
                            sid = sidPridArray[0];
                            prid = sidPridArray[1];
                            maps.put(prid, jInfo.get(deviceKey));
                        }
                    }
                    return new BaseData(iPutType, iid, sid, maps);
                }
            }
        }
        return null;
    }

    public static String regular(String regular, String content) {
        Pattern pattern = Pattern.compile(regular);
        Matcher matcher = pattern.matcher(content);
        boolean isFind = matcher.find();
        return isFind ? matcher.group(0) : "";
    }

    /**
     * 平台请求
     */
    public static BaseData IotRequest(String cliendId, JSONObject jHandle, String iid) throws JSONException {
        String inputs = jHandle.getString("inputs");
        JSONObject inHandle = new JSONObject(inputs);
        String intent = inHandle.optString("intent");
        switch (intent) {
            case ICmdType.PLATFORM_METHOD:
                JSONArray mActions = inHandle.getJSONArray("action");
                for (int i = 0; i < mActions.length(); i++) {
                    JSONObject mAction = mActions.getJSONObject(i);
                    String did = mAction.getString("did");
                    if (did.equals(cliendId)) {
                        JSONObject mInMaps = mAction.getJSONObject("in");
                        String method = mAction.getString("method");
                        Iterator<String> mInMapsSet = mInMaps.keys();
                        Map maps = new HashMap();
                        while (mInMapsSet.hasNext()) {
                            String key = mInMapsSet.next();
                            String value = mInMaps.getString(key);
                            maps.put(key, value);
                        }
                        return new BaseData(PutType.METHOD, iid, method, maps);
                    }
                }
                break;
            case ICmdType.PLATFORM_GETPROPERTIES:
                JSONArray getdevices = inHandle.getJSONArray("devices");
                for (int i = 0; i < getdevices.length(); i++) {
                    JSONObject jDevice = getdevices.getJSONObject(i);
                    String did = jDevice.getString("did");
                    if (did.equals(cliendId)) {
                        String serv_pros =
                                jDevice.getString("serv_pros").replace("[\"", "").replace("\"]",
                                        "");
                        String[] servInfo = serv_pros.split("-");
                        Map<String, Object> maps = new HashMap<>();
                        maps.put("prid", servInfo[1]);
                        return new BaseData(PutType.GETPERTIES, iid, servInfo[0], maps);
                    }
                }
                break;
            case ICmdType.PLATFORM_SETPROPERTIES:
                JSONObject jset = new JSONObject(jHandle.getString("inputs"));
                JSONArray jdevices = jset.getJSONArray("devices");
                for (int i = 0; i < jdevices.length(); i++) {
                    JSONObject opera = (JSONObject) jdevices.get(i);
                    String did = opera.getString("did");
                    JSONObject propts = opera.getJSONObject("propts");
                    if (did.equals(cliendId)) {
                        Iterator<String> keys = propts.keys();// jsonObject.keys();
                        Map<String, Object> maps = new HashMap<>();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String[] operationsetInfo = key.split("-");
                            String sid = operationsetInfo[0];
                            String prid = operationsetInfo[1];
                            String value = propts.getString(key);
                            maps.put("prid", prid);
                            maps.put("value", value);
                            return new BaseData(PutType.SETPERTIES, iid, sid, maps);
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
