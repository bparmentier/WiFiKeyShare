/*
 * The original code in this file was taken from Android 5.0.0:
 * packages/apps/Nfc/src/com/android/nfc/NfcWifiProtectedSetup.java
 *
 * Although the original file does not have any copyright notice, it should
 * fall under the Apache License, Version 2.0, as follows:
 *
 * Copyright (C) The Android Open Source Project
 * Copyright (C) 2016 Bruno Parmentier (modified to integrate with the
 *      WiFiKeyShare application)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.brunoparmentier.wifikeyshare.ui.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.utils.NfcUtils;

public class ConfirmConnectToWifiNetworkActivity extends Activity {

    private static final String TAG = ConfirmConnectToWifiNetworkActivity.class.getSimpleName();

    public static final int ENABLE_WIFI_TIMEOUT_MILLIS = 5000;

    private WifiConfiguration wifiConfiguration;
    private AlertDialog alertDialog;
    private boolean isEnableWifiInProgress;
    private Handler handler;
    private boolean isWifiBroadcastReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);

        wifiConfiguration = NfcUtils.readTag(tag);

        if (wifiConfiguration != null) {
            String printableSsid = getPrintableSsid(wifiConfiguration.SSID);
            alertDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.confirm_connection_title_connect_to_network)
                    .setMessage(String.format(getResources().getString(R.string.confirm_connection_prompt_connect_to_network),
                            printableSsid))
                    .setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            if (!isEnableWifiInProgress) {
                                finish();
                            }
                        }
                    })
                    .setNegativeButton(R.string.action_cancel, null)
                    .setPositiveButton(R.string.confirm_connection_action_connect, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                            if (!wifiManager.isWifiEnabled()) {
                                wifiManager.setWifiEnabled(true);
                                isEnableWifiInProgress = true;

                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (getAndClearEnableWifiInProgress()) {
                                            showFailToast();
                                            ConfirmConnectToWifiNetworkActivity.this.finish();
                                        }
                                    }
                                }, ENABLE_WIFI_TIMEOUT_MILLIS);

                            } else {
                                doConnect(wifiManager);
                            }

                            alertDialog.dismiss();
                        }
                    })
                    .create();

            isEnableWifiInProgress = false;
            handler = new Handler();

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
            registerReceiver(wifiStateBroadcastReceiver, intentFilter);
            isWifiBroadcastReceiverRegistered = true;

            alertDialog.show();
        } else {
            Log.e(TAG, "onCreate: Wi-Fi configuration is null");
            finish();
        }

        super.onCreate(savedInstanceState);
    }

    private void doConnect(WifiManager wifiManager) {
        int networkId = wifiManager.addNetwork(wifiConfiguration);

        if (networkId < 0) {
            showFailToast();
        } else {
            boolean connected = wifiManager.enableNetwork(networkId, true);
            if (connected) {
                Toast.makeText(ConfirmConnectToWifiNetworkActivity.this,
                        R.string.confirm_connection_status_wifi_connected, Toast.LENGTH_SHORT).show();
            } else {
                showFailToast();
            }
        }
    }


    private void showFailToast() {
        Toast.makeText(ConfirmConnectToWifiNetworkActivity.this,
                R.string.confirm_connection_status_unable_to_connect, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        if (isWifiBroadcastReceiverRegistered) {
            ConfirmConnectToWifiNetworkActivity.this.unregisterReceiver(wifiStateBroadcastReceiver);
        }
        super.onDestroy();
    }

    private final BroadcastReceiver wifiStateBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0);
                if (wifiConfiguration != null
                        && wifiState == WifiManager.WIFI_STATE_ENABLED) {
                    if (getAndClearEnableWifiInProgress()) {
                        doConnect(
                                (WifiManager) ConfirmConnectToWifiNetworkActivity.this
                                        .getSystemService(Context.WIFI_SERVICE));
                    }
                }
            }
        }
    };

    private boolean getAndClearEnableWifiInProgress() {
        boolean enableWifiInProgress;

        synchronized (this)  {
            enableWifiInProgress = this.isEnableWifiInProgress;
            this.isEnableWifiInProgress = false;
        }

        return enableWifiInProgress;
    }

    private static String getPrintableSsid(String ssid) {
        if (ssid == null) return "";

        final int length = ssid.length();
        if ((length > 2) && (ssid.charAt(0) == '"') && (ssid.charAt(length - 1) == '"')) {
            return ssid.substring(1, length - 1);
        } else {
            return ssid;
        }
    }
}