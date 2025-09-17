package com.future.xlink.request;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.future.xlink.R;
import com.future.xlink.bean.base.BaseData;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.base.MsgData;
import com.future.xlink.bean.other.Agents;
import com.future.xlink.callback.ICmdType;
import com.future.xlink.callback.IHttpRequest;
import com.future.xlink.callback.IMqttCallback;
import com.future.xlink.callback.IPutType;
import com.future.xlink.utils.FileTools;
import com.future.xlink.request.retrofit.IApis;
import com.future.xlink.utils.GsonTools;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.android.service.MqttService;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import com.future.xlink.utils.NetTools;
import com.future.xlink.utils.Utils;

public class XLink {
    private static final String TAG = XLink.class.getSimpleName();
    private MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    private IMqttCallback iMqttCallback;
    private Context mCtx;
    private String[] pingList;
    private static volatile XLink sInstance;
    private CopyOnWriteArrayList<MsgData> pushMap = new CopyOnWriteArrayList<>();
    private boolean isRunPush = false;
    private boolean isRunConnect = false;
    private InitParams initParams;
    private Thread pushThread;
    private Thread connectThread;
    private final Object connectLock = new Object();

    private static XLink instance() {
        if (sInstance == null) {
            synchronized (XLink.class) {
                if (sInstance == null) {
                    sInstance = new XLink();
                }
            }
        }
        return sInstance;
    }

    public static boolean clearMqttDatabaseFiles(Context context) {
        try {
            File dbDir = context.getDatabasePath("dummy").getParentFile();
            if (dbDir == null || !dbDir.exists()) return true;
            File[] files = dbDir.listFiles((dir, name) -> name.startsWith("mqttAndroidService.db"));
            if (files == null || files.length == 0) return true;
            boolean allDeleted = true;
            for (File file : files) {
                if (!file.delete()) allDeleted = false;
            }
            return allDeleted;
        } catch (Exception e) {
            return false;
        }
    }

    static class MqttContentHandle implements MqttCallbackExtended {
        Context mContext;
        public MqttContentHandle(Context mContext) { this.mContext = mContext; }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            XLog.d("connectComplete: reconnect >> " + reconnect + ",serverURI >>" + serverURI);
            if (instance().iMqttCallback != null) {
                instance().iMqttCallback.connState(true, reconnect ? "reConnect complete" : "connect complete");
            }
            instance().isRunPush = true;
            instance().startPushThread();
        }

        @Override
        public void connectionLost(Throwable cause) {
            boolean isPing = NetTools.checkNet(instance().pingList, mContext);
            XLog.e("connectionLost: isPing >> " + isPing);
            if (instance().iMqttCallback != null) {
                instance().iMqttCallback.connState(false, cause == null ? (isPing ? "other exception" : "network is lost！") : cause.getMessage());
            }
            instance().isRunPush = false;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            instance().platformHandle(message.toString());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                boolean isComplete = token.isComplete();
                BaseData baseData = DataTransfer.deliveryHandle(instance().initParams.clientId, token.getMessage());
                if (instance().iMqttCallback != null) { // 判空保护
                    if (isComplete) {
                        instance().iMqttCallback.pushed(baseData);
                    } else {
                        MqttException throwException = token.getException();
                        instance().iMqttCallback.pushFail(baseData, throwException != null ? throwException.getMessage() : "message push failed!");
                    }
                } else {
                    XLog.e("deliveryComplete>iMqttCallback is null");
                }
            } catch (Throwable e) {
                XLog.e("deliveryComplete>Throwable >> " + e.getMessage());
            }
        }
    }

    private void platformHandle(String msgJson) throws JSONException {
        JSONObject jHandle = new JSONObject(msgJson);
        String act = jHandle.getString("act");
        String iid = jHandle.getString("iid");
        switch (act) {
            case ICmdType.PLATFORM_EVENT:
            case ICmdType.PLATFORM_UPLOAD:
            case ICmdType.PLATFORM_CMD:
                instance().iMqttCallback.iotReplyed(act, iid);
                break;
            default:
                BaseData baseData = DataTransfer.IotRequest(instance().initParams.clientId, jHandle, iid);
                instance().iMqttCallback.msgArrives(baseData);
                break;
        }
    }

    private void startPushThread() {
        if (pushThread == null || !pushThread.isAlive()) {
            pushThread = new Thread(() -> {
                while (isRunPush) {
                    try {
                        if (!pushMap.isEmpty() && isRunPush) {
                            pushData(pushMap.remove(0));
                        } else {
                            Thread.sleep(100);
                        }
                    } catch (Throwable throwable) {
                        XLog.e("startPushThread err>>" + throwable.getMessage());
                    }
                }
            }, "XLink-PushThread");
            pushThread.setDaemon(true);
            pushThread.start();
        }
    }

    private void startConnectThread() {
        if (connectThread == null || !connectThread.isAlive()) {
            isRunConnect = true;
            connectThread = new Thread(() -> {
                try {
                    createConnect();
                } finally {
                    isRunConnect = false;
                }
            }, "XLink-ConnectThread");
            connectThread.setDaemon(true);
            connectThread.start();
        }
    }

    private static void pushData(MsgData msgData) {
        if (!instance().isRunPush) return; // 断开后直接返回
        if (getConnectStatus()) {
            try {
                String pushData = DataTransfer.getPushData(instance().initParams.clientId, msgData);
                String topic = DataTransfer.getDiffTopic(pushData, instance().initParams.clientId, instance().initParams.mqttSsid);
                MqttMessage message = new MqttMessage(pushData.getBytes());
                message.setQos(instance().initParams.qos);
                IMqttDeliveryToken deliveryToken = instance().client.publish(topic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG,"pushData>onSuccess >> " + asyncActionToken.isComplete()+",msgData>>"+pushData);
                    }
                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG,"pushData>onFailure >> " + msgData.isDeliveryed+",msgData>>"+pushData);
                        if (instance().iMqttCallback != null) {
                            instance().iMqttCallback.pushFail(msgData, exception.getMessage());
                        }
                    }
                });
                msgData.pushCount++;
                msgData.pushTime = System.currentTimeMillis();
                deliveryToken.waitForCompletion(1400);
                msgData.isDeliveryed = deliveryToken.isComplete();
            } catch (Throwable e) {
                XLog.e("pushData>Throwable >> " + msgData.toString() + ",errMeg >> " + e.getMessage());
                if (instance().iMqttCallback != null) {
                    instance().iMqttCallback.pushFail(msgData, e.getMessage());
                }
            }
        }
    }

    // 优化：连接加锁，状态判断，连接前后清理数据库，异常时释放资源
    private void createConnect() {
        synchronized (connectLock) {
            try {
                if (client != null && client.isConnected()) {
                    XLog.d("createConnect: already connected");
                    return;
                }
                String tmpDir = mCtx.getFilesDir().getAbsolutePath();
                conOpt = DataTransfer.getConOption(initParams);
                client = new MqttAndroidClient(mCtx, initParams.mqttBroker, initParams.clientId, new MqttDefaultFilePersistence(tmpDir));
                client.setCallback(new MqttContentHandle(mCtx));
                client.connect(conOpt).waitForCompletion();
                XLog.d("createConnect Waiting for connection to finish！");
            } catch (MqttException e) {
                XLog.e("createConnect>MqttException >> ", e);
                disConnect (e.getMessage());
            }
        }
    }

    public static boolean getConnectStatus() {
        return instance().client != null && instance().client.isConnected();
    }

    public static boolean putCmd(@IPutType int iPutType, String iid, String operation, Map<String, Object> dataMap,int _status,String _description) {
        BaseData baseData = new BaseData(iPutType, iid, operation, dataMap);
        if (getConnectStatus()) {
            MsgData msgData = new MsgData(baseData.iPutType, baseData.iid, baseData.operation, baseData.maps, false,_description,_status);
            instance().pushMap.add(msgData);
            return true;
        }
        return false;
    }

    public static boolean putCmd(@IPutType int iPutType, String iid, String operation, Map<String, Object> dataMap) {
        BaseData baseData = new BaseData(iPutType, iid, operation, dataMap);
        if (getConnectStatus()) {
            MsgData msgData = new MsgData(baseData.iPutType, baseData.iid, baseData.operation, baseData.maps, false,"",0);
            instance().pushMap.add(msgData);
            return true;
        }
        return false;
    }

    public static void subscribe(String topicName) {
        try {
            if (getConnectStatus()) {
                instance().client.subscribe(topicName, instance().initParams.qos);
                instance().iMqttCallback.subscribed(topicName);
            } else {
                instance().iMqttCallback.subscribeFail(topicName, "mqtt disconnect");
            }
        } catch (MqttException e) {
            XLog.e("subscribe>MqttException >> " + topicName + ",qos >> " + instance().initParams.qos + ",ReasonCode >> " + e.getReasonCode() + ", errMeg:" + e.getMessage());
            instance().iMqttCallback.subscribeFail(topicName, "ReasonCode >> " + e.getReasonCode() + ", errMeg " + ">> " + e.getMessage());
        }
    }

    public static void connect(Context mCxt, InitParams params, IMqttCallback iMqttCallback) {
        instance().mCtx = mCxt;
        if (!getConnectStatus()) {
            String configFolder = IApis.ROOT + mCxt.getPackageName() + "/" + params.clientId + "/";
            FileTools.initLogFile(configFolder);
            instance().iMqttCallback = iMqttCallback;
            InitParams iRparams = null;
            try {
                iRparams = GsonTools.fromJson(FileTools.readFileData(configFolder + IApis.MY_PROPERTIES), InitParams.class);
            } catch (Throwable e) {
                XLog.e("connect>iRparams >> " + e.getMessage());
            }
            if (iRparams != null && iRparams.checkMqttNotNull()) {
                instance().initParams = iRparams;
                instance().pingList = new String[]{Utils.patternIp(iRparams.mqttBroker)};
                instance().startConnectThread();
            } else {
                instance().initParams = params;
                XLinkHttp.getAgentList(params, new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!TextUtils.isEmpty(jsonData)) {
                            try {
                                Agents agents = GsonTools.fromJson(jsonData, Agents.class);
                                if (agents.servers.size() > 0) {
                                    instance().pingList = new String[agents.servers.size()];
                                    for (int i = 0; i < agents.servers.size(); i++) {
                                        String getUrl = Utils.patternIp(agents.servers.get(i));
                                        instance().pingList[i] = getUrl;
                                    }
                                    String bestUrl = agents.servers.get(0);
                                    if (agents.servers.size() > 1) {
                                        bestUrl = Utils.compare(agents.servers);
                                    }
                                    XLog.d("requestComplete: bestUrl >> " + bestUrl);
                                    XLinkHttp.registerDev(params, bestUrl, new IHttpRequest() {
                                        @Override
                                        public void requestComplete(String jsonData) {
                                            boolean isWrite = FileTools.saveConfig(params, configFolder, jsonData);
                                            if (isWrite) {
                                                instance().startConnectThread();
                                            } else {
                                                instance().iMqttCallback.connState(false, mCxt.getString(R.string.credential_save_err));
                                            }
                                        }
                                        @Override
                                        public void requestErr(int errCode, String description) {
                                            XLog.e("connect>registerDev>requestErr >> " + errCode + ", description >> " + description);
                                            instance().iMqttCallback.connState(false, description);
                                        }
                                    });
                                } else {
                                    instance().iMqttCallback.connState(false, "Agent List size <= 0");
                                }
                            } catch (Throwable e) {
                                XLog.e("connect>getAgentList>Throwable >> " + e.getMessage());
                                instance().iMqttCallback.connState(false, "get Agent list failed!");
                            }
                        } else {
                            XLog.e("connect>getAgentList >> is null!");
                        }
                    }
                    @Override
                    public void requestErr(int errCode, String description) {
                        XLog.e("connect>getAgentList>requestErr >> " + description);
                        instance().iMqttCallback.connState(false, description);
                    }
                });
            }
        }
    }

    public static void unInit(Context mContext) {
        FileTools.delProperties(IApis.ROOT + mContext.getPackageName() + "/" + instance().initParams.clientId + "/" + IApis.MY_PROPERTIES);
        instance().pushMap.clear();
        disConnect();
        XLog.e("unInit >> ");
    }

    public static void disConnect(){
        disConnect ( "Active disconnect" );
    }

    // 在断开连接时清空 pushMap
    private static void disConnect(String reason) {
        synchronized (instance().connectLock) {
            instance().isRunPush = false;
            instance().isRunConnect = false;
            XLog.e("disConnect >> ");

            if (instance().iMqttCallback != null) {
                instance().iMqttCallback.connState(false, reason);
                instance().iMqttCallback = null;
            }

            if (instance().client != null) {
                try {
                    if (instance().client.isConnected()) {
                        try {
                            // 更彻底的断开
                            instance().client.disconnectForcibly(0, 0);
                        } catch (Throwable ignore) {
                            try {
                                instance().client.disconnect();
                            } catch (Throwable ignore2) {}
                        }
                    }
                    try {
                        instance().client.unregisterResources();
                    } catch (Throwable ignore) {}
                    try {
                        instance().client.close();
                    } catch (Throwable ignore) {}
                } catch (Throwable ignore) {
                }
                instance().client = null;
            }

            // 停止 service（放在最后，保证 client 已释放）
            try {
                instance().mCtx.stopService(
                        new Intent(instance().mCtx, MqttService.class)
                );
            } catch (Throwable ignore) {}

            // 清理队列和线程
            instance().pushMap.clear();
            if (instance().pushThread != null && instance().pushThread.isAlive()) {
                instance().pushThread.interrupt();
                instance().pushThread = null;
            }
            if (instance().connectThread != null && instance().connectThread.isAlive()) {
                instance().connectThread.interrupt();
                instance().connectThread = null;
            }
        }
    }

}
