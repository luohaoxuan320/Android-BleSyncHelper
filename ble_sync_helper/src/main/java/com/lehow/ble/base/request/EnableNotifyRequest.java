package com.lehow.ble.base.request;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.lehow.ble.base.BaseRequest;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/19.
 */

public class EnableNotifyRequest extends BaseRequest {
    protected UUID GATT_NOTIFY_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private boolean isEnable = true;

    public EnableNotifyRequest(UUID serviceUuid, UUID charUuid) {
        super(serviceUuid, charUuid);
    }
    public EnableNotifyRequest(UUID serviceUuid, UUID charUuid, boolean isEnable) {
        super(serviceUuid, charUuid);
        this.isEnable = isEnable;
    }
    protected void logi(final String message) {
        Log.i(getClass().getSimpleName(), message);
    }
    protected void loge(final String message) {
        Log.i(getClass().getSimpleName(), message);
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(boolean enable) {
        isEnable = enable;
    }

    @Override
    protected boolean execute(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        boolean b = bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, isEnable);
        if (!b) {
            loge(" open local notify failed");
            return false;
        }
        // enable notifications on the device
        final BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(GATT_NOTIFY_CONFIG);
        descriptor.setValue(isEnable?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        logi("gatt.writeDescriptor(" + descriptor.getUuid() +", value=0x01-00)");
        return bluetoothGatt.writeDescriptor(descriptor);
    }

}
