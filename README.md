# Android-BleSyncHelper
一个将android BLE操作同步处理的辅助类，简化蓝牙的操作处理，防止多线程操作可能出现的异常情况。同时默认加入了重连的处理

# 类结构图如下
![image.png](http://upload-images.jianshu.io/upload_images/1760078-ec15ab171fb3ea85.png?imageMogr2/auto-orient/strip%7CimageView2/2/w/1240)


# 示例
##  获取实例

```
//获取实例
iBleManagerSrv = BleOperateManager.getInstance(this);

```
## 扫描设备

```
 //扫描设备
 BleScannerHelper.getScannerInstance().scanDevice(null, new ScanCallback() {
     @Override
     public void onScanResult(int callbackType, ScanResult result) {
         super.onScanResult(callbackType, result);
     }
     @Override
     public void onBatchScanResults(List<ScanResult> results) {
         super.onBatchScanResults(results);
         //返回扫描到的设备
     }
     @Override
     public void onScanFailed(int errorCode) {
         super.onScanFailed(errorCode);
         //扫描失败
     }
 });
```

## 扫描特征服务

```
        //扫描特征服务设备,如扫描含标准心率服务的设备
        BleScannerHelper.getScannerInstance().scanDevice(UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB"), new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
            }
        });
```

## 扫描指定设备


```
 //扫描指定设备
 BleScannerHelper.getScannerInstance().scanTheDevice("你的mac地址", new OnTheScanResult() {
     @Override
     public void onResult(BluetoothDevice bluetoothDevice) {
         if (bluetoothDevice != null) {
             //找到了设备
         }else{
             //没找到设备
         }
     }
     @Override
     public void onScanFailed(int errorCode) {
     }
 });
```

## 连接设备

```
iBleManagerSrv.connectDirectly("你的mac地址");
```
## 监听蓝牙连接变化和取消监听

```
SimpleConnectionChangeListener simpleConnectionChangeListener=new SimpleConnectionChangeListener() {
    @Override
    public void onConnected() {
        //连接上
    }
    @Override
    public void onDisconnected() {
        //连接断开
    }
    @Override
    public void onConnectedFailed(int errCode) {
        //连接失败
    }
    @Override
    public void onConnecting() {
        //正在连接
    }
};
//监听，蓝牙连接变化
iBleManagerSrv.addOnConnectionChangeListener(simpleConnectionChangeListener);
//移除蓝牙连接变化
iBleManagerSrv.removeOnConnectionChangeListener(simpleConnectionChangeListener);
```

## 打开Notify并监听数据变化

```
 //打开Notify监听数据变化
 iBleManagerSrv.execute(new EnableNotifyRequest(UUID.fromString("你是服务UUID"), UUID.fromString("你的特征UUID"), true), new OnGattEventCallback() {
     @Override
     public void onReceivedData(UUID char_uuid, byte[] data) {
         //数据返回
     }
     @Override
     public void onOpResult(BaseRequest theRequest, int errCode) {
         if (errCode != OnGattEventCallback.ACTION_OK) {
             //蓝牙操作失败
         }
     }
 });
```
## 关闭Notify取消监听

```
//关闭notify取消监听，如果不关注执行结果，回调接口可以直接null
iBleManagerSrv.execute(new EnableNotifyRequest(UUID.fromString("你是服务UUID"), UUID.fromString("你的特征UUID"), false), new OnGattEventCallback() {
    @Override
    public void onReceivedData(UUID char_uuid, byte[] data) {
        //此处无数据返回
    }
    @Override
    public void onOpResult(BaseRequest theRequest, int errCode) {
        if (errCode != OnGattEventCallback.ACTION_OK) {
            //蓝牙操作失败
        }
    }
});
```
## 写数据

```
//写数据 has response
WriteRequest writeRequest = new WriteRequest(UUID.fromString("你是服务UUID"), UUID.fromString("你的特征UUID"));
writeRequest.setValue(new byte[]{});//根据MTU来限制大小
iBleManagerSrv.execute(writeRequest, new OnGattEventCallback() {
    @Override
    public void onReceivedData(UUID char_uuid, byte[] data) {
        //这里不会有数据返回
    }
    @Override
    public void onOpResult(BaseRequest theRequest, int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //写操作成功
        }
    }
});
//写数据 no response
WriteRequest writeRequestNoRsp =  WriteRequest.getNoRspInstance(UUID.fromString("你是服务UUID"), UUID.fromString("你的特征UUID"));
writeRequestNoRsp.setValue(new byte[]{});//根据MTU来限制大小
iBleManagerSrv.execute(writeRequestNoRsp, new OnGattEventCallback() {
    @Override
    public void onReceivedData(UUID char_uuid, byte[] data) {
        //这里不会有数据返回
    }
    @Override
    public void onOpResult(BaseRequest theRequest, int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //写操作成功
        }
    }
});

```
## 读数据

```
//读数据
iBleManagerSrv.execute(new ReadRequest(UUID.fromString("你是服务UUID"), UUID.fromString("你的特征UUID")), new OnGattEventCallback() {
    @Override
    public void onReceivedData(UUID char_uuid, byte[] data) {
        //数据会从此处返回
    }
    @Override
    public void onOpResult(BaseRequest theRequest, int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //操作成功
        }
    }
});
```
## 读RSSI信号强度

```
//读RSSI信号强度
iBleManagerSrv.execute(ReadRssiRequest.getInstance(), new IOpResponse<SimpleIntegerDataParser,Integer>() {
    @Override
    public void onReceivedData(Integer integer) {
        //RSSI 值返回
    }
    @Override
    public void onOpResult(int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //操作成功
        }
    }
});
```
## 读取电量（标准服务）

```
//读取电量
iBleManagerSrv.execute(BatteryHandle.getReadRequest(), new IOpResponse<BatteryHandle,Integer>() {
    @Override
    public void onReceivedData(Integer integer) {
        //数据返回
    }
    @Override
    public void onOpResult(int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //操作成功
        }
    }
});
```
## 读取设备信息（标准服务）

```
//读取设备信息
iBleManagerSrv.execute(DeviceInfoHandle.getReadFwRequest(), new IOpResponse<DeviceInfoHandle,String>() {
    @Override
    public void onReceivedData(String s) {
        //数据返回
    }
    @Override
    public void onOpResult(int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //操作成功
        }
    }
});
```
## 监听心率数据（标准服务）

```
//打开心率Notify通道，并监听数据
iBleManagerSrv.execute(HRMHandle.getEnableNotifyRequest(true), new IOpResponse<HRMHandle,HRMHandle.HREntity>() {
    @Override
    public void onReceivedData(HRMHandle.HREntity hrEntity) {
        //数据返回
    }
    @Override
    public void onOpResult(int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //操作成功
        }
    }
});

//打开Notify后，记得要关闭，不然会内存泄漏，回调null，不关心操作结果
iBleManagerSrv.execute(HRMHandle.getEnableNotifyRequest(false), null);
```
## 监听跑步数据（标准服务）

```
//监听跑步数据
iBleManagerSrv.execute(RunningHandle.getEnableNotifyRequest(true), new IOpResponse<RunningHandle,RunningHandle.RunningEntity>() {
    @Override
    public void onReceivedData(RunningHandle.RunningEntity runningEntity) {
        //数据返回
    }
    @Override
    public void onOpResult(int errCode) {
        if (errCode == OnGattEventCallback.ACTION_OK) {
            //操作成功
        }
    }
});
//记得关闭
```
## 设置连接模式

```
//单次连接 只会连接一次，连接失败或者连接上后再断开，不会再去连接
iBleManagerSrv.setConnModel(new DefaultConnModel());
iBleManagerSrv.connectDirectly("mac地址");
//使用默认重连模式
iBleManagerSrv.setConnModel(new AutoConnModel("mac地址", "设备名"));
iBleManagerSrv.connectDirectly("mac地址");
```
也可以自己继承BleConnModel 实现自定义的重连间隔和逻辑



# 注意事项
1. Notify

   打开后，用完一定要关闭，防止内存泄漏
    
   Notify的通道具有唯一排他性，如果你在A处open后，在B处又open，B处的会将A处的监听给覆盖掉了。同样在B处的关闭动作也会讲A处的监听给关掉。所以若一个Notify多处同时用到，需要在业务上再次封装处理。
 
2.写数据

   setValue 数据大小要自己根据MTU来限制。连续两次setValue会导致数据覆盖，所以每一个新数据对应一个WriteRequest。除非确保数据写完了再setValue。



3.重连模式

        //使用默认的重连
    iBleManagerSrv.setConnModel(new AutoConnModel("mac 地址", "设备名"));
    iBleManagerSrv.connectDirectly("mac 地址");
    
    
此处连接成功后断开，或者连接失败，会使用AutoConnModel配置的重连机制去重连，连接的设备是AutoConnModel中指定的mac 地址，所以不要出现connectDirectly的mac地址和AutoConnModel中设置的不一样，当然你这样做也可以
