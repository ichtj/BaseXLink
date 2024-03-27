1,连接：注册 获取代理服务器 以服务器返回参数进行连接
    获取最优服务器列表[获取其中一个]
    获取本地配置文件[防止重复注册]

2,断开：[主动断开|通讯中断|连接丢失]

3,发送数据：接收主题：[dev|app]/sn
           事件：evt/SSID     设备处理act >> event    平台回复：act >> event-resp
           属性：up/SSID      设备处理act >> upload   平台回复：act >> upload-resp
           方法：             设备处理act >> cmd      平台回复：act >> cmd-resp

4,接收数据：[主动推送数据和接收平台指令都需要有回应]
         主动：event→Iot   Iot回复
              upload→Iot   Iot回复

         被动： iot→cmd     dev回复
               iot→set     dev回复
               iot→get     dev回复

5,回调：  连接成功     connectComplete(boolean reconnect, String serverURI);
         连接失败     connectFail(boolean isPing, String description);
         连接丢失     connectLost(Throwable throwable);
         消息到达     messageArrived(BaseData msgData);
         平台回应     messageReply(String act,String iid);
         消息推送完成 pushComplete(BaseData data);
         消息推送异常 pushFail(String description);
         订阅成功     subscribeComplete();
         订阅失败     subscribeFail(String description);

