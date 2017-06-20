package com.lehow.ble.base;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by lehow on 2017/4/17.
 */

 interface SimpleBleGattCallback  {

     void onDisconnected() ;

     void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) ;

     void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) ;

     void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) ;

     void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) ;

     void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) ;

     void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) ;


}
