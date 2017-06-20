package com.lehow.ble.ble_base_srv;

import com.lehow.ble.base.DataTransferUtils;
import com.lehow.ble.base.request.EnableNotifyRequest;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/13.
 */

public class HRMHandle extends DataParser<HRMHandle.HREntity> {
    // 心率
    public final static UUID SERVICE_HR = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB");
    public final static UUID CHAR_HR_MEASUREMENT =UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB");
    public final static UUID CHAR_BODY_SENSOR_LOCATION =UUID.fromString("00002A38-0000-1000-8000-00805F9B34FB");
    private static final String TAG ="HRMHandle" ;

    public static EnableNotifyRequest getEnableNotifyRequest(boolean isOpen){
        return new EnableNotifyRequest(SERVICE_HR, CHAR_HR_MEASUREMENT,isOpen);
    }

    public HRMHandle() {
    }

    @Override
    public HREntity parserData(byte[] data) {
        byte flag = data[0];//flag占一个字节
        boolean isValue8byte = (flag & 0x01) == 0;//最低位代表长度
        int sensorContactStatus = (flag & 0x06)>>1;//低1 2位，代表该值，截取出来,要把上面的那一个byte移除掉
        int hrValue = isValue8byte ? (data[1]&0xff):DataTransferUtils.bytesToShort(data,1);
        return new HREntity(hrValue, sensorContactStatus);
    }
    public class HREntity{
        int hrValue;
        int sensorStatus;

        public HREntity(int hrValue, int sensorStatus) {
            this.hrValue = hrValue;
            this.sensorStatus = sensorStatus;
        }

        public int getHrValue() {
            return hrValue;
        }

        public int getSensorStatus() {
            return sensorStatus;
        }
    }

}
