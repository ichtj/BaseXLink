package com.future.xlink.bean;

public class ActionFlag {
    private String act;
    private String iid;
    private String name;

    public ActionFlag(String act, String iid, String name) {
        this.act = act;
        this.iid = iid;
        this.name = name;
    }

    public String getAct() {
        return act;
    }

    public void setAct(String act) {
        this.act = act;
    }

    public String getIid() {
        return iid;
    }

    public void setIid(String iid) {
        this.iid = iid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "ActionFlag{" +
                "act='" + act + '\'' +
                ", iid='" + iid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}