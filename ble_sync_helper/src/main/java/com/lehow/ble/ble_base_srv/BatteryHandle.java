package com.lehow.ble.ble_base_srv;

import com.lehow.ble.base.request.ReadRequest;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/14.
 */

public class BatteryHandle extends DataParser<Integer> {

    // 心率
    public final static UUID SERVICE_BATTERY = UUID.fromString("0000180F-0000-1000-8000-00805F9B34FB");
    public final static UUID CHAR_BATTERY_LEVEL =UUID.fromString("00002A19-0000-1000-8000-00805F9B34FB");


    public BatteryHandle() {
    }

    public static ReadRequest getReadRequest(){
       return new ReadRequest(SERVICE_BATTERY, CHAR_BATTERY_LEVEL);
    }

    @Override
    public Integer parserData(byte[] data) {
        return data[0]&0xff;
    }


}
