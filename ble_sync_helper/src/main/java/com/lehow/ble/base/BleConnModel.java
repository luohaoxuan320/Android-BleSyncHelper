package com.lehow.ble.base;

/**
 * Created by lehow on 2017/4/13.
 */

public abstract class BleConnModel {

    public void reset() {
    }

    public abstract boolean needConnect();

    public long getDelayTimeMills() {
        return 15 * 1000;
    }

    public long getScanTimeOut() {
        return 10 * 1000;
    }

    public boolean needScanBefore() {
        return true;
    }

    public abstract String getTheDeviceAddress();

    public abstract String getTheDeviceName();

}
