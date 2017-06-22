package com.lehow.ble.base;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by lehow on 2017/4/13.
 * TODO:bleConnGatt 断开后，再次连接上后，UserGattHandle中的bleConnGatt怎么更新为最新的连接对象
 *
 */

public class BleOperateManager extends HandlerThread implements SimpleBleGattCallback , IBleManagerSrv {
    private UUID GATT_NOTIFY_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private static final String TAG = "BleOperateManager";
    private BleBaseControl bleControl;

    private BluetoothGatt bleConnGatt;

    private Handler mLocalHandler;

    private BaseRequest currentCharAction;

    private final Object mLock = new Object();
    private boolean mRequestCompleted=false;

    private int mOpState= STATE_DISCONNECTED;

    public static final int STATE_DISCONNECTED = 0;
    public static final int STATE_CONNECTING = 1;
    //    public static final int STATE_CONNECTED = 2;
    public static final int STATE_DISCONNECTING = 2;
    public static final int STATE_ACTIONING = 3;
    /**
     * 连接上，并且没有正在做操作
     */
    public static final int STATE_IDLE = 4;

    public static final int STATE_CONNECT_IN_QUEUE = 5;


    /**
     *缓存 开启notify的OnGattEventCallback ，notify的数据，会通过这个返回
     *
     * 先只考虑一个UUID只对应一个dataCallback的情况，有可能一个UUID 对应多个dataCallback
     * 已与嵌入式协商好，每个Char 的Characteristic 都是唯一的
     * TODO: 先不考虑不同service下面的Characteristic是相同的情况
     */
    private HashMap<UUID, OnGattEventCallback> notifyDataCallbackHashMap = new HashMap<>();
    /**
     * 缓存已经找到的Characteristic,避免多次查找，这里会自动初始化Characteristic
     *
     * 已与嵌入式协商好，每个Char 的Characteristic 都是唯一的
     * 不会出现多个service下面的Characteristic是相同的
     * TODO: 先不考虑不同service下面的Characteristic是相同的情况
     */
    private HashMap<UUID, BluetoothGattCharacteristic> cacheGattCharacteristicHashMap = new HashMap<>();

    private LinkedList<OnConnectionChangeListener> connectionChangeListenerLinkedList = new LinkedList<>();

    private BleConnModel bleConnModel=new DefaultConnModel();

    /**
     * 用户主动断开的连接不重连.再次连接的时候需要重置这个标志
     */
    private boolean isUserDisconnected = false;

    private static BleOperateManager bleOperateManager = null;

    private Handler mainThreadHandler;

    public synchronized static IBleManagerSrv getInstance(Context context) {
        if (bleOperateManager == null) {
            bleOperateManager = new BleOperateManager(context.getApplicationContext());
        }
        return bleOperateManager;
    }

    private BleOperateManager(Context context) {
        super("BleOperateManager");
        bleControl = new BleBaseControl(context,this);
        this.start();
        mainThreadHandler = new Handler(Looper.getMainLooper());
        mLocalHandler = new Handler(getLooper()){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
            }
        };
    }

    private OnTheScanResult innerOnTheScanResult =new OnTheScanResult() {
        @Override
        public void onResult(BluetoothDevice bluetoothDevice) {
            if (bluetoothDevice!=null) {
//                bleConnModel.reset();//找到设备后，复位连接状态，到快速模式。connectDirectly中会做这个动作
                Log.i(TAG, "innerOnTheScanResult: find the device");
                innerConnectDirectly(bluetoothDevice.getAddress(),true);
            }else{
                checkTheConnModel();
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            checkTheConnModel();
        }
    };
    /**
     * 连接指定的设备，连接前，先进行扫描动作
     * @param macAddress
     */
    public void connectWithScan(String macAddress) {
        isUserDisconnected = false;
        BleScannerHelper.getScannerInstance().scanTheDevice(macAddress, new OnTheScanResult() {
            @Override
            public void onResult(BluetoothDevice bluetoothDevice) {
                if (bluetoothDevice!=null) {
                    connectDirectly(bluetoothDevice.getAddress());
                }else{
                    notifyConnectionStateChange(OnConnectionChangeListener.STATE_CONNECTED_FAILED|OnConnectionChangeListener.FAILED_SCAN);
                    checkTheConnModel();
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                notifyConnectionStateChange(OnConnectionChangeListener.STATE_CONNECTED_FAILED|OnConnectionChangeListener.FAILED_SCAN);
                checkTheConnModel();
            }
        });
    }

    private void innerConnectWithScan() {
        long delayTimeMills = bleConnModel.getDelayTimeMills();
        mLocalHandler.postDelayed(innerScanRunnable, delayTimeMills);
    }

    private Runnable innerScanRunnable=new Runnable() {
        @Override
        public void run() {
            if (mOpState==STATE_DISCONNECTED||mOpState==STATE_DISCONNECTING) {//连接确实断开才扫描，避免出现连接上了，后台还傻傻的扫描
                if (!isBleEnable())return;
                BleScannerHelper.getScannerInstance().scanTheDevice(bleConnModel.getTheDeviceAddress(), innerOnTheScanResult);
            }
        }
    };

    /**
     * 直接连接
     * 用于扫描结果界面的连接，如果之前没做扫描，不要用这个连接成功率不高
     * @param macAddress
     */
    private boolean innerConnectDirectly(final String macAddress, boolean needResetConn) {

        Log.i(TAG, "--connectDirectly: ");
        mLocalHandler.removeCallbacks(innerScanRunnable);//移除潜在的扫描
        BleScannerHelper.getScannerInstance().stopScan();//停止扫描
        if (!bleControl.isBleEnable()) {
            mOpState = STATE_DISCONNECTED;
            Log.e(TAG, "connectDirectly: 蓝牙未打开");
            notifyConnectionStateChange(OnConnectionChangeListener.STATE_CONNECTED_FAILED|OnConnectionChangeListener.FAILED_BEL_UNOPENED);
            return false;
        }
        if (needResetConn)bleConnModel.reset();
        synchronized (BleOperateManager.this) {
            if ((mOpState == STATE_DISCONNECTED || mOpState == STATE_DISCONNECTING)&&mOpState!=STATE_CONNECT_IN_QUEUE) {//连接已断开，或者正在断开连接。则能发起连接.并且队列中不能有正在发起的连接，防止比如正在断开连接，能同时发送多个连接的runnable
//            mLocalHandler.postAtFrontOfQueue 不把他插入到队列头部，让队列中还未执行的action先空跑一遍，回掉它的onDisconnected方法
                //这里也可以用postAtFrontOfQueue进行插队重连，这样的话就可以维持一种断开立即重试的感觉，不过Characteristic的初始化就是另一个需要解决的问题
                final boolean b = mLocalHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "run: mOpState="+mOpState);
                        if (mOpState==STATE_ACTIONING||mOpState==STATE_IDLE||mOpState==STATE_CONNECTING){
//                            if (onConnectResult!=null)onConnectResult.onStateResult(OnConnectResult.SR_ERR_CONNECTING);
                            return;//做一个二次确认，防止多个连接
                        }
                        Log.i(TAG, "connectDirectly: thread="+Thread.currentThread());
                        bleConnGatt = bleControl.connect(macAddress);
                        if (bleConnGatt != null && bleControl.isRealConnected()) {//连接上
                            Log.i(TAG, "connectDirectly: 连接上了");
                            mOpState = STATE_IDLE;
                            notifyConnectionStateChange(OnConnectionChangeListener.STATE_CONNECTED);
                            if(bleConnModel!=null)bleConnModel.reset();//连接上后，重置重连逻辑
                        }else{//关闭连接
                            Log.i(TAG, "connectDirectly: 未连接上"+bleControl.isRealConnected());
                            if (bleConnGatt == null)
                                checkTheConnModel();
                            closeConnect();
                            notifyConnectionStateChange(OnConnectionChangeListener.STATE_CONNECTED_FAILED|OnConnectionChangeListener.FAILED_CONNECTED);
                        }
                    }
                });
                if (b){
                    mOpState = STATE_CONNECT_IN_QUEUE;
                    notifyConnectionStateChange(OnConnectionChangeListener.STATE_CONNECTING);
                }
                return b;
            }else{
                Log.i(TAG, "connectDirectly: 蓝牙已连接 或者正在连接 mOpState=" + mOpState);
                return false;
            }
        }


    }

    private void notifyConnectionStateChange(final int state){
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                for (OnConnectionChangeListener onConnectionChangeListener : connectionChangeListenerLinkedList) {
                    onConnectionChangeListener.onStateChange(state);
                }
            }
        });
    }

    /**
     * 直接连接
     * 用于扫描结果界面的连接，如果之前没做扫描，不要用这个连接成功率不高
     * @param macAddress
     */
    public boolean connectDirectly(final String macAddress) {
        isUserDisconnected = false;
        return innerConnectDirectly(macAddress, true);
    }



    /**
     *设置连接策略
     * 如：需要重连或则之连接一次
     * 设置这个的时候若连接时断开的，是不会重新连接的。需要自己去判断
     * @param connModel
     */
    public void setConnModel(BleConnModel connModel) {
        this.bleConnModel = connModel;
        mLocalHandler.removeCallbacks(innerScanRunnable);//切换模式的时候，要移除潜在的扫描
    }

    @Override
    public BleConnModel getCurConnModel() {
        return bleConnModel;
    }

    /**
     * 断开当前设备的连接
     * 实际是close
     */
    public void disconnect() {
        innerDisconnect(true);
    }

    private void innerDisconnect(final boolean isUser) {
        if(bleConnGatt!=null)mLocalHandler.post(new Runnable() {
            @Override
            public void run() {
                isUserDisconnected = isUser;
                closeConnect();
            }
        });

    }

    private void closeConnect(){
        mOpState = STATE_DISCONNECTING;
        if(bleConnGatt!=null)bleControl.terminateConnection(bleConnGatt);
        bleConnGatt = null;
        mOpState = STATE_DISCONNECTED;
    }


    public boolean execute(final BaseRequest baseCharAction, OnGattEventCallback onGattEventCallback) {
        if (!bleControl.isBleEnable()) {
            mOpState = STATE_DISCONNECTED;
            Log.e(TAG, "connectDirectly: 蓝牙未打开");
            return false;
        }
        if (isConnectionInvalidate()) {
            Log.i(TAG, "execute: 蓝牙连接无效 断开或者正在连接中 mOpState="+mOpState);
            return false;
        }
        baseCharAction.setOnGattEventCallback(onGattEventCallback);
        mLocalHandler.post(new Runnable() {
            @Override
            public void run() {

                synchronized (mLock) {//获取锁，防止这边还在操作， onDisconnected先返回了
                    Log.w(TAG, "==run==" + baseCharAction + " mOpState=" + mOpState);
                    if (isConnectionInvalidate()) {//断开后，队列会快速执行一遍
                        baseCharAction.getOnGattEventCallback().onOpResult(currentCharAction,OnGattEventCallback.ERR_INVALID_CONNECTION);
                        mOpState=STATE_DISCONNECTED;
                        return;
                    }
                    mOpState = STATE_ACTIONING;
                    currentCharAction = baseCharAction;
                    if (baseCharAction.needInitCharacteristic()) {
                        BluetoothGattCharacteristic charCharacteristic = findTheGattCharacteristic(baseCharAction.getServiceUuid(), baseCharAction.getCharUuid());
                        if (charCharacteristic == null) {
                            baseCharAction.getOnGattEventCallback().onOpResult(baseCharAction, OnGattEventCallback.ERR_UNFIND_CHAR);
                            mOpState = STATE_IDLE;
                            return;
                        }
                        baseCharAction.execute(bleConnGatt, charCharacteristic);
                    }else{
                        baseCharAction.execute(bleConnGatt, null);
                    }
                    Log.w(TAG, "==wait==");
                    mRequestCompleted = false;
                    waitUntilActionResponse();
                    if (mOpState != STATE_DISCONNECTED) {
                        mOpState = STATE_IDLE;
                    }else{
                        currentCharAction.getOnGattEventCallback().onOpResult(currentCharAction,OnGattEventCallback.ERR_INVALID_CONNECTION);
                    }
                    currentCharAction = null;
                    Log.w(TAG, "==notify==");
                }

            }
        });
        return true;
    }

    public boolean isConnected(){
        return mOpState == STATE_IDLE || mOpState == STATE_ACTIONING;
    }

    public boolean addOnConnectionChangeListener(OnConnectionChangeListener connectionChangeListener) {
        if (connectionChangeListener==null) {
            return false;
        }
        return connectionChangeListenerLinkedList.add(connectionChangeListener);
    }

    public boolean removeOnConnectionChangeListener(OnConnectionChangeListener connectionChangeListener) {
        return connectionChangeListenerLinkedList.remove(connectionChangeListener);
    }


    /**
     * 连接是否无效，true 无效
     * @return
     */
    private boolean isConnectionInvalidate(){
        return !(mOpState == STATE_IDLE || mOpState == STATE_ACTIONING);
    }

    /**
     * 开启notify的地方 要负责移除，否则会出现内存泄漏
     * 可以通过Enable false 来移除
     * @param baseCharAction
     */
    public void removeNotifyOpCharAction(BaseRequest baseCharAction) {
        //移除notify的callback
        notifyDataCallbackHashMap.remove(baseCharAction.getCharUuid());
    }

    @Override
    public String getCurrentConnectedDeviceAddress() {
        return bleConnGatt==null?null:bleConnGatt.getDevice().getAddress();
    }

    @Override
    public String getCurrentConnectedDeviceName() {
        Log.i(TAG, "getCurrentConnectedDeviceName: " + bleConnModel
                .getTheDeviceName() + " " + bleConnGatt.getDevice().getName());
        String name = bleConnModel == null ? null : bleConnModel
                .getTheDeviceName();
        if (TextUtils.isEmpty(name)) {
            name = bleConnGatt == null ? null : bleConnGatt.getDevice()
                    .getName();
        }
        return name;
    }

    @Override
    public boolean isBleEnable() {
        return bleControl.isBleEnable();
    }

    private void waitUntilActionResponse() {
        try {
            while (!mRequestCompleted) {
                mLock.wait();
            }
        } catch (final InterruptedException e) {
            Log.e(TAG,"Sleeping interrupted", e);
        }
    }

    protected void notifyLock() {
        mRequestCompleted = true;
        // Notify waiting thread
        synchronized (mLock) {
            mLock.notifyAll();
        }
    }

    private BluetoothGattCharacteristic findTheGattCharacteristic(UUID serviceUuid, UUID charUuid) {
        BluetoothGattCharacteristic charCharacteristic = cacheGattCharacteristicHashMap.get(charUuid);
        if (charCharacteristic == null) {
            charCharacteristic=initTheCharacteristic(serviceUuid, charUuid);
            if (charCharacteristic != null) {//找到了，保存
                cacheGattCharacteristicHashMap.put(charUuid, charCharacteristic);
            }
            return charCharacteristic;
        }
        return charCharacteristic;
    }

    private BluetoothGattCharacteristic initTheCharacteristic(UUID serviceUuid, UUID charUuid) {
        BluetoothGattService service = bleConnGatt.getService(serviceUuid);
        if (service==null){
            Log.e(TAG, "initTheCharacteristic: can't find service uuid=" + serviceUuid);
            return null;
        }
        BluetoothGattCharacteristic charCharacteristic = service.getCharacteristic(charUuid);
        return charCharacteristic;
    }

    private void dispatchOpData(final UUID char_uuid, final byte[] data) {
        final OnGattEventCallback iGattDataCallback = currentCharAction.getOnGattEventCallback();
        if (iGattDataCallback == null) {
            Log.e(TAG, "dispatchOpData: 未能找到与该char_uuid"+char_uuid+" 对应的 iGattDataCallback" );
            return;
        }
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                iGattDataCallback.onReceivedData(char_uuid,data);
            }
        });
    }
    private void dispatchOpResult(final UUID char_uuid, final int status) {
        final OnGattEventCallback iGattDataCallback =currentCharAction.getOnGattEventCallback();
        if (iGattDataCallback == null) {
            Log.e(TAG, "dispatchOpResult: 未能找到与该char_uuid"+char_uuid+" 对应的 iGattDataCallback" );
            return;
        }
        mainThreadHandler.post(new Runnable() {
            //防止 由于对象引用导致的，下一个BaseRequest覆盖了，当前需要返回的BaseRequest
            final  BaseRequest baseRequest = currentCharAction;
            @Override
            public void run() {
                iGattDataCallback.onOpResult(baseRequest,status);
            }
        });

    }


    private void dispatchNotifyData(final UUID char_uuid, final byte[] value) {
        final OnGattEventCallback iGattDataCallback = notifyDataCallbackHashMap.get(char_uuid);
        if (iGattDataCallback == null) {
            Log.e(TAG, "dispatchOpData: 未能找到与该char_uuid"+char_uuid+" 对应的 iGattDataCallback" );
            return;
        }
        mainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                iGattDataCallback.onReceivedData(char_uuid,value);
            }
        });
    }
    private void checkIsNotifyConfigAndRegisterCallback(BluetoothGattDescriptor descriptor) {

        if (descriptor.getUuid().compareTo(GATT_NOTIFY_CONFIG)==0) {//是 open notify的操作，

            byte[] value = descriptor.getValue();
            if (value != null && value.length == 2 && value[1] == 0x00) {
                if (value[0] == 0x01) {//打开notify
                    //TODO:这里未处理多个地方使用打开同一个Characteristic的notify，需要将数据分发到这几个地方去。
                    //记录这个回掉，以回掉数据到上层，上层退出的时候，要主动移除这个callback，防止内存泄漏
                    notifyDataCallbackHashMap.put(descriptor.getCharacteristic().getUuid(), currentCharAction.getOnGattEventCallback());
                } else {//关闭notify
                    notifyDataCallbackHashMap.remove(descriptor.getCharacteristic().getUuid());
                }
            }
        }
    }


    private void checkTheConnModel(){
        if (bleConnModel==null||!bleControl.isBleEnable()||isUserDisconnected)return;
        if (bleConnModel.needConnect()) {
            if (bleConnModel.needScanBefore()) {
                innerConnectWithScan();
            }else{
                innerConnectDirectly(bleConnModel.getTheDeviceAddress(),false);
            }
        }
    }

    @Override
    public void onDisconnected() {
        Log.i(TAG, "onDisconnected: ");

        // Notify waiting thread
        synchronized (mLock) {
            notifyDataCallbackHashMap.clear();//移除所有的notify callback
            cacheGattCharacteristicHashMap.clear();//移除gatt haracteristic的缓存
            mOpState = STATE_DISCONNECTING;
            innerDisconnect(false);
            //这个分发到专门的线程，会导致read的wait动作，正在等notify，而错误的接收到这个notify
            // ，而状态又不是断开，所以后面接着read，然后一直wait。真正的断开代码不会被执行
            checkTheConnModel();
            notifyConnectionStateChange(OnConnectionChangeListener.STATE_DISCONNECTED);
            notifyLock();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            dispatchOpData(characteristic.getUuid(),characteristic.getValue());
        }else{
            dispatchOpResult(characteristic.getUuid(),status);
        }
        notifyLock();
    }



    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        Log.d(TAG, "onCharacteristicWrite() called with: gatt = [" + gatt + "], characteristic = [" + characteristic + "], value = [" + DataTransferUtils.getHexString(characteristic.getValue()) +"], status = [" + status + "]");
        dispatchOpResult(characteristic.getUuid(), status);
        notifyLock();
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        dispatchNotifyData(characteristic.getUuid(),characteristic.getValue());
        //此处不用notify 等待，否则notify上来的数据会干扰 打断read data的等待结果
       /* // Notify waiting thread
        synchronized (mLock) {
            mLock.notifyAll();
        }*/
    }


    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        // Notify waiting thread
        notifyLock();
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        Log.d(TAG, "onDescriptorWrite() called with: gatt = [" + gatt + "], descriptor = [" + descriptor.getUuid() + "], Characteristic = [" + descriptor.getCharacteristic().getUuid()+ "], status = [" + status + "]");
        if (status == BluetoothGatt.GATT_SUCCESS) {
            //检查是否是open notify动作，如果是，则缓存它的回调
            checkIsNotifyConfigAndRegisterCallback(descriptor);
        }
        dispatchOpResult(descriptor.getCharacteristic().getUuid(), status);
        notifyLock();
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            dispatchOpData(null, DataTransferUtils.intToBytes(rssi));
        }else{
            dispatchOpResult(null,status);
        }
        notifyLock();
    }

}
