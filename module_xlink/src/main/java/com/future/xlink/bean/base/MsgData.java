package com.future.xlink.bean.base;

import java.util.Map;

public class MsgData extends BaseData{
    public boolean isPush;
    public boolean isDeliveryed;
    public long pushTime;
    public long pushCount;

    public MsgData(int iPutType, String iid, String operation, Map<String, Object> maps, boolean isPush, boolean isDeliveryed) {
        super(iPutType, iid, operation, maps);
        this.isPush = isPush;
        this.isDeliveryed = isDeliveryed;
    }

    public MsgData(int iPutType, String iid, String operation, Map<String, Object> maps) {
        super(iPutType, iid, operation, maps);
    }

    @Override
    public String toString() {
        return "MsgData{" +
                "iPutType=" + iPutType +
                ", iid='" + iid + '\'' +
                ", operation='" + operation + '\'' +
                ", maps=" + maps +
                ", isPush=" + isPush +
                ", isDeliveryed=" + isDeliveryed +
                ", pushTime=" + pushTime +
                '}';
    }
}
