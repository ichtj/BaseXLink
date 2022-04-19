package com.future.xlink.bean;

public class ProductInfo {
    private String deviceSn;
    private int productId;
    private String activationDate;

    public ProductInfo() {
    }

    public ProductInfo(String deviceSn, int productId, String activationDate) {
        this.deviceSn = deviceSn;
        this.productId = productId;
        this.activationDate = activationDate;
    }

    public String getDeviceSn() {
        return deviceSn;
    }

    public void setDeviceSn(String deviceSn) {
        this.deviceSn = deviceSn;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public String getActivationDate() {
        return activationDate;
    }

    public void setActivationDate(String activationDate) {
        this.activationDate = activationDate;
    }
}
