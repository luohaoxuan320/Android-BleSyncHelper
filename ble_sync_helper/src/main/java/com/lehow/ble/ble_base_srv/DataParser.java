package com.lehow.ble.ble_base_srv;

/**
 * Created by lehow on 2017/4/27.
 */

public abstract class DataParser<T> {
     public DataParser() {
     }

     public abstract T parserData(byte[] data);
}
