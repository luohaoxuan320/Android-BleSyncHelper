package com.lehow.ble.ble_base_srv;

import com.lehow.ble.base.request.ReadRequest;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

/**
 * Created by lehow on 2017/4/17.
 */

public class DeviceInfoHandle extends DataParser<String> {

    public final static UUID SERVICE_DEVICE_INFO = UUID.fromString("0000180A-0000-1000-8000-00805F9B34FB");
    /**
     * 固件版本号
     */
    public final static UUID CHAR_FIRMWARE_REVISION = UUID.fromString("00002A26-0000-1000-8000-00805F9B34FB");
    /**
     * 硬件版本号
     */
    public final static UUID CHAR_HW_REVISION = UUID.fromString("00002A27-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "DeviceInfoHandle";
    /**
     * 产品型号
     */
    public final static UUID CHAR_MODEL_NUMBER = UUID.fromString("00002A24-0000-1000-8000-00805F9B34FB");
//    public final static UUID CHAR_SERIAL_NUMBER = UUID.fromString("00002A25-0000-1000-8000-00805F9B34FB");
//    public final static UUID CHAR_SOFTWARE_REVISION = UUID.fromString("00002A28-0000-1000-8000-00805F9B34FB");
//    public final static UUID CHAR_MANUFACTURER_NAME = UUID.fromString("00002A29-0000-1000-8000-00805F9B34FB");


    public DeviceInfoHandle() {
    }

    public static ReadRequest getReadFwRequest() {
        return new ReadRequest(SERVICE_DEVICE_INFO, CHAR_FIRMWARE_REVISION);
    }
    public static ReadRequest getReadHwRequest() {
        return new ReadRequest(SERVICE_DEVICE_INFO, CHAR_HW_REVISION);
    }

    public static ReadRequest getReadModelRequest() {
        return new ReadRequest(SERVICE_DEVICE_INFO, CHAR_MODEL_NUMBER);
    }


    @Override
    public String parserData(byte[] data) {
        try {
            String version = new String(data,"UTF-8");
            return version;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }
}
