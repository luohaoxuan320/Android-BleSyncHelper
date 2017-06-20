package com.lehow.ble.base;

import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanFilter;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

/**
 * Created by lehow on 2017/4/21.
 * 开始扫描前，如果你有重连机制，并且重连中会扫描设备，那么最后先暂停重连机制，因为这个扫描是互斥的，只能有一个正在扫描的
 * 否则后台重连的机制会迅速取消了当前发起的扫描逻辑
 */

public class BleScannerHelper {
    private static final int UN_FIND = 0;
    private static final String TAG = "BleScannerHelper";

    private static BleScannerHelper bleScannerHelper = new BleScannerHelper();
    private Handler handler;
    private BluetoothLeScannerCompat scanner;
    private ScanSettings settings;

    private ScanCallback curScanCallback;
    private Runnable timeOutStopRunnable;

    private BleScannerHelper() {
        handler = new Handler(Looper.getMainLooper());
        scanner = BluetoothLeScannerCompat.getScanner();
        settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000)
                .setUseHardwareBatchingIfSupported(false).build();

        timeOutStopRunnable =new Runnable() {
            @Override
            public void run() {
                timeOutStopScan();
            }
        };
    }

    public static BleScannerHelper getScannerInstance() {
        return bleScannerHelper;
    }

    /**
     * 扫描设备
     * @param scanCallBack
     */
    public void scanDevice(UUID mUuid, final ScanCallback scanCallBack) {
        Log.i(TAG, "--scanDevice: ");
        stopScan();//先取消当前的扫描
        curScanCallback = scanCallBack;
        List<ScanFilter> filters = null;
        if (mUuid != null) {
            filters=new ArrayList<>();
            filters.add(new ScanFilter.Builder().setServiceUuid(new ParcelUuid(mUuid)).build());
        }
        scanner.startScan(filters, settings, scanCallBack);
        handler.postDelayed(timeOutStopRunnable, 10 * 1000);
    }

    private void timeOutStopScan(){
        Log.i(TAG, "-----stopScan---- ");
        handler.removeCallbacks(timeOutStopRunnable);
        if( curScanCallback != null){
            curScanCallback.onScanFailed(UN_FIND);
            scanner.stopScan(curScanCallback);
        }
        curScanCallback = null;
    }
    public void stopScan(){
        Log.i(TAG, "-----stopScan---- ");
        handler.removeCallbacks(timeOutStopRunnable);
        //去掉 curScanCallback.onScanFailed(UN_FIND); 是为了避免 正在后台重连扫描的时候，我直连，导致扫描停止，
        // 回调未找到，进而checkConnModel，将一个新的扫描动作加入到了队列中，而此时连接的动作正在线程中处理
        //就出现了连接上了，后台还在不停的扫描的情况
        if( curScanCallback != null){
            scanner.stopScan(curScanCallback);
        }
        curScanCallback = null;
    }
    private void stopScanWhenFindTheDevice(){
        Log.i(TAG, "-----stopScanWhenFindTheDevice---- ");
        handler.removeCallbacks(timeOutStopRunnable);
        if( curScanCallback != null){
            scanner.stopScan(curScanCallback);
        }
        curScanCallback = null;
    }
    /**
     * 扫描指定设备
     * @param macAddress
     * @param scanResult
     */
    public boolean scanTheDevice(final String macAddress, final OnTheScanResult scanResult) {
        Log.i(TAG, "--scanTheDevice: ");
        if (TextUtils.isEmpty(macAddress))return false;

        stopScan();
        curScanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                if (macAddress.equals(result.getDevice().getAddress())) {
                    scanResult.onResult(result.getDevice());
                    stopScanWhenFindTheDevice();
                }
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
                for (ScanResult result : results) {
                    if (macAddress.equals(result.getDevice().getAddress())) {
                        scanResult.onResult(result.getDevice());
                        stopScanWhenFindTheDevice();
                    }
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                if (errorCode == 0) {
                    scanResult.onResult(null);
                }else {
                    scanResult.onScanFailed(errorCode);
                }
            }
        };
        scanner.startScan(null, settings, curScanCallback);
        handler.postDelayed(timeOutStopRunnable, 10 * 1000);
        return true;
    }

}
