/*
 * WiFiKeyShare. Share Wi-Fi passwords with QR codes or NFC tags.
 * Copyright (C) 2016 Bruno Parmentier <dev@brunoparmentier.be>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package be.brunoparmentier.wifikeyshare.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.wifikeyshare.DividerItemDecoration;
import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.adapters.WifiNetworkAdapter;
import be.brunoparmentier.wifikeyshare.db.WifiKeysDataSource;
import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;
import be.brunoparmentier.wifikeyshare.utils.WpaSupplicantParser;
import eu.chainfire.libsuperuser.Shell;

public class WifiListActivity extends AppCompatActivity {
    private static final String TAG = WifiListActivity.class.getSimpleName();

    private static final String FILE_WIFI_SUPPLICANT = "/data/misc/wifi/wpa_supplicant.conf";

    private static final int PASSWORD_REQUEST = 1;
    private static final String KEY_NETWORK_ID = "network_id";

    private List<WifiNetwork> wifiNetworks;
    private WifiNetworkAdapter wifiNetworkAdapter;
    private RecyclerView rvWifiNetworks;
    private WifiManager wifiManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        /* Enable Wi-Fi if disabled */
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        setupWifiNetworksList();
    }

    private void setupWifiNetworksList() {
        wifiNetworks = new ArrayList<>();

        rvWifiNetworks = (RecyclerView) findViewById(R.id.rvWifiNetwork);

        wifiNetworkAdapter = new WifiNetworkAdapter(this, wifiNetworks);
        rvWifiNetworks.setAdapter(wifiNetworkAdapter);
        // Set layout manager to position the items
        rvWifiNetworks.setLayoutManager(new LinearLayoutManager(this));

        // Separator
        RecyclerView.ItemDecoration itemDecoration = new DividerItemDecoration(
                this, DividerItemDecoration.VERTICAL_LIST);
        rvWifiNetworks.addItemDecoration(itemDecoration);
        rvWifiNetworks.setHasFixedSize(true);
        rvWifiNetworks.setItemAnimator(new DefaultItemAnimator());

        //rvWifiNetworks.setItemAnimator(new SlideInUpAnimator());
        //rvWifiNetworks.getItemAnimator().setAddDuration(1000);

        /*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addWifiNetwork();
            }
        });
        */

        (new WifiListTask()).execute();
    }

    private void addWifiNetwork() {
//
//        //FrameLayout fl = (FrameLayout) findViewById(android.R.id.custom);
//        LayoutInflater inflater = (LayoutInflater) getSystemService
//                (Context.LAYOUT_INFLATER_SERVICE);
//        View v = inflater.inflate(R.layout.dialog_add_wifi, null);
//        //fl.addView(v);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setTitle("Add Wi-Fi network");
//        builder.setView(v);
//        builder.show();
//        //new AddWifiDialog(this).show();
//
//
//        //AddWifiDialog fragment = new AddWifiDialog();
//        //fragment.show(getSupportFragmentManager(), "Add Wi-Fi");

        // TODO: Show dialog box to configure new Wi-Fi AP

        wifiNetworks.add(new WifiNetwork("Test1", WifiAuthType.WEP, "mykey", false));
        wifiNetworkAdapter.notifyItemInserted(wifiNetworks.size() - 1);
        rvWifiNetworks.scrollToPosition(wifiNetworkAdapter.getItemCount() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == PASSWORD_REQUEST) {
            if (resultCode == RESULT_OK) {
                int networkId = data.getIntExtra(KEY_NETWORK_ID, -1);
                if (networkId != -1) {
                    WifiKeysDataSource wifiKeysDataSource = new WifiKeysDataSource(this);
                    String key = wifiKeysDataSource.getKey(
                            wifiNetworks.get(networkId).getSsid(),
                            wifiNetworks.get(networkId).getAuthType());
                    wifiNetworks.get(networkId).setKey(key);

                    wifiNetworkAdapter.notifyItemChanged(networkId);
                } else {
                    Log.e(TAG, "onActivityResult: invalid networkId");
                }

            }
        }
    }

    private class WifiListTask extends AsyncTask<Void, Void, List<WifiNetwork>> {

        private boolean isDeviceRooted;

        @Override
        protected List<WifiNetwork> doInBackground(Void... params) {
            List<WifiNetwork> parsedWifiNetworks;

            if (Shell.SU.available()) {
                isDeviceRooted = true;

                List<String> result = Shell.SU.run("cat " + FILE_WIFI_SUPPLICANT);
                String strRes = "";
                for (String line : result) {
                    strRes += line + "\n";
                    //Log.d(TAG, line);
                }
                parsedWifiNetworks = WpaSupplicantParser.parse(strRes);

                /*for (WifiNetwork network : parsedWifiNetworks) {
                    Log.d(TAG, network.toString());
                }*/
                return parsedWifiNetworks;
            } else {
                isDeviceRooted = false;


                List<WifiConfiguration> savedWifiConfigs = wifiManager.getConfiguredNetworks();

                parsedWifiNetworks = new ArrayList<>();
                if (savedWifiConfigs != null) {
                    for (WifiConfiguration wifiConfig : savedWifiConfigs) {
                        parsedWifiNetworks.add(WifiNetwork.fromWifiConfiguration(wifiConfig));
                    }
                }

                return parsedWifiNetworks;
            }
        }

        @Override
        protected void onPostExecute(List<WifiNetwork> parsedWifiNetworks) {
            for (WifiNetwork wifiNetwork : parsedWifiNetworks) {
                wifiNetworks.add(wifiNetwork);
                wifiNetworkAdapter.notifyItemInserted(wifiNetworkAdapter.getItemCount() - 1);
            }
            if (!isDeviceRooted) {
                new AlertDialog.Builder(WifiListActivity.this)
                        .setTitle("No root detected")
                        .setMessage("We couldn't get the Wi-Fi passwords as your device is not " +
                                "rooted. You will have to add them manually for each network you " +
                                "want to share.")
                        .setPositiveButton("Got it!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .setCancelable(false)
                        .create()
                        .show();
                setSavedKeysToWifiNetworks();
            }
        }
    }


    private void setSavedKeysToWifiNetworks() {
        WifiKeysDataSource wifiKeysDataSource = new WifiKeysDataSource(this);
        List<WifiNetwork> wifiNetworksWithKey = wifiKeysDataSource.getSavedWifiWithKeys();

        for (int i = 0; i < wifiNetworks.size(); i++) {
            for (int j = 0; j < wifiNetworksWithKey.size(); j++) {
                if (wifiNetworks.get(i).getSsid().equals(wifiNetworksWithKey.get(j).getSsid())
                        && wifiNetworks.get(i).getAuthType() == wifiNetworksWithKey.get(j).getAuthType())
                    if (wifiNetworks.get(i).needsPassword()) {
                        wifiNetworks.get(i).setKey(wifiNetworksWithKey.get(j).getKey());
                        wifiNetworkAdapter.notifyItemChanged(i);
                    }
            }
        }
    }
}
