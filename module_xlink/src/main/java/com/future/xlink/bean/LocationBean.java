package com.future.xlink.bean;

public class LocationBean {
    public double lat;
    public double lon;
    public int errcode;
    public int radius;
    public String address;

    public LocationBean() {
    }

    public LocationBean(double lat, double lon, int errcode, int radius, String address) {
        this.lat = lat;
        this.lon = lon;
        this.errcode = errcode;
        this.radius = radius;
        this.address = address;
    }
}
