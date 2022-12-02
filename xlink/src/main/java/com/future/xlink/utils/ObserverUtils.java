package com.future.xlink.utils;

import android.content.Context;

import com.elvishew.xlog.XLog;
import com.future.xlink.R;
import com.future.xlink.request.response.BaseResponse;
import com.future.xlink.request.retrofit.RetrofitClient;
import com.future.xlink.request.subscribe.SelfObserver;
import com.future.xlink.bean.Agents;
import com.future.xlink.bean.InitParams;
import com.future.xlink.bean.Register;
import com.future.xlink.bean.common.ConnStatus;
import com.future.xlink.bean.method.request.Body;
import com.future.xlink.bean.method.request.Payload;
import com.future.xlink.callback.PingListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.schedulers.Schedulers;

public class ObserverUtils {
    /**
     * 获取代理服务地址
     *
     * @param customParams 必要参数
     */
    public static void getAgentList(Context context,InitParams customParams) {
        String time = String.valueOf(System.currentTimeMillis());
        String token = Utils.getToken(customParams, time);
        if(token!=null){
            RetrofitClient.getInstance().getAgentList(customParams.getHttpServer() + GlobalConfig.AGENT_SERVER_LIST, token, time, customParams.getSn())
                    .subscribeOn(Schedulers.io()).subscribe(new SelfObserver<BaseResponse<Agents>>() {
                @Override
                public void onNext(BaseResponse<Agents> baseResponse) {
                    super.onNext(baseResponse);
                    XLog.d("getAgentList: baseResponse=" + baseResponse.toString());
                    if (baseResponse.isSuccessNonNull()) {
                        //获取服务器列表成功，进行ping操作，获得最佳连接链路
                        List<String> pinglist = baseResponse.payload.servers;
                        XLog.d("onNext pinglist:" + pinglist.toString());
                        if (pinglist == null || pinglist.size() == 0) {
                            //返回失败1
                            XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.agent_list_null)));
                        } else if (pinglist.size() == 1) {
                            //如果列表只有一个取消ping操作
                            registerRequest(context,customParams, baseResponse.payload.servers.get(0));
                        } else {
                            //执行循环ping操作,找到最优的线路
                            ObserverUtils.pingTest(pinglist, o -> registerRequest(context,customParams, o));
                        }
                    } else{
                        XBus.post(new ConnStatus(baseResponse.status, baseResponse.description));
                    }
                }

                @Override
                public void onError(Throwable e) {
                    super.onError(e);
                    XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.agent_other_err)));
                    XLog.e("getAgentList", e);
                }
            });
        }else{
            XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.unable_get_credentials)));
        }
    }

    private static void registerRequest(Context context,InitParams customParams, String url) {
        Body body = new Body();
        body.ack = "dev/" + customParams.getSn();
        Payload payload = new Payload();
        payload.did = customParams.getSn();
        payload.pdid = customParams.getPdid();
        Register register = customParams.getRegister();
        payload.oldMqttBroker = register == null ? "" : register.mqttBroker;
        payload.isNew = true;
        body.payload = payload;
        String time = String.valueOf(System.currentTimeMillis());
        String token=Utils.getToken(customParams, time);
        if (token!=null){
            RetrofitClient.getInstance().registerAgent(url + GlobalConfig.AGENT_REGISTER,
                    token, time, customParams.getSn(), body)
                    .subscribeOn(Schedulers.io()).subscribe(new SelfObserver<BaseResponse<Register>>() {
                @Override
                public void onNext(BaseResponse<Register> regPonse) {
                    super.onNext(regPonse);
                    XLog.d("registerRequest onNext status:" + regPonse.status);
                    if (regPonse.isSuccessNonNull()) {
                        customParams.setRegister(regPonse.payload);
                        XLog.d("registerRequest onNext get register:" + customParams.getRegister().toString());
                        if (customParams.registerNotNull()) {
                            String configSave = JsonFormatTool.formatJson(GsonUtils.toJsonWtihNullField(customParams));
                            boolean isWrite = Utils.writeFileData(customParams.getConfigPath(), configSave, true);
                            XLog.d("isWrite=" + isWrite);
                            if(isWrite){
                                XBus.post(new Carrier(GlobalConfig.TYPE_MODE_TO_CONNECT));
                            }else{
                                XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.init_succ_write_err)));
                            }
                        } else {
                            XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.init_params_null)));
                        }
                    } else {
                        XBus.post(new ConnStatus(regPonse.status, regPonse.description));
                    }
                }

                @Override
                public void onError(Throwable e) {
                    XLog.e("registerRequest", e);
                    XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.register_other_err)));
                }
            });
        }else{
            XBus.post(new ConnStatus(GlobalConfig.STATUSCODE_FAILED,context.getString(R.string.unable_get_credentials)));
        }
    }


    public static void pingTest(List<String> pingUrl, PingListener listener) {
        final AtomicInteger batch = new AtomicInteger(0);
        int threadNum = pingUrl.size();
        final ExecutorService executor = Executors.newFixedThreadPool(threadNum);
        List<String> list = new ArrayList<>();
        final Scheduler scheduler = Schedulers.from(executor);
        Observable.range(0, pingUrl.size())
                .groupBy(new Function<Integer, Integer>() {
                    @Override
                    public Integer apply(@NonNull Integer integer) throws Exception {
                        return batch.getAndDecrement();
                    }
                })
                .flatMap(new Function<GroupedObservable<Integer, Integer>, ObservableSource<?>>() {
                    @Override
                    public ObservableSource<?> apply(@NonNull GroupedObservable<Integer, Integer> integerIntegerGroupedObservable) throws Exception {
                        return integerIntegerGroupedObservable.observeOn(scheduler)
                                .map(new Function<Integer, String>() {

                                    @Override
                                    public String apply(@NonNull Integer integer) throws Exception {
                                        return PingUtils.ping(pingUrl.get(integer.intValue()), 3, new StringBuffer());
                                    }
                                });
                    }
                }).doFinally(new Action() {
            @Override
            public void run() throws Exception {
                executor.shutdown();
                System.out.println("ping --end");
                //ping结果比较，获取最佳的链接
                if (listener != null) {
                    listener.pingResult(compare(list));
                }

            }
        })
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(@NonNull Object o) throws Exception {
                        list.add((String) o);
                    }
                });
    }

    /**
     * 比较获取最优的服务端链接
     */
    private static String compare(List<String> list) {
        Collections.sort(list, (s1, s2) -> {
            String[] o1 = s1.split("#");
            String[] o2 = s2.split("#");
            if (o1.length > o2.length) {
                return -1;
            } else if (o1.length < o2.length) {
                return 1;
            } else if ((o1.length == o2.length) && (o1.length == 1)) {
                return 1;
            } else {
                return Float.parseFloat(o1[1]) >= Float.parseFloat(o2[1]) ? 1 : -1;
            }

        });
        return list.get(0).split("#")[0];
    }
}
