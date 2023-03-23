package com.future.xlink.callback;

/**
 * 公共的Http请求
 */
public interface IHttpRequest {
    /**
     * 设备存在
     */
    void requestComplete(String jsonData);

    /**
     * 请求错误
     * @param description 错误描述
     */
    void requestErr(int errCode,String description);
}
