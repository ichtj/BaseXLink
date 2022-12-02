package com.future.xlink;

import android.content.Context;
import android.content.Intent;

import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Protocal;
import com.future.xlink.bean.common.RespType;
import com.future.xlink.bean.mqtt.RespStatus;
import com.future.xlink.callback.MessageListener;
import com.future.xlink.mqtt.MqttManager;
import com.future.xlink.mqtt.RxMqttService;
import com.future.xlink.utils.Carrier;
import com.future.xlink.utils.GlobalConfig;
import com.future.xlink.utils.GsonUtils;
/*import com.future.xlink.utils.PropertiesUtil;*/
import com.future.xlink.bean.common.ConnStatus;
import com.future.xlink.bean.common.LostStatus;
import com.future.xlink.utils.XBus;

import java.io.File;

import io.reactivex.annotations.NonNull;

/**
 * @author chtj
 */
public class XLink {
    /**
     * 单例
     */
    private static XLink mInstance = null;
    /**
     * 消息回调接口
     */
    private MessageListener listener;

    private XLink() {
    }


    private MessageListener getListener() {
        return listener;
    }

    /**
     * 回调连接状态
     * @param connStatus 连接状态
     */
    public static void connStatus(ConnStatus connStatus) {
        MessageListener listener=  getInstance().getListener();
        if(listener!=null){
            listener.connStatus(connStatus);
        }
    }
    /**
     * 回调丢失状态
     * @param lostStatus 丢失状态
     */
    public static void lostStatus(LostStatus lostStatus) {
        MessageListener listener=  getInstance().getListener();
        if(listener!=null){
            listener.lostStatus(lostStatus);
        }
    }

    /**
     * 消息回调
     * 包括本地消息处理 平台下发的消息处理
     * @param msg 数据内容
     */
    public static void msgCallBack(Protocal msg) {
        MessageListener listener=  getInstance().getListener();
        if(listener!=null){
            listener.messageArrived(msg);
        }
    }

    public static XLink getInstance() {
        if (null == mInstance) {
            mInstance = new XLink();
        }
        return mInstance;
    }

    /**
     * xlink初始化
     *
     * @param context  初始化句柄
     * @param params   初始化参数类
     * @param listener 初始化回调函数
     */
    public void connect(@NonNull Context context, @NonNull InitParams params, @NonNull MessageListener listener) {
        this.listener = listener;
        String configFolder=GlobalConfig.ROOT_PATH+context.getPackageName()+"/"+params.getSn()+"/";
        initCreateFile(configFolder);
        Intent intent = new Intent(context, RxMqttService.class);
        params.setConfigPath(configFolder+GlobalConfig.MY_PROPERTIES);
        intent.putExtra(RxMqttService.INIT_PARAM, params);
        context.startService(intent);
    }

    private void initCreateFile(String configFolder){
        File configFile=new File(configFolder);
        if(!configFile.exists()){
            configFile.mkdirs();
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        XBus.post(new Carrier(GlobalConfig.TYPE_MODE_DISCONNECT));
    }


    /**
     * 注销函数，注销后，重连mqtt需要重新调用init()函数
     */
    public void unInit() {
        //通知连接关闭
        XBus.post(new Carrier(GlobalConfig.TYPE_MODE_UNINIT));
    }

    /**
     * 上报服务属性函数
     *
     * @param protocal 属性消息封装
     */
    public void upService(Protocal protocal) {
        publish(GlobalConfig.TYPE_REMOTE_TX_SERVICE, protocal);
    }

    /**
     * 上报事件函数
     *
     * @param protocal 事件消息封装
     */
    public void upEvent(Protocal protocal) {
        publish(GlobalConfig.TYPE_REMOTE_TX_EVENT, protocal);
    }

    /**
     * 代理服务端请求响应事件
     *
     */
    public void upResponse(Protocal protocal) {
        publish(GlobalConfig.TYPE_REMOTE_TX, protocal);
    }

    private void publish(int type, Protocal protocal) {
        if (MqttManager.getInstance().isConnect()) {
            //状态为连接 才能添加消息到集合
            XBus.post(new Carrier(type, protocal));
        } else {
            if (this.listener != null) {
                //交互已经出现问题,回调该消息异常
                protocal.setRx(GsonUtils.toJsonWtihNullField(
                        new RespStatus(RespType.RESP_CONNECT_LOST.getTye(), RespType.RESP_CONNECT_LOST.getValue())));
                this.listener.messageArrived(protocal);
            }
        }
    }

    /**
     * 获取Mqtt连接状态
     * @return true连接 false断开
     */
    public boolean isConnected(){
        return MqttManager.getInstance().isConnect();
    }
}
