package com.lehow.ble.base;

/**
 * Created by lehow on 2017/4/24.
 */

public interface OnConnectionChangeListener {

    //获取状态的掩码
    int MASK_STATE = 0xFF00;
    //获取错误的掩码
    int MASK_ERROR = 0x00FF;

    int STATE_DISCONNECTED = 0x0000;
    int STATE_CONNECTING = 0x0100;
    int STATE_CONNECTED = 0x0300;
    int STATE_CONNECTED_FAILED = 0x0400;

    /**
     *错误 蓝牙已关闭
     */
    int FAILED_BEL_UNOPENED = 0x0001;

    /**
     * 错误 连接失败
     */
    int FAILED_CONNECTED = 0x0002;
    /**
     * 扫描没有找到
     */
    int FAILED_SCAN =0x0003;

    void onStateChange(int connState);
}
