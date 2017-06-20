package com.lehow.ble.base.request;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import com.lehow.ble.base.BaseRequest;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/19.
 * TODO:这里还有一个问题,就是 同一个WriteOpAction 执行两次不同的value后，第二次的值会被发送两次，而第一次的值，会被覆盖，这个是在底层处理勒，还是留给上层处理。先留给上层处理吧，因为还要校验ACK回应
 */

public class WriteRequest extends BaseRequest {

    private byte[] value;
    private boolean noRsp = false;
    public WriteRequest(UUID serviceUuid, UUID charUuid) {
        super(serviceUuid, charUuid);
    }

    private WriteRequest(UUID serviceUuid, UUID charUuid,boolean noResponse) {
        super(serviceUuid, charUuid);
        this.noRsp = noResponse;
    }
    public static WriteRequest getNoRspInstance(UUID serviceUuid, UUID charUuid) {
        return new WriteRequest(serviceUuid, charUuid, true);
    }


    public void setValue(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    protected boolean execute(BluetoothGatt bluetoothGatt, BluetoothGattCharacteristic bluetoothGattCharacteristic) {
        if (value==null){
            return false;
        }
        bluetoothGattCharacteristic.setWriteType(noRsp?BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE:BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        boolean b = bluetoothGattCharacteristic.setValue(value);
        if (b) {
            return bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        }
        return b;
    }

}
