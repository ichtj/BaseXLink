package com.zgkx.change.testbean;

import java.util.List;

public class Person {
    private String personId;
    private String personNo;
    private String personName;
    private int personGender;
    private String deptId;
    private String deptName;
    private String remark;
    private String personPhoto;
    private String certificateType;
    private String identityNo;
    private String mobile;
    private String tel1;
    private String tel2;
    private String email;
    private String roomNo;
    private String address;
    private Object tenementType;
    private List<Vehicle> vehicleList;
    public void setPersonId(String personId) {
        this.personId = personId;
    }
    public String getPersonId() {
        return personId;
    }

    public void setPersonNo(String personNo) {
        this.personNo = personNo;
    }
    public String getPersonNo() {
        return personNo;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }
    public String getPersonName() {
        return personName;
    }

    public void setPersonGender(int personGender) {
        this.personGender = personGender;
    }
    public int getPersonGender() {
        return personGender;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }
    public String getDeptId() {
        return deptId;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }
    public String getDeptName() {
        return deptName;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
    public String getRemark() {
        return remark;
    }

    public void setPersonPhoto(String personPhoto) {
        this.personPhoto = personPhoto;
    }
    public String getPersonPhoto() {
        return personPhoto;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }
    public String getCertificateType() {
        return certificateType;
    }

    public void setIdentityNo(String identityNo) {
        this.identityNo = identityNo;
    }
    public String getIdentityNo() {
        return identityNo;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
    public String getMobile() {
        return mobile;
    }

    public void setTel1(String tel1) {
        this.tel1 = tel1;
    }
    public String getTel1() {
        return tel1;
    }

    public void setTel2(String tel2) {
        this.tel2 = tel2;
    }
    public String getTel2() {
        return tel2;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    public String getEmail() {
        return email;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }
    public String getRoomNo() {
        return roomNo;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    public String getAddress() {
        return address;
    }

    public void setTenementType(Object tenementType) {
        this.tenementType = tenementType;
    }
    public Object getTenementType() {
        return tenementType;
    }

    public void setVehicleList(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }
    public List<Vehicle> getVehicleList() {
        return vehicleList;
    }

    @Override
    public String toString() {
        return "Person{" +
                "personId='" + personId + '\'' +
                ", personNo='" + personNo + '\'' +
                ", personName='" + personName + '\'' +
                ", personGender=" + personGender +
                ", deptId='" + deptId + '\'' +
                ", deptName='" + deptName + '\'' +
                ", remark='" + remark + '\'' +
                ", personPhoto='" + personPhoto + '\'' +
                ", certificateType='" + certificateType + '\'' +
                ", identityNo='" + identityNo + '\'' +
                ", mobile='" + mobile + '\'' +
                ", tel1='" + tel1 + '\'' +
                ", tel2='" + tel2 + '\'' +
                ", email='" + email + '\'' +
                ", roomNo='" + roomNo + '\'' +
                ", address='" + address + '\'' +
                ", tenementType=" + tenementType +
                ", vehicleList=" + vehicleList +
                '}';
    }
}
