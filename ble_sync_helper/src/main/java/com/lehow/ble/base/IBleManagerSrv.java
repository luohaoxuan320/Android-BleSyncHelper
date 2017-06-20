package com.lehow.ble.base;

/**
 * Created by lehow on 2017/4/27.
 */

public interface IBleManagerSrv {

    boolean connectDirectly(final String macAddress);

    void setConnModel(BleConnModel connModel);

    BleConnModel getCurConnModel();

    void disconnect();

    boolean execute(final BaseRequest baseCharAction, OnGattEventCallback onGattEventCallback);

    boolean isConnected();

    boolean addOnConnectionChangeListener(OnConnectionChangeListener connectionChangeListener);

    boolean removeOnConnectionChangeListener(OnConnectionChangeListener connectionChangeListener);

    String getCurrentConnectedDeviceAddress();
    String getCurrentConnectedDeviceName();

    boolean isBleEnable();

}
