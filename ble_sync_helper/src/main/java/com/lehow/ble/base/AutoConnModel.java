package com.lehow.ble.base;

import android.text.TextUtils;
import android.util.Log;

/**
 * Created by lehow on 2017/4/21.
 */

public class AutoConnModel extends BleConnModel {


    private static final String TAG = "AutoConnModel";
    private int curStep = 0;

    private String devAddress;
    private String devName;

    public AutoConnModel(String devAddress, String devName) {
        this.devAddress = devAddress;
        this.devName = devName;
        Log.d(TAG, "OdmConnModel() called with: devAddress = [" + devAddress
                + "], devName = [" + devName + "]");
    }

    @Override
    public void reset() {
        curStep = 0;
    }

    @Override
    public boolean needConnect() {
        if (TextUtils.isEmpty(getTheDeviceAddress())) {
            return false;
        }
        return true;
    }

    @Override
    public long getDelayTimeMills() {
        int timeDelay = 0;
        if (curStep < 6) {//15S 后再扫描
            timeDelay = 15 * 1000;
        } else if (curStep < 6 * 2) {//1分钟后再扫描
            timeDelay = 60 * 1000;
        } else if (curStep < 6 * 3) {//5分钟
            timeDelay = 5 * 60 * 1000;
        } else {//30分钟
            timeDelay = 30 * 60 * 1000;
            curStep = 6 * 3;
        }
        curStep++;
        Log.d(TAG, "getDelayTimeMills() curStep="+curStep+" timeDelay="+timeDelay);

        return timeDelay;
    }

    @Override
    public boolean needScanBefore() {
        return true;
    }

    public String getTheDeviceAddress() {
        return devAddress;
    }

    @Override
    public String getTheDeviceName() {
        return devName;
    }
}
