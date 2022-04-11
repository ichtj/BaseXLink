package com.future.xlink.bean;

import com.future.xlink.utils.GlobalConfig;

import java.lang.reflect.Type;

public class McuProtocal extends Protocal {
    private String act;
    /**
     * 响应主题，如APP为请求端，则为APP订阅的主题,用户无需定义
     * */
    private String ack;

    private long time; //消息到达时间
    private int type; //当前消息类型
    private int status; //是否处理标志位 0，没处理，1处理

    public McuProtocal(String iid, Object tx, String rx, int overtime, String act, String ack, long time, int type, int status) {
        super(iid, tx, rx, overtime);
        this.act = act;
        this.ack = ack;
        this.time = time;
        this.type = type;
        this.status = status;
    }

    public McuProtocal() {
    }

    public String getAct() {
        return act;
    }

    public void setAct(String act) {
        this.act = act;
    }

    public String getAck() {
        return ack;
    }

    public void setAck(String ack) {
        this.ack = ack;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * 自消息纪录以来 该消息已经超时了10分钟未得到服务器回应
     * @return 是否超时
     */
    public boolean isOverTime() {
        if (getOvertime()*1000<=GlobalConfig.OVER_TIME){
            setOvertime(GlobalConfig.OVER_TIME);
        }
        return time != 0 && (System.currentTimeMillis() - time >= getOvertime());
    }
}
