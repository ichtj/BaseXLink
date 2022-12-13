package com.future.xlink.request;

import android.text.TextUtils;

import com.elvishew.xlog.XLog;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.other.Agents;
import com.future.xlink.bean.other.Body;
import com.future.xlink.bean.other.Payload;
import com.future.xlink.bean.other.ProductInfo;
import com.future.xlink.bean.other.Register;
import com.future.xlink.request.retrofit.IApis;
import com.future.xlink.callback.IHttpRequest;
import com.future.xlink.bean.base.BaseResponse;
import com.future.xlink.request.retrofit.RetrofitClient;
import com.future.xlink.request.retrofit.SelfObserver;
import com.future.xlink.utils.GsonTools;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class XLinkHttp {
    /**
     * 获取代理服务地址
     */
    public static void getAgentList(InitParams iParams, IHttpRequest iReq) {
        String time = String.valueOf(System.currentTimeMillis());
        String token = DataTransfer.getToken(iParams.appKey, iParams.appSecret, iParams.clientId, time);
        if (token != null) {
            RetrofitClient.getInstance().getAgentList(iParams.httpUrl
                    + ":" + iParams.httpPort + "/" + IApis.AGENT_SERVER_LIST, token, time, iParams.clientId)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SelfObserver<BaseResponse<Agents>>() {
                        @Override
                        public void onNext(BaseResponse<Agents> bResponse) {
                            super.onNext(bResponse);
                            XLog.d("getAgentList: baseResponse=" + bResponse.toString());
                            if (bResponse.isSuccessNonNull()) {
                                //获取服务器列表成功，进行ping操作，获得最佳连接链路
                                XLog.d("onNext pinglist:" + bResponse.payload.servers.toString());
                                if (bResponse.payload.servers == null || bResponse.payload.servers.size() == 0) {
                                    //返回失败1
                                    iReq.requestErr(bResponse.description);
                                } else {
                                    iReq.requestComplete(GsonTools.toJsonWtihNullField(bResponse.payload));
                                }
                            } else {
                                iReq.requestErr(bResponse.description);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            XLog.e("onError: ", e);
                            if (e instanceof SocketTimeoutException) {
                                iReq.requestErr("Request timed out!");
                            } else {
                                iReq.requestErr(e.getMessage());
                            }
                        }
                    });
        } else {
            iReq.requestErr("Failed to obtain credentials(getAgentList)");
        }
    }


    /**
     * 获取设备信息
     * 所在产品
     */
    public static void getDeviceInfo(InitParams iParams, IHttpRequest iReq) {
        Map<String, Object> body = new HashMap<>();
        body.put("ack", "dev/" + iParams.clientId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("did", iParams.clientId);
        payload.put("new", true);
        payload.put("pdid", iParams.pdid);
        body.put("payload", payload);

        String time = String.valueOf(System.currentTimeMillis());
        String token = DataTransfer.getToken(iParams.uniqueKey, iParams.uniqueScret, iParams.clientId, time);
        if (token != null) {
            RetrofitClient.getInstance()
                    .getDeviceInfo(iParams.httpUrl + ":" + iParams.httpPort + "/" + IApis.PRODUCT_DEVICE_INFO, token, time, iParams.clientId, body)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SelfObserver<BaseResponse<ProductInfo>>() {
                        @Override
                        public void onNext(BaseResponse<ProductInfo> baseResponse) {
                            super.onNext(baseResponse);
                            XLog.d("accept: baseResponse >> " + baseResponse.toString());
                            if (baseResponse.status == 0) {
                                //产品一致
                                iReq.requestComplete(GsonTools.toJsonWtihNullField(baseResponse.payload));
                            } else if (baseResponse.status == 1) {
                                if (baseResponse.payload == null) {
                                    //设备不存在
                                    iReq.requestErr("设备不存在！");
                                } else {
                                    //产品不一致
                                    iReq.requestErr("产品不一致！");
                                }
                            } else {
                                iReq.requestErr("检查参数,系统时间是否正常！");
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            XLog.e("onError: ", e);
                            if (e instanceof SocketTimeoutException) {
                                iReq.requestErr("Request timed out!");
                            } else {
                                iReq.requestErr(e.getMessage());
                            }
                        }
                    });
        } else {
            iReq.requestErr("Failed to obtain credentials(getDeviceInfo)");
        }
    }

    /**
     * 添加id到产品下
     */
    public static void addProdId(InitParams params, IHttpRequest iReq) {
        Map<String, Object> body = new HashMap<>();
        body.put("ack", "dev/" + params.clientId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("did", params.clientId);
        payload.put("new", true);
        payload.put("pdid", params.pdid);
        String time = String.valueOf(System.currentTimeMillis());
        body.put("payload", payload);
        String token = DataTransfer.getToken(params.uniqueKey, params.uniqueScret, params.clientId, time);
        if (token != null) {
            RetrofitClient.getInstance()
                    .uniqueProduct(params.httpUrl + ":" + params.httpPort + "/" + IApis.PRODUCT_UNIQUE, token, time, params.clientId, body)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new SelfObserver<BaseResponse>() {
                        @Override
                        public void onNext(BaseResponse baseResponse) {
                            super.onNext(baseResponse);
                            XLog.d("registeronNext:>=" + baseResponse.toString());
                            if (baseResponse.status == 0) {
                                iReq.requestComplete("");
                            } else {
                                //1 设备SN唯一验证失败, 该设备SN已经存在
                                //403 达到最大的设备数量限制
                                iReq.requestErr(baseResponse.description);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            super.onError(e);
                            XLog.e("onError: ", e);
                            if (e instanceof SocketTimeoutException) {
                                iReq.requestErr("Request timed out!");
                            } else {
                                iReq.requestErr(e.getMessage());
                            }
                        }
                    });
        } else {
            iReq.requestErr("Failed to obtain credentials(uniqueProduct)");
        }
    }

    /**
     * 注册设备
     */
    public static void registerDev(InitParams iParams, String url, IHttpRequest iReq) {
        Body body = new Body();
        body.ack = "dev/" + iParams.clientId;
        Payload payload = new Payload();
        payload.did = iParams.clientId;
        payload.pdid = iParams.pdid;
        payload.oldMqttBroker = TextUtils.isEmpty(iParams.mqttBroker) ? "" : iParams.mqttBroker;
        payload.isNew = true;
        body.payload = payload;
        String time = String.valueOf(System.currentTimeMillis());
        String token = DataTransfer.getToken(iParams.appKey, iParams.appSecret, iParams.clientId, time);
        if (token != null) {
            RetrofitClient.getInstance()
                    .registerAgent(url + IApis.AGENT_REGISTER, token, time, iParams.clientId, body)
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Consumer<BaseResponse<Register>>() {
                        @Override
                        public void accept(BaseResponse<Register> regPonse) throws Exception {
                            XLog.d("registerRequest onNext status:" + regPonse.toString());
                            if (regPonse.isSuccessNonNull()) {
                                iReq.requestComplete(GsonTools.toJsonWtihNullField(regPonse.payload));
                            } else {
                                iReq.requestErr(regPonse.description);
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable e) throws Exception {
                            XLog.e("registerRequest", e);
                            iReq.requestErr(e.getMessage());
                        }
                    });
        } else {
            iReq.requestErr("Failed to obtain credentials(registerAgent)");
        }
    }
}
