package com.lehow.ble.base.request;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.lehow.ble.base.BaseRequest;

/**
 * Created by lehow on 2017/4/24.
 */

public class ReadRssiRequest extends BaseRequest {
    private static ReadRssiRequest readRequest=new ReadRssiRequest();
    public static ReadRssiRequest getInstance() {
        return readRequest;
    }

    private ReadRssiRequest() {
        super(null, null);
    }

    @Override
    protected boolean execute(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        return bluetoothGatt.readRemoteRssi();
    }

    @Override
    public boolean needInitCharacteristic() {
        return false;
    }

}
