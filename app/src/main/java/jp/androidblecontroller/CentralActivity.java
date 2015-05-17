package jp.androidblecontroller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.*;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.TextView;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import java.util.UUID;

public class CentralActivity extends Activity {

    private final static int SDKVER_LOLLIPOP = 21;

    private final static int MESSAGE_NEW_RECEIVEDNUM = 0;
    private final static int MESSAGE_NEW_SENDNUM = 1;

    private final static int REQUEST_ENABLE_BT = 123456;
    private BluetoothManager mBleManager;
    private BluetoothAdapter mBleAdapter;
    private boolean mIsBluetoothEnable = false;
    private BluetoothLeScanner mBleScanner;
    private BluetoothGatt mBleGatt;
    private BluetoothGattCharacteristic mBleCharacteristic;

    private TextView mTxtReceivedNum;
    private TextView mTxtSendNum;

    private String mStrReceivedNum = "";
    private String mStrSendNum = "";

    // 対象のサービスUUID.
    private static final String SERVICE_UUID = "2B1DA6DE-9C29-4D6C-A930-B990EA2F12BB";
    // キャラクタリスティックUUID.
    private static final String CHARACTERISTIC_UUID = "7F855F82-9378-4508-A3D2-CD989104AF22";
    // キャラクタリスティック設定UUID(固定値).
    private static final String CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";

    // 乱数送信用.
    private Random mRandom = new Random();
    private Timer mTimer;
    private SendDataTimer mSendDataTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        mIsBluetoothEnable = false;

        // Writeリクエストで送信する値、Notificationで受け取った値をセットするTextView.
        mTxtReceivedNum = (TextView) findViewById(R.id.received_num);
        mTxtSendNum = (TextView) findViewById(R.id.send_num);

        // Bluetoothの使用準備.
        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        // Writeリクエスト用のタイマーをセット.
        mTimer = new Timer();
        mSendDataTimer = new SendDataTimer();
        // 第二引数:最初の処理までのミリ秒 第三引数:以降の処理実行の間隔(ミリ秒).
        mTimer.schedule(mSendDataTimer, 500, 1000);

        // BluetoothがOffならインテントを表示する.
        if ((mBleAdapter == null)
                || (! mBleAdapter.isEnabled())) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            // Intentでボタンを押すとonActivityResultが実行されるので、第二引数の番号を元に処理を行う.
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else
        {
            // BLEが使用可能ならスキャン開始.
            this.scanNewDevice();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Intentでユーザーがボタンを押したら実行.
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if ((mBleAdapter != null)
                        || (mBleAdapter.isEnabled())) {
                    // BLEが使用可能ならスキャン開始.
                    this.scanNewDevice();
                }
                break;
        }
    }
    private final LeScanCallback mScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                    mBleGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
                }
            });
        }
    };
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            // 接続状況が変化したら実行.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 接続に成功したらサービスを検索する.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 接続が切れたらGATTを空にする.
                if (mBleGatt != null)
                {
                    mBleGatt.close();
                    mBleGatt = null;
                }
                mIsBluetoothEnable = false;
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            // Serviceが見つかったら実行.
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // UUIDが同じかどうかを確認する.
                BluetoothGattService service = gatt.getService(UUID.fromString(SERVICE_UUID));
                if (service != null)
                {
                    // 指定したUUIDを持つCharacteristicを確認する.
                    mBleCharacteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));

                    if (mBleCharacteristic != null) {

                        // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                        mBleGatt = gatt;

                        // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                        boolean registered = mBleGatt.setCharacteristicNotification(mBleCharacteristic, true);

                        // Characteristic の Notificationを有効化する.
                        BluetoothGattDescriptor descriptor = mBleCharacteristic.getDescriptor(
                                UUID.fromString(CHARACTERISTIC_CONFIG_UUID));


                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        mBleGatt.writeDescriptor(descriptor);
                        // 接続が完了したらデータ送信を開始する.
                        mIsBluetoothEnable = true;
                    }
                }
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            // キャラクタリスティックのUUIDをチェック(getUuidの結果が全て小文字で帰ってくるのでUpperCaseに変換)
            if (CHARACTERISTIC_UUID.equals(characteristic.getUuid().toString().toUpperCase()))
            {
                // Peripheralで値が更新されたらNotificationを受ける.
                mStrReceivedNum = characteristic.getStringValue(0);
                // メインスレッドでTextViewに値をセットする.
                mBleHandler.sendEmptyMessage(MESSAGE_NEW_RECEIVEDNUM);
            }
        }
    };
    private Handler mBleHandler = new Handler()
    {
        public void handleMessage(Message msg)
        {
            // UIスレッドで実行する処理.
            switch (msg.what)
            {
                case MESSAGE_NEW_RECEIVEDNUM:
                    mTxtReceivedNum.setText(mStrReceivedNum);
                    break;
                case MESSAGE_NEW_SENDNUM:
                    mTxtSendNum.setText(mStrSendNum);
                    break;
            }
        }
    };



    private void scanNewDevice()
    {
        // OS ver.5.0以上ならBluetoothLeScannerを使用する.
        if (Build.VERSION.SDK_INT >= SDKVER_LOLLIPOP)
        {
            this.startScanByBleScanner();
        }
        else
        {
            // デバイスの検出.
            mBleAdapter.startLeScan(mScanCallback);

        }
    }

    @TargetApi(SDKVER_LOLLIPOP)
    private void startScanByBleScanner()
    {
        mBleScanner = mBleAdapter.getBluetoothLeScanner();

        // デバイスの検出.
        mBleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                result.getDevice().connectGatt(getApplicationContext(), false, mGattCallback);
            }
            @Override
            public void onScanFailed(int intErrorCode)
            {
                super.onScanFailed(intErrorCode);
            }
        });
    }


    public class SendDataTimer extends TimerTask{
        @Override
        public void run() {
            if(mIsBluetoothEnable)
            {
                // 設定時間ごとに0~999までの乱数を作成.
                mStrSendNum = String.valueOf(mRandom.nextInt(1000));
                // UIスレッドで生成した数をTextViewにセット.
                mBleHandler.sendEmptyMessage(MESSAGE_NEW_SENDNUM);
                // キャラクタリスティックに値をセットして、Writeリクエストを送信.
                mBleCharacteristic.setValue(mStrSendNum);
                mBleGatt.writeCharacteristic(mBleCharacteristic);
            }
        }
    }
    @Override
    protected void onDestroy()
    {
        mIsBluetoothEnable = false;

        if(mBleGatt != null) {
            mBleGatt.close();
            mBleGatt = null;
        }
        super.onDestroy();
    }
}