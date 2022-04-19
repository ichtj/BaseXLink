package com.future.xlink.api;


import com.future.xlink.api.response.BaseResponse;
import com.future.xlink.bean.Agents;
import com.future.xlink.bean.LogBean;
import com.future.xlink.bean.LogPayload;
import com.future.xlink.bean.ProductInfo;
import com.future.xlink.bean.Register;
import com.future.xlink.bean.request.Payload;
import com.future.xlink.utils.GlobalConfig;

import java.util.Map;

import io.reactivex.Observable;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.PartMap;
import retrofit2.http.Url;

/**
 * 接口信息
 *
 * @author lee
 */
public interface ApiService {
    @POST
    Observable <BaseResponse <Agents>> getAgentList(@Url String url,
                                                    @Header("Authorization") String token,
                                                    @Header("time") String timestamp,
                                                    @Header("SN") String sn);

    @POST
    Observable <BaseResponse <Register>> registerAgent(@Url String url,
                                                       @Header("Authorization") String token,
                                                       @Header("time") String timestamp,
                                                       @Header("SN") String sn,
                                                       @Body com.future.xlink.bean.request.Body body);

    @POST
    Observable <BaseResponse <Agents>> uniqueProduct(@Url String url,
                                                     @Header("Authorization") String token,
                                                     @Header("time") String timestamp,
                                                     @Header("SN") String sn,
                                                     @Body Map<String, Object> body);

    @POST
    Observable <BaseResponse<ProductInfo>> getDeviceInfo(@Url String url,
                                                         @Header("Authorization") String token,
                                                         @Header("time") String timestamp,
                                                         @Header("SN") String sn,
                                                         @Body Map<String, Object> body);
    @POST
    Observable <BaseResponse<LogPayload>>getUploadLogUrl(@Url String url, @Header("Authorization") String token,
                                                         @Header("time") String timestamp,
                                                         @Header("SN") String sn, @Body LogBean logBean);


    @POST
    @Multipart
    Call<BaseResponse> doUploadFile(@Url String url,
                                    @Header("Authorization") String token,
                                    @Header("time") String timestamp,
                                    @Header("SN") String sn,
                                    @PartMap Map<String, RequestBody> data,
                                    @Part MultipartBody.Part multipartBody,
                                    @Header("Content-Range") String content);


}