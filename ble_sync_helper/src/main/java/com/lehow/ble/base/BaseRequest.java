package com.lehow.ble.base;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/19.
 */

public abstract class BaseRequest {
    private UUID serviceUuid;
    private UUID charUuid;

    private OnGattEventCallback onGattEventCallback;
    public BaseRequest(UUID serviceUuid, UUID charUuid) {
        this.serviceUuid = serviceUuid;
        this.charUuid = charUuid;
    }

    public UUID getServiceUuid() {
        return serviceUuid;
    }

    public UUID getCharUuid() {
        return charUuid;
    }

    public OnGattEventCallback getOnGattEventCallback() {
        return onGattEventCallback;
    }

    public void setOnGattEventCallback(OnGattEventCallback onGattEventCallback) {
        this.onGattEventCallback = onGattEventCallback;
    }

    protected abstract boolean execute(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic);

    /**
     * 需要初始化Characteristic
     * readRssi的时候不需要初始化
     * @return
     */
    public boolean needInitCharacteristic(){
        return true;
    }

}
