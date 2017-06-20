package com.lehow.ble.base;

/**
 * Created by lehow on 2017/4/21.
 * 默认断开就不再连接了
 */

public class DefaultConnModel extends BleConnModel {

    @Override
    public boolean needConnect() {
        return false;
    }

    @Override
    public String getTheDeviceAddress() {
        return null;
    }

    @Override
    public String getTheDeviceName() {
        return null;
    }

}
