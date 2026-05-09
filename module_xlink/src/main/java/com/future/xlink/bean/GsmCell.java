package com.future.xlink.bean;

public class GsmCell {
    public int MCC;
    public int MNC;
    public int LAC;
    public int CID;

    @Override
    public String toString() {
        return "SimBean{" +
                "MCC=" + MCC +
                ", MNC=" + MNC +
                ", LAC=" + LAC +
                ", CID=" + CID +
                '}';
    }
}
