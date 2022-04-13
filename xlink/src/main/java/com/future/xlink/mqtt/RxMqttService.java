package com.future.xlink.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.XLink;
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
import com.future.xlink.utils.ThreadPool;
import com.future.xlink.utils.Utils;
import com.future.xlink.utils.XBus;
import com.google.gson.JsonSyntaxException;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.future.xlink.bean.common.ConnectType.CONNECT_NO_NETWORK;


/**
 * mqtt消息推送服务
 *
 * @author chtj
 */

public class RxMqttService extends Service {
    private static final String RESP = "-resp"; //消息回应后缀
    public static final String INIT_PARAM = "initparams";
    private final Object lock = new Object();
    InitParams customParams = null;
    private ConcurrentHashMap<String, McuProtocal> map = new ConcurrentHashMap<String, McuProtocal>(); //消息存储

    @Override
    public void onCreate() {
        super.onCreate();
        XLog.d("start service and messageHandlerThread start");
        XBus.register(this);
    }

    /**
     * 消息处理线程
     */
    class MessageHandlerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                synchronized (lock) {
                    try {
                        executeQueen();
                        lock.wait(55);
                    } catch (Throwable e) {
                        //数据处理异常5
                        XLog.e("looperQueen", e);
                    }
                }
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        XLog.d("onStartCommand map.size=" + map.size());
        if (intent != null) {
            boolean isThreadEnd = ThreadPool.isTaskEnd();
            XLog.d("isThreadEnd=" + isThreadEnd);
            if (isThreadEnd) {
                ThreadPool.execute(new MessageHandlerThread());
            }
            //获取之前保存的配置参数 旧文件 和 新文件
            InitParams localParams = null;
            try {
                customParams = (InitParams) intent.getSerializableExtra(INIT_PARAM);
                String readProperties = Utils.readFileData(customParams.getConfigPath());
                localParams = GsonUtils.fromJson(readProperties, InitParams.class);
            } catch (Throwable e) {
                XLog.e("initedParams errMeg", e);
            }
            if (localParams != null) {
                customParams = localParams;
                //代表已经注册过 那么直接提示注册成功 但这里也会有另一个问题 参数是否会过期 多次次连接失败是否应该重置该参数
                XBus.post(new Carrier(GlobalConfig.TYPE_MODE_INIT_RX, InitState.INIT_SUCCESS));
            } else {
                //代表未注册过
                if (customParams == null && Utils.checkIsNull(customParams.getKey(),
                        customParams.getSecret(), customParams.getPdid(), customParams.getConfigPath())) {
                    //判断注册参数是否有误
                    XLink.initState(InitState.INIT_PARAMS_ERR);
                } else {
                    ObserverUtils.getAgentList(customParams);
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 创建连接
     */
    private void createConect() throws Throwable {
        XLog.d("createConect: params.register=" + customParams.getRegister().toString());
        MqttManager.getInstance().creatNewConnect(RxMqttService.this, customParams);
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onEvent(Carrier msg) throws MqttException, IOException {
        switch (msg.getType()) {
            case GlobalConfig.TYPE_MODE_INIT_RX://初始化
                XLink.initState((InitState) msg.getObj());
                break;
            case GlobalConfig.TYPE_MODE_TO_CONNECT://执行连接操作
                toConnect();
                break;
            case GlobalConfig.TYPE_MODE_CONNECT_RESULT://连接状态改变
                connStatusChange((ConnectType) msg.getObj());
                break;
            case GlobalConfig.TYPE_MODE_CONNECT_LOST://连接丢失
                boolean isNetwork=PingUtils.checkNetWork();
                if(isNetwork){
                    //访问外网正常,尝试访问mqtt服务端是否正常
                    String ip=Utils.patternIp(customParams.getRegister().mqttBroker);
                    //远程服务器ping结果
                    boolean remoteServicePing=PingUtils.ping(ip,2,200);
                    if(remoteServicePing){
                        XLink.connectionLost(ConnectLostType.LOST_TYPE_1, (Throwable) msg.getObj());
                    }else{
                        XLink.connectionLost(ConnectLostType.LOST_TYPE_2, (Throwable) msg.getObj());
                    }
                }else{
                    //访问外网异常
                    XLink.connectionLost(ConnectLostType.LOST_TYPE_3, (Throwable) msg.getObj());
                }
                //XLink.connectionLost(ConnectLostType.LOST_TYPE_0, (Throwable) msg.getObj());
                break;
            case GlobalConfig.TYPE_REMOTE_RX://代理服务器下发消息
                MqttMessage mqttMessage = (MqttMessage) msg.getObj();
                arriveMsgToMap(msg.getType(), GsonUtils.fromJson(mqttMessage.toString(), Request.class));
                break;
            case GlobalConfig.TYPE_REMOTE_TX_EVENT:
            case GlobalConfig.TYPE_REMOTE_TX_SERVICE:
            case GlobalConfig.TYPE_REMOTE_TX:
                reportMsgToMap(msg.getType(), (Protocal) msg.getObj());
                break;
            default:
                break;
        }
    }

    /**
     * 去执行连接
     */
    public void toConnect() {
        boolean isNetOk = PingUtils.checkNetWork();//判断网络是否正常
        XLog.d("onEvent:TYPE_MODE_CONNECT isNetOk=" + isNetOk);
        map.clear();//创建连接时清除之前的消息队列
        if (!isNetOk) {
            XLink.connectState(CONNECT_NO_NETWORK);//回调网络不正常
        } else {
            try {
                createConect();
            } catch (Throwable e) {
                XLink.connectState(ConnectType.CONNECT_RESPONSE_TIMEOUT);
            }
        }
    }

    /**
     * 连接状态，连接结果变更
     *
     * @param type 状态
     *             结果从MqttManager的iMqttActionListener进行回调
     *             主动断开
     */
    public void connStatusChange(ConnectType type) {
        XLog.d("onEvent： TYPE_MODE_CONNECT_RESULT value=" + type.getValue());
        switch (type) {
            case CONNECT_SUCCESS://连接完成
            case RECONNECT_SUCCESS://重连成功
                MqttManager.getInstance().
                        subscribe("dev/" + customParams.getSn() + "/#", 2, this);
                break;
            case CONNECT_DISCONNECT://连接断开
                map.clear();
                MqttManager.getInstance().disConnect();
                break;
            case CONNECT_UNINIT:
                //删除原先保存的配置文件,解除旧的连接信息
                map.clear();
                GlobalConfig.delProperties(customParams.getConfigPath());
                MqttManager.getInstance().disConnect();
                break;
        }
        //回调结果
        XLink.connectState(type);
    }

    /**
     * 解析客户端上报的消息，添加到消息map集合中
     **/
    private synchronized void reportMsgToMap(int type, Protocal protocal) {
        //消息iid为上传判断
        if (protocal == null || TextUtils.isEmpty(protocal.getIid())) {
            //抛出异常消息id为空，空指针异常3
            protocal.setRx(GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_IID_LOST.getTye(), RespType.RESP_IID_LOST.getValue())));
            XLink.msgCallBack(protocal);
            return;
        }
        //iid消息重复
        if (TextUtils.isEmpty(protocal.getRx()) && map.containsKey(protocal.getIid())) {
            //抛出异常，消息iid重复发送4
            protocal.setRx(GsonUtils.toJsonWtihNullField(new RespStatus(RespType.RESP_IID_REPEAT.getTye(), RespType.RESP_IID_REPEAT.getValue())));
            XLink.msgCallBack(protocal);
            return;
        }

        McuProtocal mcuprotocal;
        if (map.containsKey(protocal.getIid())) {
            mcuprotocal = map.get(protocal.getIid());
            mcuprotocal.setStatus(mcuprotocal.getStatus() + 1);
        } else {
            mcuprotocal = new McuProtocal();
            mcuprotocal.setIid(protocal.getIid());
            mcuprotocal.setTime(System.currentTimeMillis());
            mcuprotocal.setAct("cmd");//另外新加的参数 回复某种情况下由于未及时回复 而需要回复的情况
            mcuprotocal.setAck("svr/" + customParams.getSn());//另外新加的参数 回复某种情况下由于未及时回复 而需要回复的情况
        }
        switch (type) {
            case GlobalConfig.TYPE_REMOTE_TX_SERVICE://服务属性上报
                mcuprotocal.setAct("upload");
                mcuprotocal.setAck(MsgType.MSG_PRO.getTye() + "/" + getSsid());
                break;
            case GlobalConfig.TYPE_REMOTE_TX_EVENT://事件上报
                mcuprotocal.setAct("event");
                mcuprotocal.setAck(MsgType.MSG_EVENT.getTye() + "/" + getSsid());
                break;
            case GlobalConfig.TYPE_REMOTE_TX://消息上报
                if (!mcuprotocal.getAct().contains(RESP)) {
                    mcuprotocal.setAct(mcuprotocal.getAct() + RESP);
                } else if (mcuprotocal.getAct().contains(RESP)) {
                    mcuprotocal.setStatus(mcuprotocal.getStatus() + 1);
                }
                break;
        }
        mcuprotocal.setType(type);
        mcuprotocal.setTx(protocal.getTx());
        map.put(mcuprotocal.getIid(), mcuprotocal);
    }

    /**
     * 解析代理服务器下发的消息，添加到消息map集合中
     **/
    private synchronized void arriveMsgToMap(int type, Request request) {
        McuProtocal protocal;
        if (map.containsKey(request.iid)) {
            protocal = map.get(request.iid);
            //如果接收到代理服务端下发的重复数据，还没有处理，需要过滤掉
            if (protocal.getTx() == null) {
                XLog.d("arriveMsgToMap 重复数据下发-->" + request.iid);
                return;
            }
            protocal.setStatus(protocal.getStatus() + 1);
        } else {
            protocal = new McuProtocal();
            protocal.setAck(request.ack);
            protocal.setIid(request.iid);
            protocal.setAct(request.act);
            protocal.setTime(System.currentTimeMillis());
        }
        protocal.setType(type);
        String rx = GsonUtils.toJsonWtihNullField(request.inputs);
        if (!TextUtils.isEmpty(request.act) && request.act.contains(RESP)) {
            protocal.setRx(GsonUtils.toJsonWtihNullField(
                    new RespStatus(RespType.RESP_SUCCESS.getTye(), RespType.RESP_SUCCESS.getValue())));
        } else {
            protocal.setRx(rx);
        }
        map.put(request.iid, protocal);
    }

    /**
     * 消息中转处理判断
     */
    private void executeQueen() {
        for (Map.Entry<String, McuProtocal> entry : map.entrySet()) {
            McuProtocal protocal = entry.getValue();
            if (protocal.isOverTime()) {//超时
                RespStatus respStatus= new RespStatus(RespType.RESP_OUTTIME.getTye(), RespType.RESP_OUTTIME.getValue());
                String jsonContent=GsonUtils.toJsonWtihNullField(respStatus);
                switch (protocal.getType()) {
                    case GlobalConfig.TYPE_REMOTE_RX://消息接收
                        if (protocal.getTx() == null) {
                            protocal.setTx(jsonContent);
                        }
                        break;
                    case GlobalConfig.TYPE_REMOTE_TX://消息上报
                    case GlobalConfig.TYPE_REMOTE_TX_EVENT://消息上报→事件
                    case GlobalConfig.TYPE_REMOTE_TX_SERVICE://消息上报→属性
                        if (TextUtils.isEmpty(protocal.getRx())) {
                            protocal.setRx(jsonContent);
                        }
                        break;
                }
                XLog.d("iid=[" + protocal.getIid() + "],rx=[" + protocal.getTx() + "];Message processing timeout！");
                //超时两端都需要汇报
                judgeMethod(protocal);
                reportRxMsg(protocal);
                map.remove(protocal.getIid());
            } else {//未超时
                if (protocal.getStatus() == 0) {
                    //这里表示正在处理平台下发的操作
                    judgeMethod(protocal);
                    protocal.setStatus(protocal.getStatus()+1);
                } else {
                    //这里表示设备回复平台下发的操作
                    if (protocal.getTx() != null && !TextUtils.isEmpty(protocal.getRx())) {
                        //判断平台下发的消息和回复的消息都不为空即可上报该消息
                        judgeMethod(protocal);
                        map.remove(protocal.getIid());
                    }
                }
            }
        }
    }

    /**
     * 不同消息处理
     */
    private void judgeMethod(McuProtocal protocal) {
        switch (protocal.getType()) {
            case GlobalConfig.TYPE_REMOTE_TX://消息上报
            case GlobalConfig.TYPE_REMOTE_TX_EVENT://消息上报→事件
            case GlobalConfig.TYPE_REMOTE_TX_SERVICE://消息上报→属性
                reportRxMsg(protocal);
                break;
            case GlobalConfig.TYPE_REMOTE_RX://消息接收
                XLink.msgCallBack(protocal);
                break;
        }
    }


    /**
     * 获取ssid
     */
    private String getSsid() {
        return customParams.getRegister().ssid;
    }

    /**
     * 发送消息到服务端
     */
    private void reportRxMsg(McuProtocal msg) {
        Response response = new Response();
        response.setAct(msg.getAct());
        response.setIid(msg.getIid());
        response.setPayload(msg.getTx());
        String dataJson = GsonUtils.toJsonWtihNullField(response);
        XLog.d("reportRxMsg: dataJson=" + dataJson);
        MqttManager.getInstance().publish(msg.getAck(), 2, dataJson.getBytes(), RxMqttService.this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            XLog.d("onDestroy stop service");
            map.clear();
            MqttManager.getInstance().disConnect();
            XBus.unregister(this);
        } catch (Throwable e) {
            XLog.e("onDestroy", e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
