package com.zgkx.change.testbean;

public class Device {
    private String deviceGuid;
    private String deviceId;
    private String deviceName;
    private String deviceIp;
    private String deviceGateway;
    private String deviceNetmask;
    private int deviceType;
    private int deviceIoType;
    private String parentId;
    private String parentName;
    private String remark;

    public int getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(int deviceType) {
        this.deviceType = deviceType;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public void setDeviceGuid(String deviceGuid) {
        this.deviceGuid = deviceGuid;
    }

    public String getDeviceGuid() {
        return deviceGuid;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceIp(String deviceIp) {
        this.deviceIp = deviceIp;
    }

    public String getDeviceIp() {
        return deviceIp;
    }

    public void setDeviceGateway(String deviceGateway) {
        this.deviceGateway = deviceGateway;
    }

    public String getDeviceGateway() {
        return deviceGateway;
    }

    public void setDeviceNetmask(String deviceNetmask) {
        this.deviceNetmask = deviceNetmask;
    }

    public String getDeviceNetmask() {
        return deviceNetmask;
    }

    public void setDeviceIoType(int deviceIoType) {
        this.deviceIoType = deviceIoType;
    }

    public int getDeviceIoType() {
        return deviceIoType;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRemark() {
        return remark;
    }

    @Override
    public String toString() {
        return "Device{" +
                "deviceGuid='" + deviceGuid + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceIp='" + deviceIp + '\'' +
                ", deviceGateway='" + deviceGateway + '\'' +
                ", deviceNetmask='" + deviceNetmask + '\'' +
                ", deviceType=" + deviceType +
                ", deviceIoType=" + deviceIoType +
                ", parentId='" + parentId + '\'' +
                ", parentName='" + parentName + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
