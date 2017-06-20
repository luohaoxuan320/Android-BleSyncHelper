package com.lehow.ble.base.request;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.lehow.ble.base.BaseRequest;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/19.
 */

public class ReadRequest extends BaseRequest {
    public ReadRequest(UUID serviceUuid, UUID charUuid) {
        super(serviceUuid, charUuid);
    }

    @Override
    protected boolean execute(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        return bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
    }
}
