package com.future.xlink.request;

import android.content.Context;
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

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private InitParams initParams;
    private ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(2));

    /**
     * 单例模式
     */
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

    /**
     * Mqtt消息处理 连接 消息下发
     */
    static class MqttContentHandle implements MqttCallbackExtended {
        Context mContext;

        public MqttContentHandle(Context mContext) {
            this.mContext = mContext;
        }

        /**
         * 连接完成
         */
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            XLog.d("connectComplete: reconnect >> " + reconnect + ",serverURI >>" + serverURI);
            if (instance().iMqttCallback != null) {
                instance().iMqttCallback.connState(true, reconnect ? "reConnect complete" : "connect complete");
            }
            instance().isRunPush = true;
            instance().executor.execute(instance()::startPushData);
        }

        /**
         * 连接丢失
         */
        @Override
        public void connectionLost(Throwable cause) {
            boolean isPing = NetTools.checkNet(instance().pingList, mContext);
            XLog.e("connectionLost: isPing >> " + isPing);
            if (instance().iMqttCallback != null) {
                instance().iMqttCallback.connState(false, cause == null ? (isPing ? "other exception" : "network is lost！") : cause.getMessage());
            }
            instance().isRunPush = false;
        }

        /**
         * 服务器消息到达
         */
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            instance().platformHandle(message.toString());
        }


        /**
         * 消息交付完成
         */
        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            try {
                boolean isComplete = token.isComplete();
                BaseData baseData = DataTransfer.deliveryHandle(instance().initParams.clientId, token.getMessage());
                if (isComplete) {
                    instance().iMqttCallback.pushed(baseData);
                } else {
                    MqttException throwException = token.getException();
                    instance().iMqttCallback.pushFail(baseData, throwException != null ? throwException.getMessage() : "message push failed!");
                }
            } catch (Throwable e) {
                XLog.e("deliveryComplete>Throwable >> " + e.getMessage());
            }
        }
    }

    /**
     * 消息处理
     */
    private void platformHandle(String msgJson) throws JSONException {
        JSONObject jHandle = new JSONObject(msgJson);
        String act = jHandle.getString("act");
        String iid = jHandle.getString("iid");
        switch (act) {
            case ICmdType.PLATFORM_EVENT:
            case ICmdType.PLATFORM_UPLOAD:
            case ICmdType.PLATFORM_CMD:
                //服务器回复本机发送的数据
                instance().iMqttCallback.iotReplyed(act, iid);
                break;
            default:
                //服务器请求设备中的相关数据
                BaseData baseData = DataTransfer.IotRequest(instance().initParams.clientId, jHandle, iid);
                instance().iMqttCallback.msgArrives(baseData);
                break;
        }
    }

    /**
     * start push data
     */
    public void startPushData() {
        while (instance().isRunPush) {
            if (instance().pushMap.size() > 0) {
                pushData(instance().pushMap.remove(0));
            }
        }
    }

    /**
     * 处理本地队列未推送的数据
     */
    private static void pushData(MsgData msgData) {
        if (getConnectStatus()) {
            try {
                String pushData = DataTransfer.getPushData(instance().initParams.clientId, msgData);
                String topic = DataTransfer.getDiffTopic(pushData, instance().initParams.clientId, instance().initParams.mqttSsid);
                MqttMessage message = new MqttMessage(pushData.getBytes());
                message.setQos(2);
                IMqttDeliveryToken deliveryToken = instance().client.publish(topic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.d(TAG,"pushData>onSuccess >> " + asyncActionToken.isComplete()+",msgData>>"+pushData);
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.d(TAG,"pushData>onFailure >> " + msgData.isDeliveryed+",msgData>>"+pushData);
                        instance().iMqttCallback.pushFail(msgData, exception.getMessage());
                    }
                });
                msgData.pushCount++;
                msgData.pushTime = System.currentTimeMillis();
                deliveryToken.waitForCompletion(1400);
                msgData.isDeliveryed = deliveryToken.isComplete();
            } catch (Throwable e) {
                XLog.e("pushData>Throwable >> " + msgData.toString() + ",errMeg >> " + e.getMessage());
                instance().iMqttCallback.pushFail(msgData, e.getMessage());
            }
        }
    }

    /**
     * create connect
     */
    private void createConnect() {
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            instance().conOpt = DataTransfer.getConOption(instance().initParams);
            instance().client = new MqttAndroidClient(instance().mCtx, instance().initParams.mqttBroker, instance().initParams.clientId, new MqttDefaultFilePersistence(tmpDir));
            instance().client.setCallback(new MqttContentHandle(instance().mCtx));
            instance().client.connect(instance().conOpt).waitForCompletion();
            XLog.d("createConnect Waiting for connection to finish！");
        } catch (MqttException e) {
            XLog.e("createConnect>MqttException >> ", e);
            switch (e.getReasonCode()) {
                case MqttException.REASON_CODE_CLIENT_CONNECTED:
                    instance().iMqttCallback.connState(true, instance().initParams.mqttBroker);
                    break;
                case MqttException.REASON_CODE_NOT_AUTHORIZED:
                case MqttException.REASON_CODE_SERVER_CONNECT_ERROR:
                    String configFolder = IApis.ROOT + instance().mCtx.getPackageName() + "/" + instance().initParams.clientId + "/";
                    FileTools.delProperties(configFolder + IApis.MY_PROPERTIES);
                    instance().iMqttCallback.connState(false, "ErrCode = " + e.getReasonCode());
                    break;
                default:
                    instance().iMqttCallback.connState(false, e.getMessage());
                    break;
            }
        }
    }

    /**
     * get connect status
     */
    public static boolean getConnectStatus() {
        return instance().client != null && instance().client.isConnected();
    }

    /**
     * add cmd
     *
     * @param iPutType  唯一码
     * @param operation 操作名称
     * @param dataMap   content
     */
    public static boolean putCmd(@IPutType int iPutType, String iid, String operation, Map<String, Object> dataMap) {
        BaseData baseData = new BaseData(iPutType, iid, operation, dataMap);
        if (getConnectStatus()) {
            MsgData msgData = new MsgData(baseData.iPutType, baseData.iid, baseData.operation, baseData.maps, false);
            instance().pushMap.add(msgData);
            return true;
        }
        return false;
    }

    /**
     * subscribe message
     */
    public static void subscribe(String topicName, int qos) {
        try {
            if (getConnectStatus()) {
                instance().client.subscribe(topicName, qos);
                instance().iMqttCallback.subscribed(topicName);
            } else {
                instance().iMqttCallback.subscribeFail(topicName, "mqtt disconnect");
            }
        } catch (MqttException e) {
            XLog.e("subscribe>MqttException >> " + topicName + ",qos >> " + qos + ",ReasonCode >> " + e.getReasonCode() + ", errMeg:" + e.getMessage());
            instance().iMqttCallback.subscribeFail(topicName, "ReasonCode >> " + e.getReasonCode() + ", errMeg " + ">> " + e.getMessage());
        }
    }

    /**
     * read local config and connect mqtt
     */
    public static void connect(Context mCxt, InitParams params, IMqttCallback iMqttCallback) {
        instance().mCtx = mCxt;
        if (!getConnectStatus()) {//判断未连接时进行连接操作
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
                XLog.d("connect: isShutdown >> " + instance().executor.isShutdown());
                instance().executor.execute(instance()::createConnect);
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
                                                instance().createConnect();
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

    /**
     * 解除注册
     *
     * @param mContext 上下文
     */
    public static void unInit(Context mContext) {
        FileTools.delProperties(IApis.ROOT + mContext.getPackageName() + "/" + instance().initParams.clientId + "/" + IApis.MY_PROPERTIES);
        instance().pushMap.clear();
        disConnect();
        XLog.e("unInit >> ");
    }

    /**
     * 断开并重置连接
     */
    public static void disConnect() {
        // 关闭线程池
        instance().isRunPush = false;
        XLog.e("disConnect >> ");
        if (instance().iMqttCallback != null) {
            instance().iMqttCallback.connState(false, "Active disconnect");
        }
        instance().iMqttCallback = null;
        if (instance().client != null) {
            instance().client.setCallback(null);
            try {
                instance().client.disconnect();
            } catch (Throwable e) {
            }
            try {
                instance().client.close();
            } catch (Throwable e) {
            }
            instance().client = null;
        }
        instance().executor.shutdown();
    }
}
