package com.future.xlink.bean.other;

public class ProductInfo {
    public String deviceSn;
    public int productId;
    public int officeId;
    public String productName;
    public String officeName;
    public String activationDate;

    public ProductInfo(String deviceSn, int productId, int officeId, String productName, String officeName, String activationDate) {
        this.deviceSn = deviceSn;
        this.productId = productId;
        this.officeId = officeId;
        this.productName = productName;
        this.officeName = officeName;
        this.activationDate = activationDate;
    }

    public ProductInfo() {
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
