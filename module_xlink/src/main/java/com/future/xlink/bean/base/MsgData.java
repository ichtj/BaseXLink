package com.future.xlink.bean.base;

import java.util.Map;

public class MsgData extends BaseData{
    public boolean isDeliveryed;//平台已告知接收完成
    public long pushTime;
    public long pushCount;
    public String _description;//描述
    public int _status;//0未发送 -1发送中

    public MsgData(int iPutType, String iid, String operation, Map<String, Object> maps, boolean isDeliveryed, String _description, int _status) {
        super (iPutType, iid, operation, maps);
        this.isDeliveryed = isDeliveryed;
        this._description = _description;
        this._status = _status;
    }

    @Override
    public String toString() {
        return "MsgData{" +
                "isDeliveryed=" + isDeliveryed +
                ", pushTime=" + pushTime +
                ", pushCount=" + pushCount +
                ", _description='" + _description + '\'' +
                ", _status=" + _status +
                '}';
    }
}
