package com.future.xlink.bean.base;

import com.future.xlink.callback.IPutType;

import java.util.Map;

public class BaseData {
    public @IPutType
    int iPutType;
    public String iid;
    public String operation;
    public Map<String, Object> maps;

    public BaseData(int iPutType, String iid, String operation, Map<String, Object> maps) {
        this.iPutType = iPutType;
        this.iid = iid;
        this.operation = operation;
        this.maps = maps;
    }

    @Override
    public String toString() {
        return "MsgData{" +
                "iPutType=" + iPutType +
                ", iid='" + iid + '\'' +
                ", operation='" + operation + '\'' +
                ", maps=" + maps +
                '}';
    }
}
