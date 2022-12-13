package com.future.xlink.callback;

import com.future.xlink.bean.base.BaseData;

public interface IMqttCallback {
    /**
     * 连接变化
     *
     * @param connected   true false
     * @param description connected 为false时描述错误信息
     */
    void connState(boolean connected, String description);

    /**
     * 消息到达
     *
     * @param baseData 消息内容
     */
    void msgArrives(BaseData baseData);

    /**
     * 已推送消息完成
     *
     * @param data 推送内容
     */
    void pushed(BaseData data);

    /**
     * 平台回复完毕
     *
     * @param act
     * @param iid
     */
    void iotReplyed(String act, String iid);

    /**
     * 推送内容失败
     *
     * @param baseData    推送内容
     * @param description 错误描述
     */
    void pushFail(BaseData baseData, String description);

    /**
     * 订阅成功
     *
     * @param topic 订阅内容
     */
    void subscribed(String topic);

    /**
     * 订阅失败
     *
     * @param topic       订阅内容
     * @param description 错误描述
     */
    void subscribeFail(String topic, String description);
}
