package com.lehow.ble;

import android.bluetooth.BluetoothDevice;
import android.support.v4.widget.SearchViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.lehow.ble.base.AutoConnModel;
import com.lehow.ble.base.BaseRequest;
import com.lehow.ble.base.BleOperateManager;
import com.lehow.ble.base.BleScannerHelper;
import com.lehow.ble.base.DefaultConnModel;
import com.lehow.ble.base.IBleManagerSrv;
import com.lehow.ble.base.OnGattEventCallback;
import com.lehow.ble.base.OnTheScanResult;
import com.lehow.ble.base.SimpleConnectionChangeListener;
import com.lehow.ble.base.request.EnableNotifyRequest;
import com.lehow.ble.base.request.ReadRequest;
import com.lehow.ble.base.request.ReadRssiRequest;
import com.lehow.ble.base.request.WriteRequest;
import com.lehow.ble.ble_base_srv.BatteryHandle;
import com.lehow.ble.ble_base_srv.DeviceInfoHandle;
import com.lehow.ble.ble_base_srv.HRMHandle;
import com.lehow.ble.ble_base_srv.IOpResponse;
import com.lehow.ble.ble_base_srv.RunningHandle;
import com.lehow.ble.ble_base_srv.SimpleIntegerDataParser;

import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

public class MainActivity extends AppCompatActivity {

    private IBleManagerSrv iBleManagerSrv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //获取实例
        iBleManagerSrv = BleOperateManager.getInstance(this);

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

        iBleManagerSrv.connectDirectly("你的mac地址");

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

        //单次连接 只会连接一次，连接失败或者连接上后再断开，不会再去连接
        iBleManagerSrv.setConnModel(new DefaultConnModel());
        iBleManagerSrv.connectDirectly("mac地址");

        //使用默认重连模式
        iBleManagerSrv.setConnModel(new AutoConnModel("mac地址", "设备名"));
        iBleManagerSrv.connectDirectly("mac地址");

    }
}
