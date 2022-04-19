package com.future.xlink.listener;


import com.future.xlink.bean.Protocal;
import com.future.xlink.bean.common.ConnStatus;
import com.future.xlink.bean.common.LostStatus;

public interface MessageListener {
    /**
     * 连接状态回调
     */
    void connStatus(ConnStatus result);
    /**
     * 丢失状态回调
     */
    void lostStatus(LostStatus result);
    /**
     * mqtt后台服务器下发给客户端的消息
     * @param  protocal  消息协议体
     * */
    void messageArrived(Protocal protocal);
}
