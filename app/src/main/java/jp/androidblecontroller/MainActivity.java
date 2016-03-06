package jp.androidblecontroller;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import jp.blecontroller.PeripheralActivity;

public class MainActivity extends Activity {

    private final static int REQUEST_PERMISSIONS = 1;
    private boolean isPermissionAllowed;

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
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            isPermissionAllowed = false;
            this.requestBlePermission();
        }
        else{
            isPermissionAllowed = true;
        }

        Button btnOpenCentral = (Button)findViewById(R.id.btn_open_central);
        btnOpenCentral.setOnClickListener((View v) ->{
                if(isPermissionAllowed) {
                    startActivity(new Intent(MainActivity.this, CentralActivity.class));
                }
            });

        Button btnOpenPeripheral = (Button)findViewById(R.id.btn_open_peripheral);
        btnOpenPeripheral.setOnClickListener((View v) -> {
            if (isPermissionAllowed) {
                startActivity(new Intent(MainActivity.this, PeripheralActivity.class));
            }
        });
    }
    @TargetApi(Build.VERSION_CODES.M)
    private void requestBlePermission(){
        // 権限が許可されていない場合はリクエスト.
        if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isPermissionAllowed = true;
        }
        else{
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        // 権限リクエストの結果を取得する.
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionAllowed = true;
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
