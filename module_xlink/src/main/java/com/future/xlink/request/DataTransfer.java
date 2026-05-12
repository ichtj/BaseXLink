package com.future.xlink.request;

import android.text.TextUtils;

import com.future.xlink.bean.base.BaseData;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.base.MsgData;
import com.future.xlink.callback.ICmdType;
import com.future.xlink.bean.PutType;
import com.future.xlink.callback.IPutType;
import com.future.xlink.utils.AesTools;
import com.future.xlink.utils.Utils;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataTransfer {
    private static JSONObject optObject(JSONObject parent, String name) {
        JSONObject object = parent.optJSONObject(name);
        if (object != null) {
            return object;
        }
        String value = parent.optString(name);
        if (Utils.isEmpty(value)) {
            return null;
        }
        try {
            return new JSONObject(value);
        } catch (JSONException ignored) {
            return null;
        }
    }

    private static JSONArray optArray(JSONObject parent, String name) {
        JSONArray array = parent.optJSONArray(name);
        if (array != null) {
            return array;
        }
        String value = parent.optString(name);
        if (Utils.isEmpty(value)) {
            return null;
        }
        try {
            return new JSONArray(value);
        } catch (JSONException ignored) {
            return null;
        }
    }

    /**
     * 获取推送的内容
     *
     * @param clientId 设备SN
     * @param baseData 数据内容
     * @return 需要推送的真实数据
     */
    public static String getPushData(String clientId, MsgData baseData) throws Throwable {
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
                mAction.put("_description", baseData._description);
                mAction.put("_status", baseData._status);
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
                    dev.put("_status", baseData._status);
                    dev.put("_description", baseData._description);
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
                    setDdev.put("_status", baseData._status);
                    setDdev.put("_description", baseData._description);
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
                JSONArray upgradeDatas = new JSONArray();
                JSONObject upgrade = new JSONObject();
                upgrade.put("did", clientId);
                upgrade.put("_status", baseData._status);
                upgrade.put("_description", baseData._description);
                upgradeDatas.put(upgrade);

                JSONObject upgradeList = new JSONObject();
                upgradeList.put("upgrade", upgradeDatas);

                upgradeResult.put("payload", upgradeList);
                upgradeResult.put("act", "cmd-resp");
                upgradeResult.put("act_alternative", "upgrade");
                upgradeResult.put("iid", baseData.iid);
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
        String act_alternative = jsonObject.optString("act_alternative");
        act = !Utils.isEmpty(act_alternative) ? act_alternative : act;
        switch (act) {
            case "event":
                JSONObject eventPayload = optObject(jsonObject, "payload");
                if (eventPayload == null) return null;
                JSONArray eventArray = eventPayload.optJSONArray("devices");
                if (eventArray == null) return null;
                for (int i = 0; i < eventArray.length(); i++) {
                    JSONObject jEvent = eventArray.optJSONObject(i);
                    if (jEvent == null) continue;
                    String event = jEvent.optString("event");
                    String did = jEvent.optString("did");
                    if (did.equals(cliendId)) {
                        Map<String, Object> outMaps = new HashMap<>();
                        JSONObject jsonOut = jEvent.optJSONObject("out");
                        if (jsonOut != null) {
                            Iterator<String> outSets = jsonOut.keys();
                            while (outSets.hasNext()) {
                                String key = outSets.next();
                                String value = jsonOut.optString(key);
                                outMaps.put(key, value);
                            }
                        }
                        return new BaseData(PutType.EVENT, iid, event, outMaps);
                    }
                }
                break;
            case "upload":
                JSONObject uploadJson = optObject(jsonObject, "payload");
                if (uploadJson == null) return null;
                JSONArray devicesArray = uploadJson.optJSONArray("devices");
                if (devicesArray == null) return null;
                for (int i = 0; i < devicesArray.length(); i++) {
                    JSONObject jdevice = devicesArray.optJSONObject(i);
                    if (jdevice == null) continue;
                    JSONArray serviceArray = jdevice.optJSONArray("services");
                    String did = jdevice.optString("did");
                    if (did.equals(cliendId)) {
                        if (serviceArray == null) return null;
                        for (int j = 0; j < serviceArray.length(); j++) {
                            JSONObject propertiesJson = serviceArray.optJSONObject(j);
                            if (propertiesJson == null) continue;
                            String sid = propertiesJson.optString("sid");
                            JSONArray valueArray = propertiesJson.optJSONArray("properties");
                            if (valueArray == null) return null;
                            Map<String, Object> maps = new HashMap<>();
                            for (int k = 0; k < valueArray.length(); k++) {
                                JSONObject valueJson = valueArray.optJSONObject(k);
                                if (valueJson == null) continue;
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
                JSONObject payloadJson = optObject(jsonObject, "payload");
                if (payloadJson == null) return null;
                if (payloadJson.has("devices_get")) {
                    return propertiesHandle(payloadJson, iid, cliendId, PutType.GETPERTIES);
                } else if (payloadJson.has("devices_set")) {
                    return propertiesHandle(payloadJson, iid, cliendId, PutType.SETPERTIES);
                } else {
                    JSONArray mActions = payloadJson.optJSONArray("action");
                    if (mActions == null) return null;
                    for (int i = 0; i < mActions.length(); i++) {
                        JSONObject mAction = mActions.optJSONObject(i);
                        if (mAction == null) continue;
                        String did = mAction.optString("did");
                        if (did.equals(cliendId)) {
                            String method = mAction.optString("method");
                            Map maps = new HashMap();
                            JSONObject mOut = mAction.optJSONObject("out");
                            if (mOut != null) {
                                Iterator<String> outSets = mOut.keys();
                                while (outSets.hasNext()) {
                                    String key = outSets.next();
                                    String value = String.valueOf(mOut.opt(key));
                                    maps.put(key, value);
                                }
                            }
                            return new BaseData(PutType.METHOD, iid, method, maps);
                        }
                    }
                }
                break;
            case "upgrade":
                return new BaseData(PutType.UPGRADE, iid, "upgrade", null);
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
        JSONObject deviceJson = optObject(payloadJson,
                iPutType == PutType.GETPERTIES ? "devices_get" : "devices_set");
        if (deviceJson == null) return null;
        Iterator<String> deviceSet = deviceJson.keys();
        while (deviceSet.hasNext()) {
            String clientIdkey = deviceSet.next();
            if (clientIdkey.equals(cliendId)) {
                JSONArray clientIdArray = deviceJson.optJSONArray(cliendId);
                if (clientIdArray == null) return null;
                for (int i = 0; i < clientIdArray.length(); i++) {
                    JSONObject jInfo = clientIdArray.optJSONObject(i);
                    if (jInfo == null) continue;
                    String sid = "";
                    String prid;
                    Iterator<String> jInfoSet = jInfo.keys();
                    Map<String, Object> maps = new HashMap<>();
                    while (jInfoSet.hasNext()) {
                        String deviceKey = jInfoSet.next();
                        String sidPrid = regular("[0-9]*-[0-9]*", deviceKey);
                        if (!Utils.isEmpty(sidPrid)) {
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
        // Platform command payloads are not always stable across versions, so use opt*
        // and return null when required sections are missing instead of throwing.
        JSONObject inHandle = optObject(jHandle, "inputs");
        if (inHandle == null) return null;
        String intent = inHandle.optString("intent");
        switch (intent) {
            case ICmdType.PLATFORM_METHOD:
                JSONArray mActions = inHandle.optJSONArray("action");
                if (mActions == null) return null;
                for (int i = 0; i < mActions.length(); i++) {
                    JSONObject mAction = mActions.optJSONObject(i);
                    if (mAction == null) continue;
                    String did = mAction.optString("did");
                    if (did.equals(cliendId)) {
                        JSONObject mInMaps = mAction.optJSONObject("in");
                        if (mInMaps == null) return null;
                        String method = mAction.optString("method");
                        Iterator<String> mInMapsSet = mInMaps.keys();
                        Map maps = new HashMap();
                        while (mInMapsSet.hasNext()) {
                            String key = mInMapsSet.next();
                            String value = mInMaps.optString(key);
                            maps.put(key, value);
                        }
                        return new BaseData(PutType.METHOD, iid, method, maps);
                    }
                }
                break;
            case ICmdType.PLATFORM_GETPROPERTIES:
                JSONArray getdevices = inHandle.optJSONArray("devices");
                if (getdevices == null) return null;
                for (int i = 0; i < getdevices.length(); i++) {
                    JSONObject jDevice = getdevices.optJSONObject(i);
                    if (jDevice == null) continue;
                    String did = jDevice.optString("did");
                    if (did.equals(cliendId)) {
                        String serv_pros =
                                jDevice.optString("serv_pros").replace("[\"", "").replace("\"]",
                                        "");
                        String[] servInfo = serv_pros.split("-");
                        if (servInfo.length < 2) return null;
                        Map<String, Object> maps = new HashMap<>();
                        maps.put("prid", servInfo[1]);
                        return new BaseData(PutType.GETPERTIES, iid, servInfo[0], maps);
                    }
                }
                break;
            case ICmdType.PLATFORM_SETPROPERTIES:
                JSONArray jdevices = inHandle.optJSONArray("devices");
                if (jdevices == null) return null;
                for (int i = 0; i < jdevices.length(); i++) {
                    JSONObject opera = jdevices.optJSONObject(i);
                    if (opera == null) continue;
                    String did = opera.optString("did");
                    JSONObject propts = opera.optJSONObject("propts");
                    if (did.equals(cliendId)) {
                        if (propts == null) return null;
                        Iterator<String> keys = propts.keys();// jsonObject.keys();
                        Map<String, Object> maps = new HashMap<>();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String[] operationsetInfo = key.split("-");
                            if (operationsetInfo.length < 2) continue;
                            String sid = operationsetInfo[0];
                            String prid = operationsetInfo[1];
                            String value = propts.optString(key);
                            maps.put("prid", prid);
                            maps.put("value", value);
                            return new BaseData(PutType.SETPERTIES, iid, sid, maps);
                        }
                    }
                }
                break;
            case ICmdType.PLATFORM_UPGRADE:
                JSONArray jArray = inHandle.optJSONArray(ICmdType.PLATFORM_UPGRADE);
                if (jArray == null) return null;
                for (int i = 0; i < jArray.length(); i++) {
                    JSONObject jsonObject = jArray.optJSONObject(i);
                    if (jsonObject == null) continue;
                    String jsonDid = jsonObject.optString("did");
                    if (jsonDid.equals(cliendId)) {
                        Iterator<String> keys = jsonObject.keys();// jsonObject.keys();
                        Map maps = new HashMap();
                        while (keys.hasNext()) {
                            String key = keys.next();
                            String value = jsonObject.optString(key);
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
        conOpt.setCleanSession(params.cleanSession);
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(30);
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(60);
        conOpt.setAutomaticReconnect(params.autoReConnect);
        // 用户名
        String userName = AesTools.decrypt(params.appKey, params.mqttUsername);
        String password = AesTools.decrypt(params.appKey, params.mqttPassword);
        //解码凭证是否正常

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
        return Utils.isEmpty(tokenMsg) ? null : "Basic " + tokenMsg;
    }

    /**
     * 创建消息数据唯一编号
     */
    public static String createIID() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
