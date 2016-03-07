package jp.androidblecontroller;

import android.app.Activity;
import android.content.IntentSender;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

/**
 * Created by masanori on 2016/03/07.
 */
public class LocationAccesser implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{
    private GoogleApiClient apiClient;
    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }
    @Override
    public void onConnectionSuspended(int cause) {
    }
    @Override
    public void onConnected(Bundle bundle){
    }
    public void checkIsGpsOn(FragmentActivity activity, IBleActivity bleActivity){
        // OS Version 6.0以降はGPSがOffだとScanできないのでチェック.
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        locationRequest.setInterval(3000L);
        locationRequest.setFastestInterval(500L);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        builder.setAlwaysShow(true);

        if(apiClient == null) {
            apiClient = new GoogleApiClient
                .Builder(activity.getApplicationContext())
                .enableAutoManage(activity, this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        }

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(apiClient, builder.build());
        // ResultCallback<LocationSettingsResult>() - onResult(LocationSettingsResult settingsResult).
        result.setResultCallback(
            settingsResult -> {
                final Status status = settingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // GPSがOnならScan開始.
                        bleActivity.onGpsIsEnabled();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // GPSがOffならIntent表示. onActivityResultで結果取得.
                            status.startResolutionForResult(
                                    activity, R.string.request_enable_location);
                        } catch (IntentSender.SendIntentException ex) {
                            // Runnable() - run().
                            activity.runOnUiThread(
                                () -> {
                                    AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                                    alert.setTitle(activity.getString(R.string.error_title));
                                    alert.setMessage(ex.getLocalizedMessage());
                                    // DialogInterface.OnClickListener() - onClick(DialogInterface dialog, int which).
                                    alert.setPositiveButton(activity.getString(android.R.string.ok), null);
                                    alert.show();
                                });
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Locationが無効なら無視.
                        break;
                }
            });
    }
}
