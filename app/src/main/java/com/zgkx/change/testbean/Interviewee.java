package com.zgkx.change.testbean;

import java.util.List;

public class Interviewee {
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
    private int tenementType;
    private List<Vehicle> vehicleList;

    public Interviewee() {
    }

    public Interviewee(String personId, String personNo, String personName, int personGender, String deptId, String deptName, String remark, String personPhoto, String certificateType, String identityNo, String mobile, String tel1, String tel2, String email, String roomNo, String address, int tenementType, List<Vehicle> vehicleList) {
        this.personId = personId;
        this.personNo = personNo;
        this.personName = personName;
        this.personGender = personGender;
        this.deptId = deptId;
        this.deptName = deptName;
        this.remark = remark;
        this.personPhoto = personPhoto;
        this.certificateType = certificateType;
        this.identityNo = identityNo;
        this.mobile = mobile;
        this.tel1 = tel1;
        this.tel2 = tel2;
        this.email = email;
        this.roomNo = roomNo;
        this.address = address;
        this.tenementType = tenementType;
        this.vehicleList = vehicleList;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String personId) {
        this.personId = personId;
    }

    public String getPersonNo() {
        return personNo;
    }

    public void setPersonNo(String personNo) {
        this.personNo = personNo;
    }

    public String getPersonName() {
        return personName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public int getPersonGender() {
        return personGender;
    }

    public void setPersonGender(int personGender) {
        this.personGender = personGender;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPersonPhoto() {
        return personPhoto;
    }

    public void setPersonPhoto(String personPhoto) {
        this.personPhoto = personPhoto;
    }

    public String getCertificateType() {
        return certificateType;
    }

    public void setCertificateType(String certificateType) {
        this.certificateType = certificateType;
    }

    public String getIdentityNo() {
        return identityNo;
    }

    public void setIdentityNo(String identityNo) {
        this.identityNo = identityNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getTel1() {
        return tel1;
    }

    public void setTel1(String tel1) {
        this.tel1 = tel1;
    }

    public String getTel2() {
        return tel2;
    }

    public void setTel2(String tel2) {
        this.tel2 = tel2;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRoomNo() {
        return roomNo;
    }

    public void setRoomNo(String roomNo) {
        this.roomNo = roomNo;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getTenementType() {
        return tenementType;
    }

    public void setTenementType(int tenementType) {
        this.tenementType = tenementType;
    }

    public List<Vehicle> getVehicleList() {
        return vehicleList;
    }

    public void setVehicleList(List<Vehicle> vehicleList) {
        this.vehicleList = vehicleList;
    }
}
