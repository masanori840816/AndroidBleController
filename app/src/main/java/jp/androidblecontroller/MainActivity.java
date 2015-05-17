package jp.androidblecontroller;


import android.app.Activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    private Button mBtnOpenCentral;
    private Button mBtnOpenPeripheral;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // デバイスがBLEに対応していなければトースト表示.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        mBtnOpenCentral = (Button)findViewById(R.id.btn_open_central);
        mBtnOpenCentral.setOnClickListener(mBtnOpenCentralClicked);

        mBtnOpenPeripheral = (Button)findViewById(R.id.btn_open_peripheral);
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
            // TODO: PeripheralのActivityを開く
        }
    };
}
