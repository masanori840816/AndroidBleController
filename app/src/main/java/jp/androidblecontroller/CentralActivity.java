package jp.androidblecontroller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.le.*;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.BluetoothAdapter;
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
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import java.util.UUID;

public class CentralActivity extends FragmentActivity implements IBleActivity{
    public class SendDataTimer extends TimerTask {
        @Override
        public void run() {
            if (isBleEnabled) {
                String sendValue = String.valueOf(random.nextInt(1000));
                // UIスレッドで生成した数をTextViewにセット.
                runOnUiThread(
                        () -> {
                            sendValueView.setText(sendValue);
                        });
                // Characteristicにランダムな値をセットして、Writeリクエストを送信.
                bleCharacteristic.setValue(sendValue);
                bleGatt.writeCharacteristic(bleCharacteristic);
            }
        }
    }
    private final static LocationAccesser locationAccesser = new LocationAccesser();

    private BluetoothManager bleManager;
    private BluetoothAdapter bleAdapter;
    private boolean isBleEnabled = false;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bleGatt;
    private BluetoothGattCharacteristic bleCharacteristic;

    private TextView receivedValueView;
    private TextView sendValueView;

    // 乱数送信用.
    private Random random = new Random();
    private Timer timer;
    private SendDataTimer sendDataTimer;

    public void onGpsIsEnabled(){
        // 2016.03.07現在GPSを要求するのが6.0以降のみなのでOnになったら新しいAPIでScan開始.
        this.startScanByBleScanner();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_central);

        isBleEnabled = false;

        // Writeリクエストで送信する値、Notificationで受け取った値をセットするTextView.
        receivedValueView = (TextView) findViewById(R.id.received_value_view);
        sendValueView = (TextView) findViewById(R.id.send_value_view);

        // Bluetoothの使用準備.
        bleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bleAdapter = bleManager.getAdapter();

        // Writeリクエスト用のタイマーをセット.
        timer = new Timer();
        sendDataTimer = new SendDataTimer();
        // 第二引数:最初の処理までのミリ秒 第三引数:以降の処理実行の間隔(ミリ秒).
        timer.schedule(sendDataTimer, 500, 1000);

        Button sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener((View v) ->{
            if(isBleEnabled){
                bleCharacteristic.setValue(((EditText) findViewById(R.id.input_area)).getText().toString());
                bleGatt.writeCharacteristic(bleCharacteristic);
            }
        });
        // BluetoothがOffならインテントを表示する.
        if ((bleAdapter == null)
                || (! bleAdapter.isEnabled())) {
            // Intentでボタンを押すとonActivityResultが実行されるので、第二引数の番号を元に処理を行う.
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), R.string.request_ble_on);
        }
        else{
            // BLEが使用可能ならスキャン開始.
            this.scanNewDevice();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Intentでユーザーがボタンを押したら実行.
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case R.string.request_ble_on:
                if ((bleAdapter != null)
                        || (bleAdapter.isEnabled())) {
                    // BLEが使用可能ならスキャン開始.
                    this.scanNewDevice();
                }
                break;
            case R.string.request_enable_location:
                if(resultCode == RESULT_OK){
                    onGpsIsEnabled();
                }
                break;
        }
    }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){
            // 接続状況が変化したら実行.
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 接続に成功したらサービスを検索する.
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 接続が切れたらGATTを空にする.
                if (bleGatt != null){
                    bleGatt.close();
                    bleGatt = null;
                }
                isBleEnabled = false;
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status){
            // Serviceが見つかったら実行.
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // UUIDが同じかどうかを確認する.
                BluetoothGattService bleService = gatt.getService(UUID.fromString(getString(R.string.uuid_service)));
                if (bleService != null){
                    // 指定したUUIDを持つCharacteristicを確認する.
                    bleCharacteristic = bleService.getCharacteristic(UUID.fromString(getString(R.string.uuid_characteristic)));
                    if (bleCharacteristic != null) {
                        // Service, CharacteristicのUUIDが同じならBluetoothGattを更新する.
                        bleGatt = gatt;
                        // キャラクタリスティックが見つかったら、Notificationをリクエスト.
                        bleGatt.setCharacteristicNotification(bleCharacteristic, true);

                        // Characteristic の Notificationを有効化する.
                        BluetoothGattDescriptor bleDescriptor = bleCharacteristic.getDescriptor(
                                UUID.fromString(getString(R.string.uuid_characteristic_config)));
                        bleDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bleGatt.writeDescriptor(bleDescriptor);
                        // 接続が完了したらデータ送信を開始する.
                        isBleEnabled = true;
                    }
                }
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            // キャラクタリスティックのUUIDをチェック(getUuidの結果が全て小文字で帰ってくるのでUpperCaseに変換)
            if (getString(R.string.uuid_characteristic).equals(characteristic.getUuid().toString().toUpperCase())){
                runOnUiThread(
                        () -> {
                            // Peripheral側で更新された値をセットする.
                            receivedValueView.setText(characteristic.getStringValue(0));
                        });
            }
        }
    };
    private void scanNewDevice(){
        // OS ver.6.0以上ならGPSがOnになっているかを確認する(GPSがOffだとScanに失敗するため).
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            locationAccesser.checkIsGpsOn(this, this);
        }
        // OS ver.5.0以上ならBluetoothLeScannerを使用する.
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            this.startScanByBleScanner();
        }
        else{
            // デバイスの検出.
            // BluetoothAdapter.LeScanCallback() - onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord).
            bleAdapter.startLeScan(
                (final BluetoothDevice device, int rssi, byte[] scanRecord) -> {
                runOnUiThread(
                        () -> {
                        // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                        bleGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
                    });
                });
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void startScanByBleScanner(){
        bleScanner = bleAdapter.getBluetoothLeScanner();

        // デバイスの検出.
        bleScanner.startScan(new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                // スキャン中に見つかったデバイスに接続を試みる.第三引数には接続後に呼ばれるBluetoothGattCallbackを指定する.
                result.getDevice().connectGatt(getApplicationContext(), false, mGattCallback);
            }

            @Override
            public void onScanFailed(int intErrorCode) {
                super.onScanFailed(intErrorCode);
            }
        });
    }

}