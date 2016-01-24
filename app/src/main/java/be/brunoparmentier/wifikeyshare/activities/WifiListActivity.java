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
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.wifikeyshare.DividerItemDecoration;
import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.adapters.WifiNetworkAdapter;
import be.brunoparmentier.wifikeyshare.db.WifiKeysDataSource;
import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;
import be.brunoparmentier.wifikeyshare.utils.AboutDialog;
import be.brunoparmentier.wifikeyshare.utils.ContextMenuRecyclerView;
import be.brunoparmentier.wifikeyshare.utils.WpaSupplicantParser;
import eu.chainfire.libsuperuser.Shell;

public class WifiListActivity extends AppCompatActivity {
    private static final String TAG = WifiListActivity.class.getSimpleName();

    private static final String FILE_WIFI_SUPPLICANT = "/data/misc/wifi/wpa_supplicant.conf";

    private static final int PASSWORD_REQUEST = 1;
    private static final String KEY_NETWORK_ID = "network_id";
    private static final String PREF_KEY_HAS_READ_NO_ROOT_DIALOG = "has_read_no_root_dialog";

    private WifiKeysDataSource wifiKeysDataSource;
    private List<WifiNetwork> wifiNetworks;
    private WifiNetworkAdapter wifiNetworkAdapter;
    private ContextMenuRecyclerView rvWifiNetworks;
    private WifiManager wifiManager;
    private boolean isDeviceRooted = false;
    private int networkIdToUpdate = -1; // index of item to update in networks list

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

        wifiKeysDataSource = new WifiKeysDataSource(this);
        wifiKeysDataSource.open();

        setupWifiNetworksList();
    }

    private void setupWifiNetworksList() {
        wifiNetworks = new ArrayList<>();

        rvWifiNetworks = (ContextMenuRecyclerView) findViewById(R.id.rvWifiNetwork);

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
        registerForContextMenu(rvWifiNetworks);

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
        // TODO: Show dialog box to configure new Wi-Fi AP

        wifiNetworks.add(new WifiNetwork("Test1", WifiAuthType.WEP, "mykey", false));
        wifiNetworkAdapter.notifyItemInserted(wifiNetworks.size() - 1);
        rvWifiNetworks.scrollToPosition(wifiNetworkAdapter.getItemCount() - 1);
    }

    @Override
    protected void onResume() {
        wifiKeysDataSource.open();

        if (networkIdToUpdate > -1) {
            String key = wifiKeysDataSource.getWifiKey(
                    wifiNetworks.get(networkIdToUpdate).getSsid(),
                    wifiNetworks.get(networkIdToUpdate).getAuthType());
            if (key == null) {
                Log.d(TAG, "onResume: key is null");
            } else {
                wifiNetworks.get(networkIdToUpdate).setKey(key);
                wifiNetworkAdapter.notifyItemChanged(networkIdToUpdate);
            }
            networkIdToUpdate = -1;
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        wifiKeysDataSource.close();
        super.onPause();
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
        if (id == R.id.action_about) {
            final AlertDialog aboutDialog = new AboutDialog(this);
            aboutDialog.show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        int itemPosition = ((ContextMenuRecyclerView.RecyclerContextMenuInfo) menuInfo).position;

        menu.setHeaderTitle(wifiNetworks.get(itemPosition).getSsid());
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.context_menu, menu);

        boolean canViewPasword = wifiNetworks.get(itemPosition).isPasswordProtected()
                && !wifiNetworks.get(itemPosition).getKey().isEmpty();
        boolean canClearPassword = canViewPasword;

        MenuItem viewPasswordMenuItem = menu.findItem(R.id.context_menu_wifi_list_view_password);
        viewPasswordMenuItem.setEnabled(canViewPasword);

        MenuItem clearPasswordMenuItem = menu.findItem(R.id.context_menu_wifi_list_clear_password);
        clearPasswordMenuItem.setEnabled(canClearPassword);
        clearPasswordMenuItem.setVisible(!isDeviceRooted);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int itemPosition = ((ContextMenuRecyclerView.RecyclerContextMenuInfo) item.getMenuInfo()).position;
        switch (item.getItemId()) {
            case (R.id.context_menu_wifi_list_view_password):
                final AlertDialog viewPasswordDialog = new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.wifilist_dialog_view_password))
                        .setView(R.layout.dialog_show_password)
                        .setPositiveButton(R.string.action_close, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                viewPasswordDialog.show();

                /* Set SSID, security and password values */
                TextView ssidTextView = (TextView) viewPasswordDialog.findViewById(R.id.ssid_value);
                TextView authTypeTextView = (TextView) viewPasswordDialog.findViewById(R.id.auth_type_value);
                TextView passwordTextView = (TextView) viewPasswordDialog.findViewById(R.id.password_value);
                ssidTextView.setText(wifiNetworks.get(itemPosition).getSsid());
                authTypeTextView.setText(wifiNetworks.get(itemPosition).getAuthType().toString());
                passwordTextView.setText(wifiNetworks.get(itemPosition).getKey());
                passwordTextView.setTextIsSelectable(true);
                return true;
            case (R.id.context_menu_wifi_list_clear_password):
                removeSavedWifiKey(itemPosition);
                return true;
        }

        return super.onContextItemSelected(item);
    }

    private void removeSavedWifiKey(int position) {
        /* Reset key in local Wi-Fi list */
        wifiNetworks.get(position).setKey("");

        /* Notify adapter that the Wi-Fi network has changed */
        wifiNetworkAdapter.notifyItemChanged(position);

        /* Remove key from saved keys database */
        WifiNetwork wifiNetwork = wifiNetworks.get(position);
        String ssid = wifiNetwork.getSsid();
        WifiAuthType authType = wifiNetwork.getAuthType();
        if (wifiKeysDataSource.removeWifiKey(ssid, authType) == 0) {
            Log.e(TAG, "No key was removed from database");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == PASSWORD_REQUEST) {
            if (resultCode == RESULT_OK) {
                networkIdToUpdate = data.getIntExtra(KEY_NETWORK_ID, -1);
            }
        }
    }

    private class WifiListTask extends AsyncTask<Void, Void, List<WifiNetwork>> {

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
                /* TODO: EAP networks are not yet supported */
                if (wifiNetwork.getAuthType() != WifiAuthType.WPA_EAP
                        && wifiNetwork.getAuthType() != WifiAuthType.WPA2_EAP) {
                    wifiNetworks.add(wifiNetwork);
                    wifiNetworkAdapter.notifyItemInserted(wifiNetworkAdapter.getItemCount() - 1);
                }
            }
            if (!isDeviceRooted) {
                boolean hasReadNoRootDialog = PreferenceManager
                        .getDefaultSharedPreferences(WifiListActivity.this)
                        .getBoolean(PREF_KEY_HAS_READ_NO_ROOT_DIALOG, false);
                if (!hasReadNoRootDialog) {
                    new AlertDialog.Builder(WifiListActivity.this)
                            .setTitle(getString(R.string.wifilist_dialog_noroot_title))
                            .setMessage(getString(R.string.wifilist_dialog_noroot_msg))
                            .setPositiveButton(getString(R.string.action_got_it), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    PreferenceManager.getDefaultSharedPreferences(WifiListActivity.this)
                                            .edit()
                                            .putBoolean(PREF_KEY_HAS_READ_NO_ROOT_DIALOG, true)
                                            .apply();
                                    dialogInterface.dismiss();
                                }
                            })
                            .setCancelable(false)
                            .create()
                            .show();
                }
                setSavedKeysToWifiNetworks();
            }
        }
    }

    private void setSavedKeysToWifiNetworks() {
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
