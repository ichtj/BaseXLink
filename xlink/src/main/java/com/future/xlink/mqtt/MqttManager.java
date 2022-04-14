package com.future.xlink.mqtt;

import android.content.Context;
import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Register;
import com.future.xlink.bean.common.ConnectType;
import com.future.xlink.utils.AESUtils;
import com.future.xlink.utils.Carrier;
import com.future.xlink.utils.GlobalConfig;
import com.future.xlink.utils.PingUtils;
import com.future.xlink.utils.Utils;
import com.future.xlink.utils.XBus;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

/**
 * 管理mqtt的连接,发布,订阅,断开连接, 断开重连等操作
 *
 * @author chtj
 */
public class MqttManager implements MqttCallbackExtended {
    private static MqttManager mInstance = null;
    /**
     * 回调
     */
    private MqttAndroidClient client;
    private MqttConnectOptions conOpt;
    private InitParams params;

    private MqttManager() {
    }


    public static MqttManager getInstance() {
        if (mInstance == null) {
            synchronized (MqttManager.class) {
                if (mInstance == null) {
                    mInstance = new MqttManager();
                }
            }
        }
        return mInstance;
    }

    /**
     * 创建Mqtt 连接
     */
    public void creatNewConnect(Context context, InitParams params) {
        this.params = params;
        Register register = params.getRegister();
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        conOpt = new MqttConnectOptions();
        // 清除缓存
        conOpt.setCleanSession(params.isCleanSession());
        // 设置超时时间，单位：秒
        conOpt.setConnectionTimeout(params.getOutTime());
        // 心跳包发送间隔，单位：秒
        conOpt.setKeepAliveInterval(params.getKeepAliveTime());
        conOpt.setAutomaticReconnect(params.isAutomaticReconnect());
        // 用户名
        XLog.d("getMqttConnectOptions: start>>> key:" + params.getKey() + ",mqttUsername=" + register.mqttUsername + ",mqttPassword=" + register.mqttPassword);
        String userName = AESUtils.decrypt(params.getKey(), register.mqttUsername);
        String pwd = AESUtils.decrypt(params.getKey(), register.mqttPassword);
        //解码凭证是否正常
        if (!TextUtils.isEmpty(userName) && !TextUtils.isEmpty(pwd)) {
            try {
                char[] password = pwd.toCharArray();
                XLog.d("getMqttConnectOptions: decrypt>>> userName:" + userName + ",password=" + pwd);
                conOpt.setUserName(userName);
                conOpt.setPassword(password);
                conOpt.setServerURIs(new String[]{register.mqttBroker});
                conOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
                String clientId = params.getSn();
                client = new MqttAndroidClient(context, register.mqttBroker, clientId, dataStore);
                XLog.d("creatConnect client id=" + client.getClientId() + ",dataStore=" + tmpDir);
                client.setCallback(this);
                connAndListener();
            }catch (Throwable e){
                XLog.e(e);
                XBus.post(new Carrier(GlobalConfig.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_SESSION_ERR));
            }
        } else {
            //凭证异常
            XBus.post(new Carrier(GlobalConfig.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_VOUCHER_ERR));
        }
    }

    /**
     * 连接完成
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        XLog.d("connectComplete reconnect=" + reconnect + ",serverURI=" + serverURI);
        XBus.post(new Carrier(GlobalConfig.TYPE_MODE_CONNECT_RESULT, reconnect ? ConnectType.RECONNECT_SUCCESS : ConnectType.CONNECT_SUCCESS));
    }

    /**
     * 连接丢失
     */
    @Override
    public void connectionLost(Throwable cause) {
        if (cause == null) {
            cause = new Throwable("Other connectionLost exceptions");
        }
        XLog.d("MqttCallback connectionLost ", cause);
        XBus.post(new Carrier(GlobalConfig.TYPE_MODE_CONNECT_LOST, cause));
    }

    /**
     * 这里是消息从平台下发完成
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        XLog.d("messageArrived topic=" + topic + ",message=" + message.toString());
        XBus.post(new Carrier(GlobalConfig.TYPE_REMOTE_RX, topic, message));
    }

    /**
     * 这里是消息交互完成
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            JSONObject tokenMeg = new JSONObject(token.getMessage().toString());
            if (tokenMeg != null) {
                String iid = tokenMeg.getString("iid");
                XLog.d("deliveryComplete token iid=" + iid + ",isComplete=" + token.isComplete());
            }
        } catch (Exception e) {
            XLog.e("deliveryComplete", e);
        }
    }

    /**
     * 建立连接 并监听连接回调
     * 连接结果将在 iMqttActionListener中进行回调 使用旧连接
     */
    public void connAndListener() throws Throwable {
        boolean isConnect = isConnect();
        XLog.d("isConnect=" + isConnect);
        if (!isConnect) {
            IMqttToken itoken = client.connect(conOpt);
            XLog.d("Waiting for connection to finish！");
            //阻止当前线程，直到该令牌关联的操作完成
            itoken.waitForCompletion();
        }
    }

    /**
     * 判断mqtt是否连接完成
     */
    public boolean isConnect() {
        return client != null && client.isConnected();
    }

    /**
     * Publish / send a message to an MQTT server
     *
     * @param topicName the name of the topic to publish to
     * @param qos       the quality of service to delivery the message at (0,1,2)
     * @param payload   the set of bytes to send to the MQTT server
     */
    public void publish(String topicName, int qos, byte[] payload, Context context) {
        try {
            boolean isMqttConnect = isConnect();
            boolean isNetConnect = Utils.isNetConnect(context);
            XLog.d("isMqttConnect=" + isMqttConnect + ",isNetConnect=" + isNetConnect);
            if (isMqttConnect && isNetConnect) {
                // Create and configure a message
                MqttMessage message = new MqttMessage(payload);
                message.setQos(qos);
                client.publish(topicName, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        XLog.d("public message successful");
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        boolean isNetOk = PingUtils.checkNetWork();
                        XLog.e("public message onFailure isNetOk=" + isNetOk + ",exception=" + exception.getMessage());
                    }
                });
            }
        } catch (Throwable e) {
            XLog.e("publish", e);
        }
    }


    /**
     * Subscribe to a topic on an MQTT server
     * <p>
     * Once subscribed this method waits for the messages to arrive from the server
     * <p>
     * that match the subscription. It continues listening for messages until the enter key is
     * <p>
     * pressed.
     *
     * @param topicName to subscribe to (can be wild carded)
     * @param qos       the maximum quality of service to receive messages at for this subscription
     */
    public void subscribe(String topicName, int qos, Context context) {
        if (isConnect() && Utils.isNetConnect(context)) {
            XLog.d("subscribe " + "Subscribing to topic \"" + topicName + "\" qos " + qos);
            try {
                client.subscribe(topicName, qos);
            } catch (Throwable e) {
                XLog.e("subscribe", e);
            }
        }
    }

    /**
     * 断开连接
     */
    public void disConnect() {
        if (client != null) {
            client.setCallback(null);
            XLog.d("release the mqtt connection");
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (Throwable e) {
                XLog.e("disConnect1", e);
            }
            try {
                client.close();
            } catch (Throwable e) {
                XLog.e("disConnect2", e);
            }
            client = null;
        }
    }

}