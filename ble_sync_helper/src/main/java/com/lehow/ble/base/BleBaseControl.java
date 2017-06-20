package com.lehow.ble.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * Created by lehow on 2017/4/13.
 */

 class BleBaseControl {

    private static final String TAG ="BleBaseControl" ;
    private Context mContext;
    private BluetoothAdapter mBluetoothAdapter;

    protected int mConnectionState;
    protected final static int STATE_DISCONNECTED = 0;
    protected final static int STATE_CONNECTING = -1;
    protected final static int STATE_CONNECTED = -2;
    protected final static int STATE_CONNECTED_AND_READY = -3; // indicates that services were discovered
    protected final static int STATE_DISCONNECTING = -4;
    protected final static int STATE_CLOSED = -5;


    private int mError;
    public static final int ERROR_MASK = 0x1000;
    /**
     * Error thrown when {@code gatt.discoverServices();} returns false.
     */
    public static final int ERROR_SERVICE_DISCOVERY_NOT_STARTED = ERROR_MASK | 0x05;
    /**
     * Thrown when the service discovery has finished but the DFU service has not been found. The device does not support DFU of is not in DFU mode.
     */
    public static final int ERROR_SERVICE_NOT_FOUND = ERROR_MASK | 0x06;
    /**
     * Thrown when the required DFU service has been found but at least one of the DFU characteristics is absent.
     * @deprecated This error will no longer be thrown. {@link #ERROR_SERVICE_NOT_FOUND} will be thrown instead.
     */
    @Deprecated
    public static final int ERROR_CHARACTERISTICS_NOT_FOUND = ERROR_MASK | 0x07;
    /**
     * Thrown when unknown response has been obtained from the target. The DFU target must follow specification.
     */
    public static final int ERROR_INVALID_RESPONSE = ERROR_MASK | 0x08;
    /**
     * Thrown when the the service does not support given type or mime-type.
     */
    public static final int ERROR_FILE_TYPE_UNSUPPORTED = ERROR_MASK | 0x09;
    /**
     * Thrown when the the Bluetooth adapter is disabled.
     */
    public static final int ERROR_BLUETOOTH_DISABLED = ERROR_MASK | 0x0A;
    /**
     * DFU Bootloader version 0.6+ requires sending the Init packet. If such bootloader version is detected, but the init packet has not been set this error is thrown.
     */
    public static final int ERROR_INIT_PACKET_REQUIRED = ERROR_MASK | 0x0B;
    /**
     * Thrown when the firmware file is not word-aligned. The firmware size must be dividable by 4 bytes.
     */
    public static final int ERROR_FILE_SIZE_INVALID = ERROR_MASK | 0x0C;
    /**
     * Thrown when the received CRC does not match with the calculated one. The service will try 3 times to send the data, and if the CRC fails each time this error will be thrown.
     */
    public static final int ERROR_CRC_ERROR = ERROR_MASK | 0x0D;
    /**
     * Thrown when device had to be paired before the DFU process was started.
     */
    public static final int ERROR_DEVICE_NOT_BONDED = ERROR_MASK | 0x0E;

    public static final int ERROR_DEVICE_TIME_OUT = ERROR_MASK | 0x0F;
    /**
     * Flag set when the DFU target returned a DFU error. Look for DFU specification to get error codes.
     */
    public static final int ERROR_REMOTE_MASK = 0x2000;
    /**
     * The flag set when one of {@link BluetoothGattCallback} methods was called with status other than {@link BluetoothGatt#GATT_SUCCESS}.
     */
    public static final int ERROR_CONNECTION_MASK = 0x4000;
    /**
     * The flag set when the {@link BluetoothGattCallback#onConnectionStateChange(BluetoothGatt, int, int)} method was called with
     * status other than {@link BluetoothGatt#GATT_SUCCESS}.
     */
    public static final int ERROR_CONNECTION_STATE_MASK = 0x8000;



    /**
     * Lock used in synchronization purposes
     */
    private final Object mLock = new Object();

    private String mDeviceAddress;

    private SimpleBleGattCallback simpleBleGattCallback;

    private Handler handler;

    /**
     * 传入的context最好是application的或者service的，不要传人activity，防止内存泄漏
     * @param mContext
     */
    public BleBaseControl(final Context mContext, SimpleBleGattCallback simpleBleGattCallback) {
        this.mContext = mContext;
        this.simpleBleGattCallback = simpleBleGattCallback;
        handler = new Handler(Looper.getMainLooper());
        initialize();
    }

    private boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            loge("Unable to initialize BluetoothManager.");
            return false;
        }
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            loge("Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            Log.d(TAG, "onConnectionStateChange() called with: gatt = [" + gatt + "], status = [" + status + "], newState = [" + newState + "]");

            // Check whether an error occurred
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    logi("Connected to GATT server。Connected to " + mDeviceAddress);
                    mConnectionState = STATE_CONNECTED;

					/*
					 *  The onConnectionStateChange callback is called just after establishing connection and before sending Encryption Request BLE event in case of a paired device.
					 *  In that case and when the Service Changed CCCD is enabled we will get the indication after initializing the encryption, about 1600 milliseconds later.
					 *  If we discover services right after connecting, the onServicesDiscovered callback will be called immediately, before receiving the indication and the following
					 *  service discovery and we may end up with old, application's services instead.
					 *
					 *  This is to support the buttonless switch from application to bootloader mode where the DFU bootloader notifies the master about service change.
					 *  Tested on Nexus 4 (Android 4.4.4 and 5), Nexus 5 (Android 5), Samsung Note 2 (Android 4.4.2). The time after connection to end of service discovery is about 1.6s
					 *  on Samsung Note 2.
					 *
					 *  NOTE: We are doing this to avoid the hack with calling the hidden gatt.refresh() method, at least for bonded devices.
					 */
                    if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_BONDED) {
                        logi("Waiting 1600 ms for a possible Service Changed indication...");
                        waitFor(1600);
                        // After 1.6s the services are already discovered so the following gatt.discoverServices() finishes almost immediately.

                        // NOTE: This also works with shorted waiting time. The gatt.discoverServices() must be called after the indication is received which is
                        // about 600ms after establishing connection. Values 600 - 1600ms should be OK.
                    }
                    // Attempts to discover services after successful connection.
                    logi("Discovering services...");
                    final boolean success = gatt.discoverServices();
                    logi("Attempting to start service discovery... " + (success ? "succeed" : "failed"));

                    if (!success) {
                        mError = ERROR_SERVICE_DISCOVERY_NOT_STARTED;
                    } else {
                        // Just return here, lock will be notified when service discovery finishes
                        return;
                    }
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    logi("Disconnected from GATT server");
                    mConnectionState = STATE_DISCONNECTED;
                    if (simpleBleGattCallback != null)
                        simpleBleGattCallback.onDisconnected();
                }
            } else {
                loge("Connection state change error: " + status + " newState: " + newState);
                if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    mConnectionState = STATE_DISCONNECTED;
                    if (simpleBleGattCallback != null)
                        simpleBleGattCallback.onDisconnected();
                }
                mError = ERROR_CONNECTION_STATE_MASK | status;
            }

            // Notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logi("Services discovered");
                mConnectionState = STATE_CONNECTED_AND_READY;
            } else {
                loge("Service discovery error: " + status);
                mError = ERROR_CONNECTION_MASK | status;
            }

            // Notify waiting thread
            synchronized (mLock) {
                mLock.notifyAll();
            }
        }

        // Other methods just pass the parameters through
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (simpleBleGattCallback != null)
                simpleBleGattCallback.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            logi("onCharacteristicRead uuid=" + characteristic.getUuid() + " value=" + DataTransferUtils.getHexString(characteristic.getValue()));
            if (simpleBleGattCallback != null)
                simpleBleGattCallback.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged() called with: gatt = [" + gatt + "], characteristic = [" + characteristic.getUuid() + "]");
            if (simpleBleGattCallback != null)
                simpleBleGattCallback.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.d(TAG, "onDescriptorWrite() called with: gatt = [" + gatt + "], descriptor = [" + descriptor.getUuid() + "]");
            if (simpleBleGattCallback != null)
                simpleBleGattCallback.onDescriptorWrite(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (simpleBleGattCallback != null)
                simpleBleGattCallback.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            if (simpleBleGattCallback != null)
                simpleBleGattCallback.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    public boolean isBleEnable(){
        return mBluetoothAdapter.isEnabled();
    }
    /**
     * Connects to the BLE device with given address. This method is SYNCHRONOUS, it wait until the connection status change from {@link #STATE_CONNECTING} to {@link #STATE_CONNECTED_AND_READY} or an
     * error occurs. This method returns <code>null</code> if Bluetooth adapter is disabled.
     *
     * @param address the device address
     * @return the GATT device or <code>null</code> if Bluetooth adapter is disabled.
     */
    public BluetoothGatt connect(final String address) {
        if (!mBluetoothAdapter.isEnabled())
            return null;

        if (TextUtils.isEmpty(address)) {
            return null;
        }
        this.mDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        mError = 0;
        logi("Connecting to the device...");
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        logi("gatt = device.connectGatt(autoConnect = false) device=" + device);
        final BluetoothGatt gatt = device.connectGatt(mContext, false, mGattCallback);
        connTimeOutRunnable.bluetoothGatt = gatt;
        handler.postDelayed(connTimeOutRunnable, 30 * 1000);//连接超时30s
        // We have to wait until the device is connected and services are discovered
        // Connection error may occur as well.
        try {
            synchronized (mLock) {
                logi("mConnectionState="+mConnectionState+" mError"+mError);
                while ((mConnectionState == STATE_CONNECTING || mConnectionState == STATE_CONNECTED) && mError == 0)
                    mLock.wait();
                if (mError == ERROR_DEVICE_TIME_OUT) {
                    return null;
                }
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
        handler.removeCallbacks(connTimeOutRunnable);
        return gatt;
    }

    private ConnTimeOutRunnable connTimeOutRunnable=new ConnTimeOutRunnable() {
        @Override
        public void run() {
            if (mConnectionState == STATE_CONNECTING || mConnectionState == STATE_CONNECTED){//还在连接等待中
                Log.e(TAG, "connect 连接超时 无返回" );
                mError = ERROR_DEVICE_TIME_OUT;
                synchronized (mLock) {//唤醒等待，关闭连接
                    mLock.notifyAll();
                }
                terminateConnection(bluetoothGatt);
            }
        }
    };
    abstract class ConnTimeOutRunnable implements Runnable {
        BluetoothGatt bluetoothGatt;
    }

    /**
     * Disconnects from the device and cleans local variables in case of error. This method is SYNCHRONOUS and wait until the disconnecting process will be completed.
     *
     * @param gatt  the GATT device to be disconnected
     * @param error error number
     */
    protected void terminateConnection(final BluetoothGatt gatt, final int error) {
        if (mConnectionState != STATE_DISCONNECTED) {
            // Disconnect from the device
            disconnect(gatt);
        }

        // Close the device
        refreshDeviceCache(gatt, false);
        close(gatt);
        waitFor(600);
//        if (error != 0)report(error);
    }

    public void terminateConnection(final BluetoothGatt gatt) {
        if (mConnectionState != STATE_DISCONNECTED) {
            // Disconnect from the device
            disconnect(gatt);
        }

        // Close the device
        refreshDeviceCache(gatt, false);
        close(gatt);
        waitFor(600);
//        if (error != 0)report(error);
    }
    /**
     * Disconnects from the device. This is SYNCHRONOUS method and waits until the callback returns new state. Terminates immediately if device is already disconnected. Do not call this method
     * directly, use {@link #terminateConnection(BluetoothGatt, int)} instead.
     *
     * @param gatt the GATT device that has to be disconnected
     */
    public void disconnect(final BluetoothGatt gatt) {
        if (mConnectionState == STATE_DISCONNECTED)
            return;

        logi("Disconnecting...");
        mConnectionState = STATE_DISCONNECTING;

        logi("Disconnecting from the device...");
        gatt.disconnect();

        // We have to wait until device gets disconnected or an error occur
        waitUntilDisconnected();
        logi( "Disconnected");
    }


    /**
     * Wait until the connection state will change to {@link #STATE_DISCONNECTED} or until an error occurs.
     */
    protected void waitUntilDisconnected() {
        try {
            synchronized (mLock) {
                while (mConnectionState != STATE_DISCONNECTED && mError == 0)
                    mLock.wait();
            }
        } catch (final InterruptedException e) {
            loge("Sleeping interrupted", e);
        }
    }

    /**
     * Wait for given number of milliseconds.
     * @param millis waiting period
     */
    protected void waitFor(final int millis) {
        synchronized (mLock) {
            try {
                logi("wait(" + millis + ")");
                mLock.wait(millis);
            } catch (final InterruptedException e) {
                loge("Sleeping interrupted", e);
            }
        }
    }

    /**
     * Closes the GATT device and cleans up.
     *
     * @param gatt the GATT device to be closed
     */
    protected void close(final BluetoothGatt gatt) {
        logi("Cleaning up...");
        logi("gatt.close()");
        gatt.close();
        mConnectionState = STATE_CLOSED;
    }

    /**
     * Clears the device cache. After uploading new firmware the DFU target will have other services than before.
     *
     * @param gatt  the GATT device to be refreshed
     * @param force <code>true</code> to force the refresh
     */
    protected void refreshDeviceCache(final BluetoothGatt gatt, final boolean force) {
		/*
		 * If the device is bonded this is up to the Service Changed characteristic to notify Android that the services has changed.
		 * There is no need for this trick in that case.
		 * If not bonded, the Android should not keep the services cached when the Service Changed characteristic is present in the target device database.
		 * However, due to the Android bug (still exists in Android 5.0.1), it is keeping them anyway and the only way to clear services is by using this hidden refresh method.
		 */
        if (force || gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
            logi( "gatt.refresh() (hidden)");
			/*
			 * There is a refresh() method in BluetoothGatt class but for now it's hidden. We will call it using reflections.
			 */
            try {
                final Method refresh = gatt.getClass().getMethod("refresh");
                if (refresh != null) {
                    final boolean success = (Boolean) refresh.invoke(gatt);
                    logi("Refreshing result: " + success);
                }
            } catch (Exception e) {
                loge("An exception occurred while refreshing device", e);
                logi( "Refreshing failed");
            }
        }
    }


    public boolean isRealConnected(){
        return mConnectionState == STATE_CONNECTED_AND_READY;
    }

    private void loge(final String message) {
        Log.e(TAG, message);
    }
    private void loge(final String message, final Throwable e) {
        Log.e(TAG, message, e);
    }
    private void logi(final String message) { Log.i(TAG, message);}


}
