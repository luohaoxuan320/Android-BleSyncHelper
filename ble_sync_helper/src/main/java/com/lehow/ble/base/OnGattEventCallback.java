package com.lehow.ble.base;

import java.util.UUID;

/**
 * Created by lehow on 2017/4/17.
 */

public interface OnGattEventCallback {

    int ACTION_OK = 0x00;
    int ERR_UNFIND_CHAR = -1;
    int ERR_INVALID_CONNECTION = -2;



     /**
      * read 和 notify都从这个接口返回数据
      * @param char_uuid
      * @param data
      */
      void onReceivedData(UUID char_uuid,byte[] data);


     /**
      * 操作的结果
      * 比如 read Characteristic的结果，开启notify的结果，write数据的结果，
      * errorCode==0 没有错误
      * @param theRequest
      * @param errCode
      */
      void onOpResult(BaseRequest theRequest, int errCode);

}
