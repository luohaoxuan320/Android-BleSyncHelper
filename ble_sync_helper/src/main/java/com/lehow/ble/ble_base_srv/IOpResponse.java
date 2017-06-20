package com.lehow.ble.ble_base_srv;

import com.lehow.ble.base.OnGattEventCallback;
import com.lehow.ble.base.BaseRequest;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.UUID;

/**
 * Created by lehow on 2017/4/17.
 */

public abstract class  IOpResponse<K extends DataParser<T>,T> implements OnGattEventCallback {

    @Override
    public void onReceivedData(UUID char_uuid, byte[] data) {
        try {
            K k= (K) getGenericClass().newInstance();
            onReceivedData(k.parserData(data));
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }



    private Class<T> getGenericClass() {
        Type genType = this.getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        return  (Class<T>)params[0];
    }

    @Override
    public void onOpResult(BaseRequest theRequest, int errCode) {
        onOpResult(errCode);
    }

    public abstract void onReceivedData(T t);

    public abstract void onOpResult(int errCode);
}
