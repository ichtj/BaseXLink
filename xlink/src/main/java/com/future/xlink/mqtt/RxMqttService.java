package com.future.xlink.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.XLink;
import com.future.xlink.api.SubscriberSingleton;
import com.future.xlink.bean.Constants;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.McuProtocal;
import com.future.xlink.bean.Protocal;
import com.future.xlink.bean.Register;
import com.future.xlink.bean.common.ConnectLostType;
import com.future.xlink.bean.common.ConnectType;
import com.future.xlink.bean.common.InitState;
import com.future.xlink.bean.common.MsgType;
import com.future.xlink.bean.common.RespType;
import com.future.xlink.bean.mqtt.Request;
import com.future.xlink.bean.mqtt.RespStatus;
import com.future.xlink.bean.mqtt.Response;
import com.future.xlink.listener.MessageListener;
import com.future.xlink.utils.Carrier;
import com.future.xlink.utils.GlobalConfig;
import com.future.xlink.utils.GsonUtils;
import com.future.xlink.utils.ObserverUtils;
import com.future.xlink.utils.PingUtils;
import com.future.xlink.utils.PropertiesUtil;
import com.future.xlink.utils.ThreadPool;
import com.future.xlink.utils.XBus;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

import static com.future.xlink.bean.common.ConnectType.CONNECT_NO_NETWORK;


/**
 * mqtt消息推送服务
 *
 * @author chtj
 */

public class RxMqttService extends Service {
    private static final String TAG = "RxMqttService";
    private static final String RESP = "-resp"; //消息回应后缀
    public static final String INIT_PARAM = "initparams";
    private final Object lock = new Object();
    private boolean threadTerminated = false; //线程控制器

    InitParams params = null;
    private MqttManager mqttManager;
    private ConcurrentHashMap<String, McuProtocal> map = new ConcurrentHashMap<String, McuProtocal>(); //消息存储
    private String ssid = null;

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.d("onCreate start service");
        XBus.register(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            XLog.d("onStartCommand map.size=" + map.size());
            params = (InitParams) intent.getSerializableExtra(INIT_PARAM);
            try {
                looperQueen();
                if (TextUtils.isEmpty(params.key) ||
                        TextUtils.isEmpty(params.secret) || TextUtils.isEmpty(params.pdid)) {
                    //判断注册参数是否有误
                    toInit(InitState.INIT_PARAMS_LOST);
                } else {
                    //查看本地文件是否已经记录了注册参数
                    Register register = PropertiesUtil.getProperties(this);
                    if (register.isNull()) {
                        XLog.d("No local registration was detected");
                        //未注册过那么先获取代理服务器列表
                        ObserverUtils.getAgentList(RxMqttService.this, params);
                    } else {
                        XLog.d("this devices has been registered");
                        //直接提示已注册过
                        toInit(InitState.INIT_SUCCESS);
                    }
                }
            } catch (Throwable e) {
                toInit(InitState.INIT_SERVICE_ERR);
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 创建连接
     */
    private void createConect(Register register) throws Throwable {
        mqttManager = MqttManager.getInstance();
        mqttManager.creatNewConnect(RxMqttService.this, params, register);
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Carrier msg) throws MqttException, IOException {
        switch (msg.type) {
            case Carrier.TYPE_MODE_INIT_RX://初始化
                toInit((InitState) msg.obj);
                break;
            case Carrier.TYPE_MODE_TO_CONNECT://执行连接操作
                toConnect();
                break;
            case Carrier.TYPE_MODE_CONNECT_RESULT://连接状态改变
                connStatusChange((ConnectType) msg.obj);
                break;
            case Carrier.TYPE_MODE_CONNECT_LOST://连接丢失
                connectLost((Throwable) msg.obj);
                break;
            case Carrier.TYPE_REMOTE_RX://代理服务器下发消息
                MqttMessage mqttMessage = (MqttMessage) msg.obj;
                String temp = mqttMessage.toString();
                Request request = GsonUtils.fromJson(temp, Request.class);
                parseData(msg.type, request);
                break;
            case Carrier.TYPE_REMOTE_TX_EVENT:
            case Carrier.TYPE_REMOTE_TX_SERVICE:
            case Carrier.TYPE_REMOTE_TX:
                //事件，服务属性上报
                Protocal protocal = (Protocal) msg.obj;
                parseData(msg.type, protocal);
                break;
            default:
                break;
        }
    }

    /**
     * 进行初始化操作
     *
     * @param initState 初始化状态
     */
    public void toInit(InitState initState) {
        XLog.d("onEvent： TYPE_MODE_INIT_RX initState="+initState.getValue());
        //获取初始化参数状态
        if (XLink.getInstance().getListener() != null) {
            XLink.getInstance().getListener().initState(initState);
        }
    }

    /**
     * 去执行连接
     */
    public void toConnect() {
        boolean isConning=MqttManager.getInstance().isConnStatus;
        XLog.d("toConnect: isConning="+isConning);
        if(!isConning){
            //标记正在连接中
            MqttManager.getInstance().isConnStatus=true;
            XLog.d("onEvent： TYPE_MODE_CONNECT");
            map.clear();//创建连接时清除之前的消息队列
            boolean isNetOk = PingUtils.checkNetWork();//判断网络是否正常
            XLog.d("onEvent: isNetOk=" + isNetOk);
            if (!isNetOk) {
                connTypeCallBack(CONNECT_NO_NETWORK);//回调网络不正常
            } else {
                Register register = PropertiesUtil.getProperties(RxMqttService.this);
                try {
                    XLog.d("toConnect: register="+register.toString());
                    createConect(register);
                } catch (Throwable e) {
                    connTypeCallBack(ConnectType.CONNECT_RESPONSE_TIMEOUT);
                }
            }
        }
    }

    /**
     * 连接丢失
     *
     * @param error 连接异常信息
     */
    public void connectLost(Throwable error) {
        XLog.d("onEvent： TYPE_MODE_CONNECT_LOST");
        //连接丢失
        if (params.automaticReconnect) {
            checkReconnect();//开始重连状态检测
        }
        connLostCallBack(ConnectLostType.LOST_TYPE_0, error);
    }

    /**
     * 连接状态，连接结果变更
     *
     * @param type 状态
     *             结果从MqttManager的iMqttActionListener进行回调
     *             主动断开
     */
    public void connStatusChange(ConnectType type) {
        //连接状态
        XLog.d("onEvent： TYPE_MODE_CONNECT_RESULT value=" + type.getValue());
        stopCheckReconnect();//停止网络状态检测
        switch (type) {
            case CONNECT_SUCCESS://连接完成
            case RECONNECT_SUCCESS://重连成功
                subscrible();
                break;
            case CONNECT_DISCONNECT://连接断开
                //断开操作
                if (mqttManager != null) {
                    mqttManager.disConnect();
                    mqttManager=null;
                }
                //连接断开时,清理集合中的消息
                map.clear();
                break;
        }
        //回调结果
        connTypeCallBack(type);
    }

    /**
     * 回调连接的状态
     *
     * @param type 连接状态
     */
    private void connTypeCallBack(ConnectType type) {
        MessageListener listener = XLink.getInstance().getListener();
        if (listener != null) {
            listener.connectState(type);
        }
    }

    /**
     * 回调连接丢失信息
     *
     * @param type  类型
     * @param cause 异常信息
     */
    private void connLostCallBack(ConnectLostType type, Throwable cause) {
        MqttManager.getInstance().isConnStatus=false;
        MessageListener listener = XLink.getInstance().getListener();
        if (listener != null) {//回调连接丢失以及异常信息
            listener.connectionLost(type, cause);
        }
    }

    /**
     * 订阅消息
     */
    private void subscrible() {
        //添加#进行匹配
        mqttManager.subscribe("dev/" + params.sn + "/#", 2);
    }

    /**
     * 解析客户端上报的消息，添加到消息map集合中
     **/
    private synchronized void parseData(int type, Protocal protocal) {
        //消息iid为上传判断
        if (protocal == null || TextUtils.isEmpty(protocal.iid)) {
            //抛出异常消息id为空，空指针异常3
            protocal.rx = GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_IID_LOST.getTye(), RespType.RESP_IID_LOST.getValue()));
            sendTxMsg(protocal);
            return;
        }
        //iid消息重复
        if (TextUtils.isEmpty(protocal.rx) && map.containsKey(protocal.iid)) {
            //抛出异常，消息iid重复发送4
            protocal.rx = GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_IID_REPEAT.getTye(), RespType.RESP_IID_REPEAT.getValue()));
            sendTxMsg(protocal);
            return;
        }

        McuProtocal mcuprotocal;
        if (map.containsKey(protocal.iid)) {
            mcuprotocal = map.get(protocal.iid);
            mcuprotocal.status = mcuprotocal.status + 1;
        } else {
            mcuprotocal = new McuProtocal();
            mcuprotocal.iid = protocal.iid;
            mcuprotocal.time = System.currentTimeMillis();
            mcuprotocal.act = "cmd";//另外新加的参数 回复某种情况下由于未及时回复 而需要回复的情况
            mcuprotocal.ack = "svr/" + Build.SERIAL;//另外新加的参数 回复某种情况下由于未及时回复 而需要回复的情况
        }
        if (type == Carrier.TYPE_REMOTE_TX_SERVICE) {
            mcuprotocal.act = "upload";
            mcuprotocal.ack = MsgType.MSG_PRO.getTye() + "/" + getSsid();
        } else if (type == Carrier.TYPE_REMOTE_TX_EVENT) {
            mcuprotocal.act = "event";
            mcuprotocal.ack = MsgType.MSG_EVENT.getTye() + "/" + getSsid();
        } else if (type == Carrier.TYPE_REMOTE_TX) {
            if (!mcuprotocal.act.contains(RESP)) {
                mcuprotocal.act = mcuprotocal.act + RESP;
            } else if (mcuprotocal.act.contains(RESP)) {
                mcuprotocal.status = mcuprotocal.status + 1;
            }
        }
        mcuprotocal.type = type;
        mcuprotocal.tx = protocal.tx;
        map.put(mcuprotocal.iid, mcuprotocal);
    }

    /**
     * 解析代理服务器下发的消息，添加到消息map集合中
     **/
    private synchronized void parseData(int type, Request request) {
        McuProtocal protocal;
        if (map.containsKey(request.iid)) {
            protocal = map.get(request.iid);
            //如果接收到代理服务端下发的重复数据，还没有处理，需要过滤掉
            if (protocal.tx == null) {
                XLog.d("parseData 重复数据下发-->" + request.iid);
                return;
            }
            protocal.status = protocal.status + 1;
        } else {
            protocal = new McuProtocal();
            protocal.ack = request.ack;
            protocal.iid = request.iid;
            protocal.act = request.act;
            protocal.time = System.currentTimeMillis();
        }
        protocal.type = type;
        String rx = GsonUtils.toJsonWtihNullField(request.inputs);
        if (!TextUtils.isEmpty(request.act) && request.act.contains(RESP)) {
            protocal.rx = GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_SUCCESS.getTye(), RespType.RESP_SUCCESS.getValue()));
        } else {
            protocal.rx = rx;
        }
        map.put(request.iid, protocal);
    }

    /**
     * 开启消息接收线程，轮询消息处理
     */
    private void looperQueen() {
        threadTerminated = false;
        ThreadPool.add(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    while (!threadTerminated) {
                        try {
                            executeQueen();
                            lock.wait(50);
                        } catch (Throwable e) {
                            //数据处理异常5
                            XLog.e(e);
                        }
                    }
                }
            }
        });
    }

    /**
     * 消息中转处理判断
     */
    private void executeQueen() {
        int mapErrCount = 0;//代表map集合中未正常处理的消息错误的计数
        for (Map.Entry<String, McuProtocal> entry : map.entrySet()) {
            McuProtocal protocal = entry.getValue();
            if (protocal.isOverTime()) {
                //超时10分钟,服务器还没有
                if (protocal.type == Carrier.TYPE_REMOTE_RX) {
                    if (protocal.tx == null) {
                        protocal.tx = GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_OUTTIME.getTye(), RespType.RESP_OUTTIME.getValue()));
                    }
                } else if (protocal.type == Carrier.TYPE_REMOTE_TX || protocal.type == Carrier.TYPE_REMOTE_TX_EVENT || protocal.type == Carrier.TYPE_REMOTE_TX_SERVICE) {
                    //告知消息超时，发送消息到代理服务器
                    if (TextUtils.isEmpty(protocal.rx)) {
                        protocal.rx = GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_OUTTIME.getTye(), RespType.RESP_OUTTIME.getValue()));
                    }
                }
                XLog.d("iid=[" + protocal.iid + "],rx=[" + protocal.tx + "];Message processing timeout！");
                //超时两端都需要汇报
                sendTxMsg(protocal);
                sendRxMsg(protocal);
                map.remove(protocal.iid);
            } else {
                if (protocal.status == 0) {
                    //这里只是代表准备进行发送，至于又没有发送成功，不确定
                    judgeMethod(protocal);
                    //这里只是代表已处理
                    protocal.status = protocal.status + 1;
                } else {
                    if (protocal.tx != null) {
                        if (!TextUtils.isEmpty(protocal.rx)) {
                            //这里表示已经接收到了服务器的回复
                            judgeMethod(protocal);
                            map.remove(protocal.iid);
                        }
                        if (protocal.isTimeoutSeconds()) {
                            //判断消息是否超过了2分钟
                            mapErrCount++;
                        }
                    }
                }
            }
        }
        if (mapErrCount >= 5) {
            XLog.d("executeQueen: Heartbeat events failed to be reported for many times！");
            //主动关闭连接
            XLink.getInstance().disconnect();
        }
    }

    private void judgeMethod(McuProtocal protocal) {
        //非超时处理
        if (protocal.type == Carrier.TYPE_REMOTE_TX || protocal.type == Carrier.TYPE_REMOTE_TX_EVENT || protocal.type == Carrier.TYPE_REMOTE_TX_SERVICE) {
            //消息上报
            sendRxMsg(protocal);
        } else if (protocal.type == Carrier.TYPE_REMOTE_RX) {
            //消息接收
            sendTxMsg(protocal);
        }
    }


    /**
     * 获取ssid
     */
    private String getSsid() {
        if (TextUtils.isEmpty(ssid)) {
            PropertiesUtil propertiesUtil = PropertiesUtil.getInstance(RxMqttService.this).init();
            ssid = propertiesUtil.readString(Constants.SSID, "");
        }
        return ssid;
    }

    /**
     * 转发消息到调用端
     */
    private void sendTxMsg(Protocal msg) {
        MessageListener listener = XLink.getInstance().getListener();
        if (listener != null) {
            listener.messageArrived(msg);
        }
    }

    /**
     * 发送消息到服务端
     */
    private void sendRxMsg(McuProtocal msg) {
        try {
            if (mqttManager != null && mqttManager.isConnect()) {
                Response response = new Response();
                response.act = msg.act;
                response.iid = msg.iid;
                response.payload = msg.tx;
                String dataJson = GsonUtils.toJsonWtihNullField(response);
                XLog.d("sendRxMsg: dataJson=" + dataJson);
                mqttManager.publish(msg.ack, 2, dataJson.getBytes());
            }
        } catch (Throwable e) {
            //7消息发送异常
            XLog.e(e);
        }
    }

    int timeout = 0;

    /**
     * 网络重连状态检测
     */
    public void checkReconnect() {
        timeout = 0;//重置记录超时后 网络正常的时间
        int outtime = params.reconnectTime * 60 / 10;//超时时间
        SubscriberSingleton.add(TAG, Observable.interval(1, 10, TimeUnit.SECONDS).
                observeOn(AndroidSchedulers.mainThread()).
                subscribe(aLong -> {
                    boolean isConnect = mqttManager.isConnect();//mqtt连接是断开的
                    boolean isNetOk = PingUtils.checkNetWork();
                    int nowValue = aLong.intValue();//当前的计时
                    if (!isConnect) {
                        ConnectLostType type;
                        if (nowValue == outtime) {
                            //时间刚好达到超时时间
                            XLog.d("checkReconnect: next ");
                            if (isNetOk) {
                                //114能ping通，说明网络通讯正常；
                                boolean state2 = PingUtils.ping(GlobalConfig.HTTP_SERVER);
                                if (state2) {
                                    //网络和代理连接正常，但服务无法正常连接
                                    type = ConnectLostType.LOST_TYPE_1;
                                } else {
                                    //网络正常，代理服务异常2
                                    type = ConnectLostType.LOST_TYPE_2;
                                }
                            } else {
                                //本地网络异常，只记录一次
                                type = ConnectLostType.LOST_TYPE_3;
                            }
                            connLostCallBack(type, new Throwable(type.getValue()));
                        } else if (nowValue > outtime) {
                            //超过指定时间后的处理
                            if (isNetOk) { //网络正常 需要尝试去重连
                                if (timeout != 0 && nowValue >= timeout + (60 / 10)) {
                                    //超过指定重连的时间并且加两分钟还未成功
                                    //那么网络应该正常但是 网络或者连接已经重置
                                    type = ConnectLostType.LOST_TYPE_5;
                                    connLostCallBack(type, new Throwable(type.getValue()));
                                    stopCheckReconnect();
                                    mqttManager.disConnect();
                                    mqttManager=null;
                                } else {
                                    if (timeout == 0) {
                                        //记录当前的超时
                                        timeout = nowValue;
                                        type = ConnectLostType.LOST_TYPE_4;
                                        connLostCallBack(type, new Throwable(type.getValue()));
                                    }
                                }
                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        XLog.e(throwable);
                    }
                }));
    }

    /**
     * 停止网络状态检测
     **/
    private void stopCheckReconnect() {
        //重置记录超时后 网络正常的时间
        timeout = 0;
        XLog.d("stopCheckReconnect map.size=" + map.size());
        SubscriberSingleton.clear(TAG);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            XLog.d("onDestroy stop service");
            XBus.unregister(this);
            map.clear();
            stopCheckReconnect();
            threadTerminated = true;
            mqttManager.disConnect();
            mqttManager = null;
        } catch (Throwable e) {
            //停止服务异常2
            XLog.e(e);
        }
    }
}
