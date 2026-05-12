package com.future.xlink.request;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import com.future.xlink.utils.NetTools;
import com.future.xlink.utils.Utils;
import com.future.xlink.xlog.XLogTools;

public class XLink {
    private static final String TAG = XLink.class.getSimpleName();
    private MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    private IMqttCallback iMqttCallback;
    private Context mCtx;
    private String[] pingList;
    private static volatile XLink sInstance;
    // Android 4.4.2 supports LinkedBlockingQueue. It avoids CopyOnWriteArrayList
    // remove(0) copies and lets the worker block while no message is pending.
    private final LinkedBlockingQueue<MsgData> pushQueue = new LinkedBlockingQueue<>();
    private volatile boolean isRunPush = false;
    private volatile boolean isRunConnect = false;
    private InitParams initParams;
    private Thread pushThread;
    private Thread connectThread;
    private final Object connectLock = new Object();
    private volatile int connectGeneration = 0;
    private CopyOnWriteArrayList<String> replyRecordList = new CopyOnWriteArrayList<>();

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
        if (instance().client != null || instance().isRunConnect || instance().isRunPush) {
            XLogTools.d(TAG,"CLEAR_MQTT_DB_DISCONNECT_FIRST");
            disConnect("Clear mqtt cache");
        }
        try {
            Context appContext = context.getApplicationContext();
            File dbDir = appContext.getDatabasePath("dummy").getParentFile();
            if (dbDir == null || !dbDir.exists()) return true;
            File[] files = dbDir.listFiles((dir, name) -> name.startsWith("mqttAndroidService.db"));
            if (files == null || files.length == 0) return true;
            boolean allDeleted = true;
            for (File file : files) {
                boolean deleted = file.delete();
                XLogTools.d(TAG,"CLEAR_MQTT_DB_FILE name=" + file.getName() + " deleted=" + deleted);
                if (!deleted) allDeleted = false;
            }
            return allDeleted;
        } catch (Exception e) {
            XLogTools.e(TAG,"CLEAR_MQTT_DB_ERROR " + getThrowableMessage(e, "clear mqtt database failed"));
            return false;
        }
    }

    private static String getConfigFolder(Context context, String clientId) {
        return new File(context.getFilesDir(), "xlink/" + context.getPackageName() + "/" + clientId + "/").getAbsolutePath() + File.separator;
    }

    private static void notifyConnState(boolean connected, String reason) {
        IMqttCallback callback = instance().iMqttCallback;
        if (callback != null) {
            callback.connState(connected, reason);
        }
    }

    private static String getThrowableMessage(Throwable throwable, String defaultMessage) {
        if (throwable == null) return defaultMessage;
        String message = throwable.getMessage();
        return Utils.isEmpty(message) ? defaultMessage : message;
    }

    private boolean isActiveConnectGeneration(int generation) {
        return generation == connectGeneration && isRunConnect;
    }

    private boolean isCurrentConnectGeneration(int generation) {
        return generation == connectGeneration;
    }

    private void finishConnectGeneration(int generation) {
        synchronized (connectLock) {
            if (generation == connectGeneration) {
                isRunConnect = false;
            }
        }
    }

    private static void closeMqttClient(MqttAndroidClient mqttClient) {
        if (mqttClient == null) return;
        try {
            if (mqttClient.isConnected()) {
                mqttClient.disconnect().waitForCompletion(1000);
            }
        } catch (Throwable throwable) {
            XLogTools.e(TAG,"MQTT_DISCONNECT_ERROR " + getThrowableMessage(throwable, "disconnect failed"));
        }
        try {
            mqttClient.unregisterResources();
        } catch (Throwable throwable) {
            XLogTools.e(TAG,"MQTT_UNREGISTER_ERROR " + getThrowableMessage(throwable, "unregister failed"));
        }
        try {
            mqttClient.close();
        } catch (Throwable throwable) {
            XLogTools.e(TAG,"MQTT_CLOSE_ERROR " + getThrowableMessage(throwable, "close failed"));
        }
    }

    private boolean shouldDispatchReply(String act, String iid) {
        String key = act + "#" + iid;
        if (replyRecordList.contains(key)) {
            XLogTools.d(TAG,"REPLY_DUP_IGNORE act=" + act + " iid=" + iid);
            return false;
        }
        replyRecordList.add(key);
        if (replyRecordList.size() > 300) {
            replyRecordList.remove(0);
        }
        return true;
    }

    private void onMqttConnected(int generation, boolean reconnect, String source) {
        if (!isCurrentConnectGeneration(generation)) {
            XLogTools.d(TAG,"CONNECT_STALE_IGNORE source=" + source + " gen=" + generation + " currentGen=" + connectGeneration);
            return;
        }
        isRunConnect = false;
        isRunPush = true;
        XLogTools.d(TAG,"CONNECT_READY source=" + source + " reconnect=" + reconnect + " gen=" + generation + " queue=" + pushQueue.size());
        notifyConnState(true, reconnect ? "reConnect complete" : "connect complete");
        startPushThread();
    }

    static class MqttContentHandle implements MqttCallbackExtended {
        Context mContext;
        int generation;
        public MqttContentHandle(Context mContext, int generation) {
            this.mContext = mContext;
            this.generation = generation;
        }

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (!instance().isCurrentConnectGeneration(generation)) {
                XLogTools.d(TAG,"CONNECT_CALLBACK_STALE_IGNORE gen=" + generation + " currentGen=" + instance().connectGeneration);
                return;
            }
            XLogTools.d(TAG,"CONNECT_CALLBACK reconnect=" + reconnect + " uri=" + serverURI);
            instance().onMqttConnected(generation, reconnect, "callback");
        }

        @Override
        public void connectionLost(Throwable cause) {
            boolean isPing = NetTools.checkNet(instance().pingList, mContext);
            XLogTools.e(TAG,"CONNECT_LOST pingOk=" + isPing + " reason=" + (cause == null ? "unknown" : cause.getMessage()));
            notifyConnState(false, cause == null ? (isPing ? "other exception" : "network is lost！") : cause.getMessage());
            instance().isRunPush = false;
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            instance().platformHandle(topic, message.toString());
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                boolean isComplete = token.isComplete();
                BaseData baseData = DataTransfer.deliveryHandle(instance().initParams.clientId, token.getMessage());
                if (baseData == null) {
                    XLogTools.e(TAG,"PUBLISH_DELIVERY_PARSE_EMPTY");
                    return;
                }
                IMqttCallback callback = instance().iMqttCallback;
                if (callback != null) { // 判空保护
                    if (isComplete) {
                        callback.pushed(baseData);
                    } else {
                        MqttException throwException = token.getException();
                        callback.pushFail(baseData, throwException != null ? throwException.getMessage() : "message push failed!");
                    }
                } else {
                    XLogTools.e(TAG,"PUBLISH_CALLBACK_MISSING");
                }
            } catch (Throwable e) {
                XLogTools.e(TAG,"PUBLISH_DELIVERY_ERROR error=" + e.getMessage());
            }
        }
    }

    private void platformHandle(String topic, String msgJson) throws JSONException {
        JSONObject jHandle = new JSONObject(msgJson);
        String act = jHandle.optString("act");
        String iid = jHandle.optString("iid");
        if (Utils.isEmpty(act) || Utils.isEmpty(iid)) {
            XLogTools.e(TAG,"MSG_DROP reason=missing_act_or_iid topic=" + topic);
            return;
        }
        switch (act) {
            case ICmdType.PLATFORM_EVENT:
            case ICmdType.PLATFORM_UPLOAD:
            case ICmdType.PLATFORM_CMD:
                if (!instance().shouldDispatchReply(act, iid)) {
                    break;
                }
                IMqttCallback replyCallback = instance().iMqttCallback;
                if (replyCallback != null) {
                    replyCallback.iotReplyed(act, iid);
                }
                break;
            default:
                BaseData baseData = DataTransfer.IotRequest(instance().initParams.clientId, jHandle, iid);
                if (baseData == null) {
                    XLogTools.e(TAG,"MSG_DROP reason=unsupported_or_invalid_payload act=" + act + " iid=" + iid + " topic=" + topic);
                    break;
                }
                IMqttCallback msgCallback = instance().iMqttCallback;
                if (msgCallback != null) {
                    msgCallback.msgArrives(baseData);
                }
                break;
        }
    }

    private void startPushThread() {
        if (pushThread == null || !pushThread.isAlive()) {
            XLogTools.d(TAG,"PUSH_WORKER_START queue=" + pushQueue.size());
            pushThread = new Thread(() -> {
                while (isRunPush) {
                    try {
                        MsgData msgData = pushQueue.poll(1, TimeUnit.SECONDS);
                        if (msgData != null && isRunPush) {
                            XLogTools.d(TAG,"PUSH_DEQUEUE queueAfter=" + pushQueue.size());
                            pushData(msgData);
                        }
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        XLogTools.d(TAG,"PUSH_WORKER_INTERRUPTED");
                        break;
                    } catch (Throwable throwable) {
                        XLogTools.e(TAG,"PUSH_WORKER_ERROR " + throwable.getMessage());
                    }
                }
            }, "XLink-PushThread");
            pushThread.setDaemon(true);
            pushThread.start();
        } else {
            XLogTools.d(TAG,"PUSH_WORKER_ALREADY_RUNNING queue=" + pushQueue.size());
        }
    }

    private void startConnectThread(int generation) {
        synchronized (connectLock) {
            // 如果 connectThread 还在运行，则不启动新的线程
            if (connectThread == null || !connectThread.isAlive()) {
                isRunConnect = true;
                connectThread = new Thread(() -> {
                    try {
                        createConnect(generation);
                    } finally {
                        finishConnectGeneration(generation);
                    }
                }, "XLink-ConnectThread");
                connectThread.setDaemon(true);
                connectThread.start();
            }
        }
    }

    private static void pushData(MsgData msgData) {
        if (!instance().isRunPush) return; // 断开后直接返回
        if (getConnectStatus()) {
            try {
                String pushData = DataTransfer.getPushData(instance().initParams.clientId, msgData);
                String topic = DataTransfer.getDiffTopic(pushData, instance().initParams.clientId, instance().initParams.mqttSsid);
                XLogTools.d(TAG,"PUBLISH_START iid=" + msgData.iid + " type=" + msgData.iPutType + " op=" + msgData.operation + " topic=" + topic);
                MqttMessage message = new MqttMessage(pushData.getBytes());
                message.setQos(instance().initParams.qos);
                IMqttDeliveryToken deliveryToken = instance().client.publish(topic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        XLogTools.d(TAG,"PUBLISH_OK iid=" + msgData.iid + " complete=" + asyncActionToken.isComplete());
                    }
                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        String errorMessage = getThrowableMessage(exception, "mqtt publish failed");
                        XLogTools.e(TAG,"PUBLISH_FAIL iid=" + msgData.iid + " delivered=" + msgData.isDeliveryed + " error=" + errorMessage);
                        IMqttCallback callback = instance().iMqttCallback;
                        if (callback != null) {
                            callback.pushFail(msgData, errorMessage);
                        }
                    }
                });
                msgData.pushCount++;
                msgData.pushTime = System.currentTimeMillis();
                deliveryToken.waitForCompletion(1400);
                msgData.isDeliveryed = deliveryToken.isComplete();
            } catch (Throwable e) {
                String errorMessage = getThrowableMessage(e, "mqtt publish error");
                XLogTools.e(TAG,"PUBLISH_ERROR iid=" + msgData.iid + " type=" + msgData.iPutType + " op=" + msgData.operation + " error=" + errorMessage);
                IMqttCallback callback = instance().iMqttCallback;
                if (callback != null) {
                    callback.pushFail(msgData, errorMessage);
                }
            }
        }
    }

    // 优化：连接加锁，状态判断，连接前后清理数据库，异常时释放资源
    private void createConnect(int generation) {
        try {
            MqttAndroidClient connectClient;
            synchronized (connectLock) {
                if (!isActiveConnectGeneration(generation)) {
                    XLogTools.d(TAG,"CONNECT_THREAD_STALE_IGNORE gen=" + generation + " currentGen=" + connectGeneration);
                    return;
                }
                // 如果已经连接，则直接返回
                if (client != null && client.isConnected()) {
                    XLogTools.d(TAG,"CONNECT_THREAD_ALREADY_CONNECTED gen=" + generation);
                    return;
                }

                String tmpDir = mCtx.getFilesDir().getAbsolutePath();
                conOpt = DataTransfer.getConOption(initParams);
                client = new MqttAndroidClient(mCtx, initParams.mqttBroker, initParams.clientId, new MqttDefaultFilePersistence(tmpDir));
                client.setCallback(new MqttContentHandle(mCtx, generation));
                connectClient = client;
            }

                // 开始连接
                connectClient.connect(conOpt).waitForCompletion();
                if (!isActiveConnectGeneration(generation) && connectClient.isConnected()) {
                    XLogTools.d(TAG,"CONNECT_STALE_CLOSE gen=" + generation + " currentGen=" + connectGeneration);
                    closeMqttClient(connectClient);
                    return;
                }
                XLogTools.d(TAG,"CONNECT_WAIT_DONE gen=" + generation + " connected=" + connectClient.isConnected());
                if (connectClient.isConnected()) {
                    onMqttConnected(generation, false, "waitForCompletion");
                }
            } catch (MqttException e) {
                XLogTools.e(TAG,"CONNECT_ERROR " + e.getMessage());
                disConnect(e.getMessage());
            } catch (Throwable throwable) {
                XLogTools.e(TAG,"CONNECT_FATAL_ERROR " + throwable.getMessage());
                disConnect(throwable.getMessage());
            }
    }

    public static boolean getConnectStatus() {
        return instance().client != null && instance().client.isConnected();
    }

    public static boolean putCmd(@IPutType int iPutType, String iid, String operation, Map<String, Object> dataMap,int _status,String _description) {
        BaseData baseData = new BaseData(iPutType, iid, operation, dataMap);
        if (getConnectStatus()) {
            MsgData msgData = new MsgData(baseData.iPutType, baseData.iid, baseData.operation, baseData.maps, false,_description,_status);
            instance().pushQueue.offer(msgData);
            XLogTools.d(TAG,"QUEUE_ADD iid=" + iid + " type=" + iPutType + " op=" + operation + " queue=" + instance().pushQueue.size() + " worker=" + instance().isRunPush);
            if (!instance().isRunPush) {
                instance().isRunPush = true;
                instance().startPushThread();
            }
            return true;
        }
        return false;
    }

    public static boolean putCmd(@IPutType int iPutType, String iid, String operation, Map<String, Object> dataMap) {
        BaseData baseData = new BaseData(iPutType, iid, operation, dataMap);
        if (getConnectStatus()) {
            MsgData msgData = new MsgData(baseData.iPutType, baseData.iid, baseData.operation, baseData.maps, false,"",0);
            instance().pushQueue.offer(msgData);
            XLogTools.d(TAG,"QUEUE_ADD iid=" + iid + " type=" + iPutType + " op=" + operation + " queue=" + instance().pushQueue.size() + " worker=" + instance().isRunPush);
            if (!instance().isRunPush) {
                instance().isRunPush = true;
                instance().startPushThread();
            }
            return true;
        }
        return false;
    }

    public static void subscribe(String topicName) {
        try {
            IMqttCallback callback = instance().iMqttCallback;
            if (getConnectStatus()) {
                instance().client.subscribe(topicName, instance().initParams.qos);
                if (callback != null) {
                    callback.subscribed(topicName);
                }
            } else if (callback != null) {
                callback.subscribeFail(topicName, "mqtt disconnect");
            }
        } catch (MqttException e) {
            XLogTools.e(TAG,"SUBSCRIBE_FAIL topic=" + topicName + " qos=" + instance().initParams.qos + " code=" + e.getReasonCode() + " error=" + e.getMessage());
            IMqttCallback callback = instance().iMqttCallback;
            if (callback != null) {
                callback.subscribeFail(topicName, "reasonCode=" + e.getReasonCode() + ", error=" + e.getMessage());
            }
        }
    }

    public static void connect(Context mCxt, InitParams newConfig, IMqttCallback iMqttCallback) {
        instance().mCtx = mCxt.getApplicationContext();
        final int generation;
        synchronized (instance().connectLock) {
            if (instance().client != null && instance().client.isConnected()) {
                XLogTools.d(TAG,"CONNECT_IGNORE_ALREADY_CONNECTED clientId=" + newConfig.clientId);
                instance().iMqttCallback = iMqttCallback;
                notifyConnState(true, "already connected");
                return;
            }
            if (instance().isRunConnect) {
                XLogTools.d(TAG,"CONNECT_IGNORE_IN_PROGRESS clientId=" + newConfig.clientId + " gen=" + instance().connectGeneration);
                instance().iMqttCallback = iMqttCallback;
                notifyConnState(false, "connecting");
                return;
            }
            instance().isRunConnect = true;
            instance().connectGeneration++;
            generation = instance().connectGeneration;
            XLogTools.d(TAG,"CONNECT_START clientId=" + newConfig.clientId + " gen=" + generation);
        }
        if (!getConnectStatus()) {
            String configFolder = getConfigFolder(instance().mCtx, newConfig.clientId);
            FileTools.initLogFile(configFolder);
            instance().iMqttCallback = iMqttCallback;
            InitParams saveConfig = null;
            try {
                saveConfig = GsonTools.fromJson(FileTools.readFileData(configFolder + IApis.MY_PROPERTIES), InitParams.class);
            } catch (Throwable e) {
                XLogTools.e(TAG,"CONFIG_READ_FAIL " + e.getMessage());
            }
            if (InitParams.checkProductIsSame(saveConfig,newConfig)) {
                instance().initParams = saveConfig;
                instance().pingList = new String[]{Utils.patternIp(saveConfig.mqttBroker)};
                instance().startConnectThread(generation);
            } else {
                instance().initParams = newConfig;
                XLinkHttp.getAgentList(newConfig, new IHttpRequest() {
                    @Override
                    public void requestComplete(String jsonData) {
                        if (!instance().isActiveConnectGeneration(generation)) {
                            XLogTools.d(TAG,"AGENT_LIST_STALE_IGNORE gen=" + generation + " currentGen=" + instance().connectGeneration);
                            return;
                        }
                        if (!Utils.isEmpty(jsonData)) {
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
                                    XLogTools.d(TAG,"AGENT_SELECTED url=" + bestUrl + " count=" + agents.servers.size());
                                    XLinkHttp.registerDev(newConfig, bestUrl, new IHttpRequest() {
                                        @Override
                                        public void requestComplete(String jsonData) {
                                            if (!instance().isActiveConnectGeneration(generation)) {
                                                XLogTools.d(TAG,"REGISTER_STALE_IGNORE gen=" + generation + " currentGen=" + instance().connectGeneration);
                                                return;
                                            }
                                            boolean isWrite = FileTools.saveConfig(newConfig, configFolder, jsonData);
                                            if (isWrite) {
                                                instance().startConnectThread(generation);
                                            } else {
                                                instance().finishConnectGeneration(generation);
                                                notifyConnState(false, instance().mCtx.getString(R.string.credential_save_err));
                                            }
                                        }
                                        @Override
                                        public void requestErr(int errCode, String description) {
                                            XLogTools.e(TAG,"REGISTER_FAIL code=" + errCode + " desc=" + description);
                                            instance().finishConnectGeneration(generation);
                                            notifyConnState(false, description);
                                        }
                                    });
                                } else {
                                    instance().finishConnectGeneration(generation);
                                    notifyConnState(false, "Agent List size <= 0");
                                }
                            } catch (Throwable e) {
                                XLogTools.e(TAG,"AGENT_LIST_HANDLE_ERROR " + e.getMessage());
                                instance().finishConnectGeneration(generation);
                                notifyConnState(false, "get Agent list failed!");
                            }
                        } else {
                            XLogTools.e(TAG,"AGENT_LIST_EMPTY_RESPONSE");
                            instance().finishConnectGeneration(generation);
                            notifyConnState(false, "get Agent list is null");
                        }
                    }
                    @Override
                    public void requestErr(int errCode, String description) {
                        XLogTools.e(TAG,"AGENT_LIST_FAIL code=" + errCode + " desc=" + description);
                        instance().finishConnectGeneration(generation);
                        notifyConnState(false, description);
                    }
                });
            }
        }
    }

    public static void unInit(Context mContext) {
        Context appContext = mContext.getApplicationContext();
        if (instance().initParams != null && !Utils.isEmpty(instance().initParams.clientId)) {
            FileTools.delProperties(getConfigFolder(appContext, instance().initParams.clientId) + IApis.MY_PROPERTIES);
        }
        instance().pushQueue.clear();
        disConnect();
        XLogTools.d(TAG,"UNINIT_DONE");
    }

    public static void disConnect(){
        disConnect ( "Active disconnect" );
    }

    // Disconnect must invalidate pending connect/publish work so stale callbacks cannot
    // revive an old MQTT client after the user has switched state quickly.
    private static void disConnect(String reason) {
        synchronized (instance().connectLock) {
            instance().isRunPush = false;
            instance().isRunConnect = false;
            instance().connectGeneration++;
            instance().replyRecordList.clear();
            XLogTools.d(TAG,"DISCONNECT_START");

            IMqttCallback callback = instance().iMqttCallback;
            if (callback != null) {
                callback.connState(false, reason);
                instance().iMqttCallback = null;
            }

            if (instance().client != null) {
                closeMqttClient(instance().client);
                instance().client = null;
            }

            // 停止服务并清理线程
            try {
                instance().mCtx.stopService(new Intent(instance().mCtx, MqttService.class));
            } catch (Throwable ignore) {}

            // Clear queued messages and stop workers on disconnect.
            instance().pushQueue.clear();
            if (instance().pushThread != null && instance().pushThread.isAlive()) {
                instance().pushThread.interrupt();
                instance().pushThread = null;
            }
            if (instance().connectThread != null && instance().connectThread.isAlive()) {
                if (instance().connectThread != Thread.currentThread()) {
                    instance().connectThread.interrupt();
                }
                instance().connectThread = null;  // Explicitly reset the connectThread to null
            }
        }
    }
}
