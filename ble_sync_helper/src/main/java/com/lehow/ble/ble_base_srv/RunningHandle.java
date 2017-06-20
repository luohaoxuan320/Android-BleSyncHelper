package com.lehow.ble.ble_base_srv;

import com.lehow.ble.base.DataTransferUtils;
import com.lehow.ble.base.request.EnableNotifyRequest;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/18.
 */


public class RunningHandle extends DataParser<RunningHandle.RunningEntity> {
    public final static UUID SERVICE_RUNNING_SPEED_AND_CADENCE = UUID.fromString("00001814-0000-1000-8000-00805F9B34FB");
    public final static UUID CHAR_RSC_MEASUREMENT = UUID.fromString("00002A53-0000-1000-8000-00805F9B34FB");
    private static final String TAG = "RunningHandle";


    public RunningHandle() {
    }

    public static EnableNotifyRequest getEnableNotifyRequest(boolean isOpen){
        return  new EnableNotifyRequest(SERVICE_RUNNING_SPEED_AND_CADENCE, CHAR_RSC_MEASUREMENT,isOpen);
    }

    @Override
    public RunningEntity parserData(byte[] data) {
        byte flag = data[0];
        boolean isStrideLengthExist = (flag & 0x01) > 0;
        boolean isTotalDistanceExist = (flag & 0x02) > 0;
        boolean isRunning = (flag & 0x04) > 0;

        float speed = DataTransferUtils.bytesToShort(data, 1) / 256.0f;

        int cadence = data[3]&0xff;

        //2byte的长度
        float strideLength = isStrideLengthExist ? DataTransferUtils.bytesToShort(data, 4) /100.0f: -1;
        //如果strideLength 存在 totalDistance要加上2个字节的偏移
        //4byte的长度
        float totalDistance = isTotalDistanceExist ? DataTransferUtils.bytesToInt(data, 4 + (isStrideLengthExist ? 2 : 0))/100.0f: -1;

        return new RunningEntity(speed, cadence, strideLength, totalDistance,isRunning);
    }


    public class RunningEntity {
        float speed;
        int cadence;
        float strideLength;
        float totalDistance;
        boolean isRunning;

        public RunningEntity(float speed, int cadence, float strideLength, float totalDistance, boolean isRunning) {
            this.speed = speed;
            this.cadence = cadence;
            this.strideLength = strideLength;
            this.totalDistance = totalDistance;
            this.isRunning = isRunning;
        }

        public float getSpeed() {
            return speed;
        }

        public int getCadence() {
            return cadence;
        }

        public float getStrideLength() {
            return strideLength;
        }

        public float getTotalDistance() {
            return totalDistance;
        }

        public boolean isRunning() {
            return isRunning;
        }
    }

}

