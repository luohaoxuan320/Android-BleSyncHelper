package com.lehow.ble.base;

import android.bluetooth.BluetoothDevice;

/**
 * Created by lehow on 2017/4/13.
 */

public interface OnTheScanResult {
    void onResult(BluetoothDevice bluetoothDevice);

    void onScanFailed(int errorCode);
}
