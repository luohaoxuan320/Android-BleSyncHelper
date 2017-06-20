package com.lehow.ble.ble_base_srv;

import com.lehow.ble.base.DataTransferUtils;

/**
 * Created by lehow on 2017/4/27.
 */

public class SimpleIntegerDataParser extends DataParser<Integer> {
    @Override
    public Integer parserData(byte[] data) {
        return DataTransferUtils.bytesToInt(data, 0);
    }
}
