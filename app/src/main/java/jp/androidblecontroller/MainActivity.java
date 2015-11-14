package jp.androidblecontroller;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import jp.blecontroller.PeripheralActivity;

public class MainActivity extends Activity {

    private Button mBtnOpenCentral;
    private Button mBtnOpenPeripheral;

    private final static int REQUEST_PERMISSIONS = 1;
    private final static int SDKVER_MARSHMALLOW = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // デバイスがBLEに対応していなければトースト表示.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        // Android6.0以降なら権限確認.
        if(Build.VERSION.SDK_INT >= SDKVER_MARSHMALLOW)
        {
            this.requestBlePermission();
        }

        mBtnOpenCentral = (Button)findViewById(R.id.btn_open_central);
        mBtnOpenCentral.setOnClickListener(mBtnOpenCentralClicked);

        mBtnOpenPeripheral = (Button)findViewById(R.id.btn_open_peripheral);
        mBtnOpenPeripheral.setOnClickListener(mBtnOpenPeripheralClicked);
    }
    @TargetApi(SDKVER_MARSHMALLOW)
    private void requestBlePermission(){
        // 権限が許可されていない場合はリクエスト.
        if(checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            requestPermissions( new String[]{
                    Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION
            },REQUEST_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 権限リクエストの結果を取得する.
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Succeed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
            private final View.OnClickListener mBtnOpenCentralClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent ntnCentral = new Intent(MainActivity.this, CentralActivity.class);
            startActivity(ntnCentral);
        }
    };
    private final View.OnClickListener mBtnOpenPeripheralClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent ntnPeripheral = new Intent(MainActivity.this, PeripheralActivity.class);
            startActivity(ntnPeripheral);
        }
    };
}
