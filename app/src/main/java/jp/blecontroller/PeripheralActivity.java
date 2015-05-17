package jp.blecontroller;

import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import jp.androidblecontroller.R;


public class PeripheralActivity extends Activity {
    private final static int SDKVER_LOLLIPOP = 21;
    // 対象のサービスUUID.
    private static final String SERVICE_UUID = "2B1DA6DE-9C29-4D6C-A930-B990EA2F12BB";
    // キャラクタリスティックUUID.
    private static final String CHARACTERISTIC_UUID = "7F855F82-9378-4508-A3D2-CD989104AF22";
    // キャラクタリスティック設定UUID(固定値).
    private static final String CHARACTERISTIC_CONFIG_UUID = "00002902-0000-1000-8000-00805f9b34fb";
    private final static int REQUEST_ENABLE_BT = 123456;

    private BluetoothAdapter mBleAdapter = null;
    private BluetoothManager mBleManager = null;
    private BluetoothLeAdvertiser mBleAdvertiser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_peripheral);

        // Bluetoothの使用準備.
        mBleManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBleAdapter = mBleManager.getAdapter();

        if(Build.VERSION.SDK_INT >= SDKVER_LOLLIPOP)
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
    @TargetApi(SDKVER_LOLLIPOP)
    private void prepareBle()
    {
        mBleAdvertiser = mBleAdapter.getBluetoothLeAdvertiser();
        //mBLAdvertiserの確認
        if(mBleAdvertiser != null){
            Log.d("OUT", "存在する" + mBleAdvertiser);
        }else {
            Log.d("OUT", "存在しない" + mBleAdvertiser);
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
                    this.prepareBle();
                }
                break;
        }
    }
}
