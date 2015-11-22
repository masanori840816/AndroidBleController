package jp.blecontroller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import jp.androidblecontroller.R;

public class PeripheralActivity extends Activity {

    private final static int REQUEST_ENABLE_BT = 123456;

    private BluetoothAdapter mBleAdapter = null;
    private BluetoothManager mBleManager = null;
    private BluetoothLeAdvertiser mBtAdvertiser;

    private BluetoothGattServer mBtGattServer = null;

    private final static int MESSAGE_NEW_RECEIVEDNUM = 0;
    private final static int MESSAGE_NEW_UPDATEDNUM = 1;

    private TextView mTxtReceivedNum;
    private TextView mTxtUpdatedNum;

    private String mStrReceivedNum;
    private String mStrUpdateNum;

    private BluetoothDevice mConnectedDevice;

    // for sending random numbers.
    private boolean mIsConnected = false;
    private Random mRandom = new Random();
    private Timer mTimer;
    private SendDataTimer mSendDataTimer;

    public class SendDataTimer extends TimerTask {
        @Override
        public void run() {
            if(mIsConnected)
            {
                // create random number 0~999 every 1000 milliseconds.
                mStrUpdateNum = String.valueOf(mRandom.nextInt(1000));
                // update value and change TextView's text to created number on UI thread.
                mHndBleHandler.sendEmptyMessage(MESSAGE_NEW_UPDATEDNUM);
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral);

        mTxtReceivedNum = (TextView) findViewById(R.id.peripheral_received_num);
        mTxtUpdatedNum = (TextView) findViewById(R.id.peripheral_updated_num);

        // prepare using Bluetooth.
        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            // BluetoothがOffならインテントを表示する.
            if ((mBleAdapter == null)
                    || (! mBleAdapter.isEnabled())) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // Intentでボタンを押すとonActivityResultが実行されるので、第二引数の番号を元に処理を行う.
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else
            {
                this.prepareBle();
            }
        }
        else
        {
            Toast.makeText(this, R.string.advertising_not_supported, Toast.LENGTH_SHORT).show();
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void prepareBle()
    {
        // prepare timer for update values.
        mTimer = new Timer();
        mSendDataTimer = new SendDataTimer();
        // 第二引数:最初の処理までのミリ秒 第三引数:以降の処理実行の間隔(ミリ秒).
        mTimer.schedule(mSendDataTimer, 500, 1000);

        mBtAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
        //mBtAdvertiserの確認
        if(mBtAdvertiser != null){

            BluetoothGattService btGattService = new BluetoothGattService(UUID.fromString(getResources().getString(R.string.uuid_service)), BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic btGattCharacteristic = new BluetoothGattCharacteristic(UUID.fromString(getResources().getString(R.string.uuid_characteristic))
                    ,BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE
                    ,BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);

            BluetoothGattDescriptor dataDescriptor = new BluetoothGattDescriptor(
                    UUID.fromString(getResources().getString(R.string.uuid_characteristic_config))
                    ,BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ);
            btGattCharacteristic.addDescriptor(dataDescriptor);

            btGattService.addCharacteristic( btGattCharacteristic);
            mBtGattServer = mBleManager.openGattServer(this, mGattServerCallback);
            mBtGattServer.addService(btGattService);
            AdvertiseData.Builder dataBuilder=new AdvertiseData.Builder();
            AdvertiseSettings.Builder settingsBuilder=new AdvertiseSettings.Builder();
            dataBuilder.setIncludeTxPowerLevel(false);
            dataBuilder.addServiceUuid(ParcelUuid.fromString(getResources().getString(R.string.uuid_service)));
            settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
            settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
            BluetoothLeAdvertiser bluetoothLeAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
            bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(),dataBuilder.build()
                    , new AdvertiseCallback(){
                        @Override
                        public void onStartSuccess(AdvertiseSettings settingsInEffect) {

                        }
                        @Override
                        public void onStartFailure(int errorCode) {

                        }
                    });
        }else {
            Toast.makeText(this, R.string.advertising_not_supported, Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // execute when the user push button on the Intent.
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if ((mBleAdapter != null)
                        || (mBleAdapter.isEnabled())) {
                    // if BLE is enabled, start advertising.
                    this.prepareBle();
                }
                break;
        }
    }
    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("Peripheral", "onServiceAdded status=GATT_SUCCESS service="
                        + service.getUuid().toString());
            } else {
                Log.d("Peripheral", "onServiceAdded status!=GATT_SUCCESS");
            }
        }
        @Override
        public void onConnectionStateChange(android.bluetooth.BluetoothDevice device, int status,
                                            int newState) {
            if(newState == 2){
                // set connected device.
                mConnectedDevice = device;
                mIsConnected = true;
            }
            else{
                mIsConnected = false;
            }
        }
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d("Peripheral", "characteristic writeRequest " + characteristic.getStringValue(0));

            mStrReceivedNum = characteristic.getStringValue(offset);
            mHndBleHandler.sendEmptyMessage(MESSAGE_NEW_RECEIVEDNUM);

            if(responseNeeded){
                mBtGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);
            }
        }
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId
                , BluetoothGattDescriptor descriptor, boolean preparedWrite
                , boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            if(responseNeeded){
                mBtGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value);
            }
        }
    };
    private Handler mHndBleHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            // execute on UI thread.
            switch (msg.what)
            {
                case MESSAGE_NEW_RECEIVEDNUM:
                    mTxtReceivedNum.setText(mStrReceivedNum);
                    break;
                case MESSAGE_NEW_UPDATEDNUM:
                    mTxtUpdatedNum.setText(mStrUpdateNum);
                    // Update value.
                    notifyConnectedDevice();
                    break;
            }
        }
    };
    private void notifyConnectedDevice() {
        // update value for connected device.
        BluetoothGattCharacteristic readCharacteristic = mBtGattServer.getService(UUID.fromString(getResources().getString(R.string.uuid_service)))
                .getCharacteristic(UUID.fromString(getResources().getString(R.string.uuid_characteristic)));
        readCharacteristic.setValue(mStrUpdateNum);
        mBtGattServer.notifyCharacteristicChanged(mConnectedDevice, readCharacteristic, false);

    }
}
