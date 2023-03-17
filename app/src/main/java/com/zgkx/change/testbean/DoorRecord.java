package com.zgkx.change.testbean;


public class DoorRecord {
    private String recordId;//
    private Person person;//
    private String remark;//
    private Device device;//
    private String cardNo;//
    private int eventType;//
    private int recordType;//
    private int inOutType;
    private int cardType;
    private Interviewee interviewee;//受访者
    private String crossTime;//
    private int offlineFlag;
    private String pictureFile;//
    private int errorNo;
    private String errorName;
    private int TemperatureDevID;
    private String TemperatureDevName;
    private double Temperature;
    private String reTrySend;

    public DoorRecord() {
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(Person person) {
        this.person = person;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public int getEventType() {
        return eventType;
    }

    public void setEventType(int eventType) {
        this.eventType = eventType;
    }

    public int getRecordType() {
        return recordType;
    }

    public void setRecordType(int recordType) {
        this.recordType = recordType;
    }

    public int getInOutType() {
        return inOutType;
    }

    public void setInOutType(int inOutType) {
        this.inOutType = inOutType;
    }

    public int getCardType() {
        return cardType;
    }

    public void setCardType(int cardType) {
        this.cardType = cardType;
    }

    public Interviewee getInterviewee() {
        return interviewee;
    }

    public void setInterviewee(Interviewee interviewee) {
        this.interviewee = interviewee;
    }

    public String getCrossTime() {
        return crossTime;
    }

    public void setCrossTime(String crossTime) {
        this.crossTime = crossTime;
    }

    public int getOfflineFlag() {
        return offlineFlag;
    }

    public void setOfflineFlag(int offlineFlag) {
        this.offlineFlag = offlineFlag;
    }

    public String getPictureFile() {
        return pictureFile;
    }

    public void setPictureFile(String pictureFile) {
        this.pictureFile = pictureFile;
    }

    public int getErrorNo() {
        return errorNo;
    }

    public void setErrorNo(int errorNo) {
        this.errorNo = errorNo;
    }

    public String getErrorName() {
        return errorName;
    }

    public void setErrorName(String errorName) {
        this.errorName = errorName;
    }

    public int getTemperatureDevID() {
        return TemperatureDevID;
    }

    public void setTemperatureDevID(int temperatureDevID) {
        TemperatureDevID = temperatureDevID;
    }

    public String getTemperatureDevName() {
        return TemperatureDevName;
    }

    public void setTemperatureDevName(String temperatureDevName) {
        TemperatureDevName = temperatureDevName;
    }

    public double getTemperature() {
        return Temperature;
    }

    public void setTemperature(double temperature) {
        Temperature = temperature;
    }

    public String getReTrySend() {
        return reTrySend;
    }

    public void setReTrySend(String reTrySend) {
        this.reTrySend = reTrySend;
    }

    @Override
    public String toString() {
        String result="DoorRecord{" +
                "recordId='" + recordId + '\'' +
                ", person=" + person+
                ", remark='" + remark + '\'' +
                ", device=" + device+
                ", cardNo='" + cardNo + '\'' +
                ", eventType=" + eventType +
                ", recordType=" + recordType +
                ", inOutType=" + inOutType +
                ", cardType=" + cardType +
                ", interviewee=" + interviewee +
                ", crossTime='" + crossTime + '\'' +
                ", offlineFlag=" + offlineFlag +
                ", pictureFile='" + pictureFile + '\'' +
                ", errorNo=" + errorNo +
                ", errorName='" + errorName + '\'' +
                ", TemperatureDevID=" + TemperatureDevID +
                ", TemperatureDevName='" + TemperatureDevName + '\'' +
                ", Temperature=" + Temperature +
                ", reTrySend='" + reTrySend + '\'' +
                '}';
        return result;
    }
}
