package com.future.xlink.bean.base;

public class BaseResponse <T>{
    public  String description;
    public  long status;
    public  T payload;
    public boolean isSuccessNonNull() {
        return status == 0 && payload != null;
    }

    public boolean isSuccess() {
        return status == 0;
    }

    @Override
    public String toString() {
        return "BaseResponse{" +
                "description='" + description + '\'' +
                ", status=" + status +
                ", payload=" + payload +
                '}';
    }
}
