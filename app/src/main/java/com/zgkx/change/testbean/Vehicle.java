package com.zgkx.change.testbean;

public class Vehicle {
    private String plateNumber;
    private String vehicleBrand;
    private String vehicleColor;
    private String plateColor;
    private String remark;
    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }
    public String getPlateNumber() {
        return plateNumber;
    }

    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }
    public String getVehicleBrand() {
        return vehicleBrand;
    }

    public void setVehicleColor(String vehicleColor) {
        this.vehicleColor = vehicleColor;
    }
    public String getVehicleColor() {
        return vehicleColor;
    }

    public void setPlateColor(String plateColor) {
        this.plateColor = plateColor;
    }
    public String getPlateColor() {
        return plateColor;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
    public String getRemark() {
        return remark;
    }

    @Override
    public String toString() {
        return "Vehicle{" +
                "plateNumber='" + plateNumber + '\'' +
                ", vehicleBrand='" + vehicleBrand + '\'' +
                ", vehicleColor='" + vehicleColor + '\'' +
                ", plateColor='" + plateColor + '\'' +
                ", remark='" + remark + '\'' +
                '}';
    }
}
