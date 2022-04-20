package com.future.xlink.mqtt;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.elvishew.xlog.XLog;
import com.future.xlink.R;
import com.future.xlink.XLink;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.McuProtocal;
import com.future.xlink.bean.Protocal;
import com.future.xlink.bean.common.MsgType;
import com.future.xlink.bean.common.RespType;
import com.future.xlink.bean.mqtt.Request;
import com.future.xlink.bean.mqtt.RespStatus;
import com.future.xlink.bean.mqtt.Response;
import com.future.xlink.utils.Carrier;
import com.future.xlink.utils.GlobalConfig;
import com.future.xlink.utils.GsonUtils;
import com.future.xlink.bean.common.LostStatus;
import com.future.xlink.utils.ObserverUtils;
import com.future.xlink.utils.PingUtils;
import com.future.xlink.bean.common.ConnStatus;
import com.future.xlink.utils.ThreadPool;
import com.future.xlink.utils.Utils;
import com.future.xlink.utils.XBus;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


/**
 * mqtt消息推送服务
 *
 * @author chtj
 */

public class RxMqttService extends Service {
    private static final String RESP = "-resp"; //消息回应后缀
    public static final String INIT_PARAM = "initparams";
    private final Object lock = new Object();
    private InitParams customParams = null;
    private Disposable disposable;
    private boolean isNetLost = false;
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
                XBus.post(new Carrier(GlobalConfig.TYPE_MODE_TO_CONNECT));
            } else {
                //代表未注册过
                if (customParams != null && customParams.productNotNull()) {
                    ObserverUtils.getAgentList(this, customParams);
                } else {
                    //判断注册参数是否有误
                    XLink.connStatus(new ConnStatus(GlobalConfig.STATUSCODE_FAILED, getString(R.string.init_params_null)));
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * 创建连接
     */
    private void createConect() {
        XLog.d("createConect: params.register=" + customParams.getRegister().toString());
        MqttManager.getInstance().creatNewConnect(RxMqttService.this, customParams);
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onStatus(ConnStatus msg) throws MqttException, IOException {
        connStatusChange(msg);
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onData(Carrier msg) throws MqttException, IOException {
        switch (msg.getType()) {
            case GlobalConfig.TYPE_MODE_LOST_RESULT://连接丢失操作
                connLost();
                break;
            case GlobalConfig.TYPE_MODE_UNINIT://执行注销操作
                disConnDelConfig();
                XLink.connStatus(new ConnStatus(GlobalConfig.STATUSCODE_FAILED, getString(R.string.conn_uninit)));
                break;
            case GlobalConfig.TYPE_MODE_TO_CONNECT://执行连接操作
                toConnect();
                break;
            case GlobalConfig.TYPE_MODE_DISCONNECT://执行断开
                map.clear();
                MqttManager.getInstance().disConnect();
                XLink.connStatus(new ConnStatus(GlobalConfig.STATUSCODE_FAILED, getString(R.string.conn_disconnect)));
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
        }
    }


    /**
     * 连接丢失的判断
     */
    public void connLost() {
        //连接丢失 不管本地网络是否正常 都将丢失连接
        if (PingUtils.checkNetWork()) {//访问外网正常
            if (!serviceIsNormal()) {
                XLink.lostStatus(new LostStatus(GlobalConfig.LOST_AGENT_CONN_ERR,true, getString(R.string.agent_conn_err)));
                //网络正常 代理服务连接异常 这里需要重置部分参数
                disConnDelConfig();
            } else {
                //网络正常 代理服务连接正常 丢失检测
                checkLostTime();
                XLink.lostStatus(new LostStatus(GlobalConfig.LOST_OTHER_ERR,true, getString(R.string.agent_succ_connerr)));
            }
        } else {
            //网络异常
            checkLostTime();
            XLink.lostStatus(new LostStatus(GlobalConfig.LOST_NETWORK_ERR,false, getString(R.string.device_net_err)));
        }
    }

    /**
     * 检查网络丢失时常
     */
    public void checkLostTime() {
        XLog.d("checkLostTime");
        isNetLost=true;
        Date lostTime=new Date();
        if (disposable == null) {
            disposable = Observable
                    .interval(0, 1, TimeUnit.MINUTES)
                    .subscribeOn(Schedulers.io())//调用切换之前的线程。
                    .observeOn(AndroidSchedulers.mainThread())//调用切换之后的线程。observeOn之后，不可再调用subscribeOn 切换线程
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            if(!isNetLost){
                                //连接或者重连成功 关闭此任务
                                closeDisposable();
                            }
                            if(PingUtils.checkNetWork()){
                                //网络正常的时候才去检查丢失连接是否超时 网络异常时检查无意义
                                long miao = (new Date().getTime() - lostTime.getTime()) / 1000;//除以1000是为了转换成秒
                                long minutes = miao / 60;   // 分钟
                                if(minutes>=30){
                                    XLog.d("lost minutes>=30");
                                    XLink.lostStatus(new LostStatus(GlobalConfig.LOST_TIMEROUT_ERR,true,getString(R.string.conn_lost_timeout)));
                                    closeDisposable();
                                }
                            }

                        }
                    });
        }
    }

    /**
     * 关闭定时检测任务
     */
    public void closeDisposable(){
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
            disposable = null;
            XLog.d("closeDisposable");
        }
    }

    /**
     * 检查代理服务是否连接正常
     */
    public boolean serviceIsNormal(){
        String ip = Utils.patternIp(customParams.getRegister().mqttBroker);
        //远程服务器ping结果
        return PingUtils.ping(ip, 1, 1);
    }

    /**
     * 断开mqtt并删除my.properties
     */
    private void disConnDelConfig() {
        map.clear();
        GlobalConfig.delProperties(customParams.getConfigPath());
        MqttManager.getInstance().disConnect();
    }

    /**
     * 去执行连接
     */
    public void toConnect() {
        boolean isNetOk = PingUtils.checkNetWork();//判断网络是否正常
        XLog.d("onEvent:TYPE_MODE_CONNECT isNetOk=" + isNetOk);
        map.clear();//创建连接时清除之前的消息队列
        if (!isNetOk) {
            XLink.connStatus(new ConnStatus(GlobalConfig.STATUSCODE_FAILED, getString(R.string.device_net_err)));//回调网络不正常
        } else {
            //尝试ping mqtt服务地址是否正常
            if (!serviceIsNormal()) {
                XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED, getString(R.string.agent_conn_err)));
            } else {
                //mqtt服务能够正常连接 那么直接去连接
                createConect();
            }
        }
    }

    /**
     * 连接状态，连接结果变更
     *
     * @param connStatus 状态
     *                   结果从MqttManager的iMqttActionListener进行回调
     *                   主动断开
     */
    public void connStatusChange(ConnStatus connStatus) {
        XLog.d("onEvent： TYPE_MODE_CONNECT_RESULT value=" + connStatus.toString());
        switch (connStatus.getCode() + "") {
            case GlobalConfig.STATUSCODE_SUCCESS + ""://连接完成
                MqttManager.getInstance().
                        subscribe("dev/" + customParams.getSn() + "/#", 2, this);
                XLink.connStatus(connStatus);
                isNetLost=false;
                break;
            case GlobalConfig.STATUSCODE_FAILED + ""://连接断开
            default:
                map.clear();
                MqttManager.getInstance().disConnect();
                disConnDelConfig();
                XLink.connStatus(connStatus);
                break;
        }
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
                RespStatus respStatus = new RespStatus(RespType.RESP_OUTTIME.getTye(), RespType.RESP_OUTTIME.getValue());
                String jsonContent = GsonUtils.toJsonWtihNullField(respStatus);
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
                    protocal.setStatus(protocal.getStatus() + 1);
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
