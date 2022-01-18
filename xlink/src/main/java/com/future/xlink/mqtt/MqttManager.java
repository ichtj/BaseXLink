package com.future.xlink.mqtt;

import android.content.Context;
import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Register;
import com.future.xlink.bean.common.ConnectType;
import com.future.xlink.utils.Carrier;
import com.future.xlink.utils.GlobalConfig;
import com.future.xlink.utils.Utils;
import com.future.xlink.utils.XBus;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

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
    private Context context;
    private InitParams params;
    //连接过程的阻塞
    private boolean isBlockStart = false;
    //是否正在连接中 此时防止重复进行连接操作
    public boolean isConnStatus = false;
    private Disposable mConnDisposable;
    /**
     * 是否初始化重连
     */
    private boolean isInitconnect = false;

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
    public void creatNewConnect(Context context, InitParams params, Register register) throws Throwable {
        this.context = context;
        this.params = params;
        isInitconnect = true;
        String tmpDir = System.getProperty("java.io.tmpdir");
        MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
        conOpt = MqConnectionFactory.getMqttConnectOptions(params, register);
        //解析注册时服务器返回的用户名密码 如果解析异常 ，可能是无权限
        boolean pwdIsNull = conOpt.getPassword() == null || conOpt.getPassword().length == 0;
        if (TextUtils.isEmpty(conOpt.getUserName()) || pwdIsNull) {
            XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_NO_PERMISSION));
            //删除配置文件
            String path = GlobalConfig.SYS_ROOT_PATH + Utils.getPackageName(context) + File.separator + params.sn + File.separator + GlobalConfig.MY_PROPERTIES;
            boolean isDel = new File(path).delete();
            XLog.d("creatConnect: isDel my.properties=" + isDel);
            isConnStatus=false;
            return;
        }
        client = new MqttAndroidClient(context, register.mqttBroker, params.sn, dataStore);
        XLog.d("creatConnect client id=" + client.getClientId() + ",dataStore=" + tmpDir);
        client.setCallback(this);
        connAndListener(context);
    }

    /**
     * 连接完成
     */
    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        XLog.d("connectComplete reconnect ==" + reconnect + "     serverURI==" + serverURI);
        XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, reconnect ? ConnectType.RECONNECT_SUCCESS : ConnectType.CONNECT_SUCCESS));
    }

    /**
     * 连接丢失
     */
    @Override
    public void connectionLost(Throwable cause) {
        Throwable newCause = new Throwable("连接丢失,请尝试重启应用！");
        XLog.d("connectionLost " + ((cause != null) ? cause.getMessage() : newCause.getMessage()));
        XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_LOST, cause != null ? cause : newCause));
    }

    /**
     * 这里是消息从平台下发完成
     */
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        XLog.d("messageArrived topic=" + topic + "====" + message.toString());
        XBus.post(new Carrier(Carrier.TYPE_REMOTE_RX, topic, message));
    }

    /**
     * 这里是消息交互完成
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        try {
            boolean isComplete = token.isComplete();
            XLog.d("deliveryComplete token isComplete=" + isComplete + ",errMeg=" + (isComplete ? "" : token.getException().toString()));
            JSONObject tokenMeg = new JSONObject(token.getMessage().toString());
            if(tokenMeg!=null){
                String iid= tokenMeg.getString("iid");
                XLog.d("deliveryComplete token iid=" + iid+",");
            }
        } catch (Exception e) {
            XLog.e(e);
        }
    }

    /**
     * 建立连接 并监听连接回调
     * 连接结果将在 iMqttActionListener中进行回调 使用旧连接
     */
    public void connAndListener(Context context) throws Throwable {
        if (!isConnect()) {
            IMqttToken itoken = client.connect(conOpt, context, iMqttActionListener);
            XLog.d("Waiting for connection to finish！");
            mConnDisposable = Observable.
                    interval(0, 2, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            //循环检测连接是否完成
                            if (!isBlockStart && isConnect()) {
                                XLog.d("connAndListener Connected to " + client.getServerURI() + " with client ID " + client.getClientId() + " connected==" + isConnect());
                                closeDisposable();
                            } else {
                                if (aLong >= 8) {
                                    XLog.d("The specified connection timeout period is reached");
                                    closeDisposable();
                                    //反馈连接超时
                                    XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_RESPONSE_TIMEOUT));
                                }
                            }
                        }
                    });
            //标记开始阻塞
            isBlockStart = true;
            //阻止当前线程，直到该令牌关联的操作完成
            itoken.waitForCompletion();
            //代表阻塞结束 也能说明连接过程完毕
            isBlockStart = false;
        }
    }

    /**
     * 关闭检测连接阻塞后 的连接状态检测
     */
    public void closeDisposable() {
        mConnDisposable.dispose();
        mConnDisposable = null;
        isConnStatus = false;
    }

    public void doConntect(Context context, InitParams params, Register register) throws Throwable {
        if (client == null) {
            XLog.d("doConntect: client == null");
            //client为空时代表需要重新建立连接
            creatNewConnect(context, params, register);
        } else {
            XLog.d("doConntect: client != null");
            //client不为空时表明 利用原有的连接信息
            this.context = context;
            connAndListener(context);
        }
    }

    /**
     * 判断mqtt是否连接完成
     */
    public boolean isConnect() {
        return client != null && client.isConnected();
    }

    private IMqttActionListener iMqttActionListener = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken arg0) {
            XLog.d("onSuccess connection onSuccess");
            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
            disconnectedBufferOptions.setBufferEnabled(params.bufferEnable);
            disconnectedBufferOptions.setBufferSize(params.bufferSize);
            disconnectedBufferOptions.setPersistBuffer(false);
            disconnectedBufferOptions.setDeleteOldestMessages(false);
            if (client != null) {
                client.setBufferOpts(disconnectedBufferOptions);
            }
        }

        @Override
        public void onFailure(IMqttToken arg0, Throwable arg1) {
            XLog.e("IMqttActionListener onFailure-->" ,arg1);
            if (params.automaticReconnect) {
                //只在客户端主动创建初始化连接时回调
                if (isInitconnect) {
                    if (arg1.getMessage().contains("无权连接")) {
                        try {
                            //1.可能是此设备在其他产品中 2.或者设备已被删除 3.该sn未添加到平台
                            XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_NO_PERMISSION));
                            //删除配置文件
                            String path = GlobalConfig.SYS_ROOT_PATH + Utils.getPackageName(context) + File.separator + params.sn + File.separator + GlobalConfig.MY_PROPERTIES;
                            boolean isDel = new File(path).delete();
                            XLog.d("onFailure: isDel my.properties=" + isDel);
                        } catch (Exception e) {
                            XLog.e("IMqttActionListener ",e);
                        }
                    } else {
                        XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_FAIL));
                    }
                }
            } else {
                XBus.post(new Carrier(Carrier.TYPE_MODE_CONNECT_RESULT, ConnectType.CONNECT_FAIL));
            }
        }
    };

    /**
     * Publish / send a message to an MQTT server
     *
     * @param topicName the name of the topic to publish to
     * @param qos       the quality of service to delivery the message at (0,1,2)
     * @param payload   the set of bytes to send to the MQTT server
     */
    public void publish(String topicName, int qos, byte[] payload) {
        //有消息发送之后，isInitconnect 状态设置为false,系统重连之后不再回调onFailure
        isInitconnect = false;
        if (isConnect()) {
            // Create and configure a message
            MqttMessage message = new MqttMessage(payload);
            message.setQos(qos);
            try {
                client.publish(topicName, message);
            } catch (Throwable e) {
                XLog.e(e);
            }
        } else {
            XLog.d("publish: client == null && isConnected=false || !isConnectIsNormal(context)");
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
    public void subscribe(String topicName, int qos) {
        if (isConnect()) {
            XLog.d("subscribe " + "Subscribing to topic \"" + topicName + "\" qos " + qos);
            try {
                client.subscribe(topicName, qos);
            } catch (Throwable e) {
                XLog.e(e);
            }
        }
    }

    /**
     * 断开连接
     */
    public void disConnect() {
        if (client != null) {
            XLog.d("release the mqtt connection");
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
            } catch (Exception e) {
                XLog.e(e);
            }
            try {
                client.unregisterResources();
                client.close();
            } catch (Throwable e) {
                XLog.e(e);
            }
            client = null;
        }
    }

}