package com.lehow.ble.base;

/**
 * Created by lehow on 2017/4/26.
 */

public abstract class SimpleConnectionChangeListener implements OnConnectionChangeListener {

    @Override
    public void onStateChange(int connState) {
        switch (connState & MASK_STATE) {
            case STATE_CONNECTED:
                onConnected();
                break;
            case STATE_DISCONNECTED:
               onDisconnected();
                break;
            case STATE_CONNECTING:
                onConnecting();
                break;
            case STATE_CONNECTED_FAILED:
                onConnectedFailed(connState & MASK_ERROR);
                break;
        }
    }


    public abstract void onConnected();

    public abstract void onDisconnected();

    public abstract void onConnectedFailed(int errCode);

    public abstract void onConnecting();

}
