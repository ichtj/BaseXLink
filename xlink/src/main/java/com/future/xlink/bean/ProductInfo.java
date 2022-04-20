package com.future.xlink.bean;

public class ProductInfo {
    private String deviceSn;
    private int productId;
    private int officeId;
    private String productName;
    private String officeName;
    private String activationDate;

    public ProductInfo() {
    }

    public ProductInfo(String deviceSn, int productId, int officeId, String productName, String officeName, String activationDate) {
        this.deviceSn = deviceSn;
        this.productId = productId;
        this.officeId = officeId;
        this.productName = productName;
        this.officeName = officeName;
        this.activationDate = activationDate;
    }

    public int getOfficeId() {
        return officeId;
    }

    public void setOfficeId(int officeId) {
        this.officeId = officeId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getOfficeName() {
        return officeName;
    }

    public void setOfficeName(String officeName) {
        this.officeName = officeName;
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

    @Override
    public String toString() {
        return "ProductInfo{" +
                "deviceSn='" + deviceSn + '\'' +
                ", productId=" + productId +
                ", officeId=" + officeId +
                ", productName='" + productName + '\'' +
                ", officeName='" + officeName + '\'' +
                ", activationDate='" + activationDate + '\'' +
                '}';
    }
}
