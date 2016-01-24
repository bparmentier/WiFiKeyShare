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

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.WifiException;
import be.brunoparmentier.wifikeyshare.db.WifiKeysDataSource;
import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;
import be.brunoparmentier.wifikeyshare.utils.AboutDialog;
import be.brunoparmentier.wifikeyshare.utils.NfcUtils;
import be.brunoparmentier.wifikeyshare.utils.QrCodeUtils;

public class WifiNetworkActivity extends AppCompatActivity {

    private static final String TAG = WifiNetworkActivity.class.getSimpleName();

    private static final String KEY_WIFI_NETWORK = "wifi_network";
    private static final String KEY_NETWORK_ID = "network_id";

    private WifiNetwork wifiNetwork;
    private int wifiNetworkId;
    private boolean isInWriteMode;
    private NfcAdapter nfcAdapter;
    private BroadcastReceiver nfcStateChangeBroadcastReceiver;
    private AlertDialog writeTagDialog;
    private int screenWidth;
    private PendingIntent nfcPendingIntent;
    private IntentFilter[] nfcIntentFilters;
    private String[][] nfcTechLists;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_network);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        SectionsPagerAdapter mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        ViewPager mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

        wifiNetwork = (WifiNetwork) getIntent().getSerializableExtra(KEY_WIFI_NETWORK);

        if (wifiNetwork.needsPassword()) {
            wifiNetworkId = getIntent().getIntExtra(KEY_NETWORK_ID, -1);
            showWifiPasswordDialog();
        }

        writeTagDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.write_to_tag))
                .setMessage(getString(R.string.write_to_tag_msg))
                .setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        disableTagWriteMode();
                        dialogInterface.dismiss();
                    }
                })
                .setCancelable(false)
                .create();

        isInWriteMode = false;
        getSupportActionBar().setTitle(wifiNetwork.getSsid());
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (isNfcAvailable()) {
            initializeNfcStateChangeListener();
            setupForegroundDispatch();
            nfcAdapter.setNdefPushMessage(NfcUtils.generateNdefMessage(wifiNetwork), this);
        }
    }

    void showWifiPasswordDialog() {
        final LayoutInflater inflater = getLayoutInflater();
        final View wifiPasswordDialogLayout = inflater.inflate(R.layout.dialog_wifi_password, null);

        final TextInputLayout wifiPasswordWrapper = (TextInputLayout) wifiPasswordDialogLayout.findViewById(R.id.wifi_key_wrapper);
        final EditText passwordEditText = (EditText) wifiPasswordDialogLayout.findViewById(R.id.wifi_key);
        //setPasswordRestrictions(passwordEditText);
        final CheckBox showPasswordCheckBox = (CheckBox) wifiPasswordDialogLayout.findViewById(R.id.show_password_checkbox);
        showPasswordCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                int selectionIndex = passwordEditText.getSelectionStart();
                if (isChecked) {
                    passwordEditText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                } else {
                    passwordEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                }
                passwordEditText.setSelection(selectionIndex);
            }
        });

        final AlertDialog wifiPasswordDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.wifi_dialog_password_title))
                .setMessage(String.format(getString(R.string.wifi_dialog_password_msg), wifiNetwork.getSsid()))
                .setView(wifiPasswordDialogLayout)
                .setPositiveButton(getString(R.string.action_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // this method gets overriden after we show the dialog
                    }
                })
                .setNegativeButton(getString(R.string.action_cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .create();


        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                wifiPasswordDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setEnabled(editable.length() >= 5);
                if (wifiPasswordWrapper.getError() != null) {
                    try {
                        if (WifiNetwork.isValidKeyLength(wifiNetwork.getAuthType(),
                                editable.toString())) {
                            wifiPasswordWrapper.setError(null);
                        }
                    } catch (final WifiException e) {
                        switch (e.getErrorCode()) {
                            case WifiException.WEP_KEY_LENGTH_ERROR:
                                wifiPasswordWrapper.setError(getString(R.string.error_wep_password_length));
                                break;
                            case WifiException.WPA_KEY_LENGTH_ERROR:
                                wifiPasswordWrapper.setError(getString(R.string.error_wpa_password_length));
                                break;
                            default:
                                wifiPasswordWrapper.setError(e.getMessage());
                                break;
                        }
                    }
                }
            }
        });

        wifiPasswordDialog.show();

        wifiPasswordDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false); // disabled by default
        wifiPasswordDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (WifiNetwork.isValidKeyLength(wifiNetwork.getAuthType(),
                            passwordEditText.getText().toString())) {

                        wifiPasswordWrapper.setError(null);
                        wifiNetwork.setKey(passwordEditText.getText().toString());

                        // Update QR code image
                        FragmentManager fm = getSupportFragmentManager();
                        QrCodeFragment qrCodeFragment = (QrCodeFragment) fm.getFragments().get(0);
                        qrCodeFragment.updateQrCode(wifiNetwork);

                        WifiKeysDataSource wifiKeysDataSource = new WifiKeysDataSource(WifiNetworkActivity.this);
                        wifiKeysDataSource.open();
                        wifiKeysDataSource.insertWifiKey(wifiNetwork);
                        wifiKeysDataSource.close();

                        Intent passwordResultIntent = new Intent();
                        passwordResultIntent.putExtra(KEY_NETWORK_ID, wifiNetworkId);
                        setResult(RESULT_OK, passwordResultIntent);

                        wifiPasswordDialog.dismiss();
                    }
                } catch (WifiException e) {
                    switch (e.getErrorCode()) {
                        case WifiException.WEP_KEY_LENGTH_ERROR:
                            wifiPasswordWrapper.setError(getString(R.string.error_wep_password_length));
                            break;
                        case WifiException.WPA_KEY_LENGTH_ERROR:
                            wifiPasswordWrapper.setError(getString(R.string.error_wpa_password_length));
                            break;
                        default:
                            wifiPasswordWrapper.setError(null);
                            break;
                    }
                }
            }
        });
    }

    private static void setPasswordRestrictions(EditText editText) {
        // Source: http://stackoverflow.com/a/4401227
        InputFilter filter = new InputFilter() {

            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                // TODO: check that the filter follows WEP/WPA recommendations
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };
        editText.setFilters(new InputFilter[]{filter});
    }

    void initializeNfcStateChangeListener() {
        nfcStateChangeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();

                if (action.equals(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED)) {
                    final int state = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);

                    switch (state) {
                        case NfcAdapter.STATE_OFF:
                        case NfcAdapter.STATE_TURNING_OFF:
                            onNfcDisabled();
                            break;
                        case NfcAdapter.STATE_TURNING_ON:
                            break;
                        case NfcAdapter.STATE_ON:
                            onNfcEnabled();
                            break;
                    }
                }
            }
        };
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isNfcAvailable()) {
            stopForegroundDispatch();
            unregisterReceiver(nfcStateChangeBroadcastReceiver);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNfcAvailable()) {
            IntentFilter filter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
            registerReceiver(nfcStateChangeBroadcastReceiver, filter);
            startForegroundDispatch();
        }
    }

    private void enableTagWriteMode() {
        isInWriteMode = true;
        writeTagDialog.show();
    }

    private void disableTagWriteMode() {
        isInWriteMode = false;
        writeTagDialog.dismiss();
    }

    protected boolean isNfcAvailable() {
        return (nfcAdapter != null);
    }

    protected boolean isNfcEnabled() {
        return (isNfcAvailable() && nfcAdapter.isEnabled());
    }

    protected void onNfcEnabled() {
        if (wifiNetwork.getAuthType() != WifiAuthType.WEP) { // writing WEP config is not supported
            // Update NFC write button and status text
            FragmentManager fm = getSupportFragmentManager();
            NfcFragment nfcFragment = (NfcFragment) fm.getFragments().get(1);
            nfcFragment.setNfcStateEnabled(true);
        }
    }

    protected void onNfcDisabled() {
        if (wifiNetwork.getAuthType() != WifiAuthType.WEP) { // writing WEP config is not supported
            // Update NFC write button and status text
            FragmentManager fm = getSupportFragmentManager();
            NfcFragment nfcFragment = (NfcFragment) fm.getFragments().get(1);
            nfcFragment.setNfcStateEnabled(false);
        }
    }

    private void setupForegroundDispatch() {
        /* initialize the PendingIntent to start for the dispatch */
        nfcPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

        /* initialize the IntentFilters to override dispatching for */
        nfcIntentFilters = new IntentFilter[3];
        nfcIntentFilters[0] = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        nfcIntentFilters[0].addCategory(Intent.CATEGORY_DEFAULT);
        nfcIntentFilters[1] = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        nfcIntentFilters[1].addCategory(Intent.CATEGORY_DEFAULT);
        nfcIntentFilters[2] = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        nfcIntentFilters[2].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            nfcIntentFilters[0].addDataType("*/*"); // Handle all MIME based dispatches.
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "setupForegroundDispatch: " + e.getMessage());
        }

        /* Initialize the tech lists used to perform matching for dispatching of the
         * ACTION_TECH_DISCOVERED intent */
        nfcTechLists = new String[][] {};
    }

    private void startForegroundDispatch() {
        nfcAdapter.enableForegroundDispatch(this, nfcPendingIntent, nfcIntentFilters, nfcTechLists);
    }

    private void stopForegroundDispatch() {
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        String action = intent.getAction();
        Log.d(TAG, "handleIntent: action=" + action);
        if (isInWriteMode) {
            /* Write tag */
            Log.d(TAG, "Writing tag");
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                    || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                if (NfcUtils.writeTag(wifiNetwork, tag)) {
                    Toast.makeText(this, R.string.nfc_tag_written, Toast.LENGTH_LONG)
                            .show();
                }
                disableTagWriteMode();
            }
        } else {
            /* Read tag */
            Log.d(TAG, "Reading tag");

            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
                if (NfcUtils.NFC_TOKEN_MIME_TYPE.equals(intent.getType())) {
                    Intent configureNetworkIntent = new Intent(intent)
                            .setClass(this, ConfirmConnectToWifiNetworkActivity.class)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    startActivity(configureNetworkIntent);
                } else {
                    Log.d(TAG, "Not a Wi-Fi configuration tag");
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wifi_network, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_about) {
            final AlertDialog aboutDialog = new AboutDialog(this);
            aboutDialog.show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class QrCodeFragment extends Fragment {

        private ImageView qrCodeImageView;

        public QrCodeFragment() {
        }

        public static QrCodeFragment newInstance(WifiNetwork wifiNetwork) {
            QrCodeFragment fragment = new QrCodeFragment();
            Bundle args = new Bundle();
            args.putSerializable(KEY_WIFI_NETWORK, wifiNetwork);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_qrcode, container, false);

            WifiNetwork wifiNetwork = (WifiNetwork) getArguments().getSerializable(KEY_WIFI_NETWORK);

            qrCodeImageView = (ImageView) rootView.findViewById(R.id.qr_code);
            qrCodeImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: show fullscreen QR code
                }
            });

            updateQrCode(wifiNetwork);

            return rootView;
        }

        public void updateQrCode(WifiNetwork wifiNetwork) {
            DisplayMetrics metrics = getContext().getResources().getDisplayMetrics();
            float widthInDp = 200f;
            int widthInPixels = (int) (metrics.density * widthInDp + 0.5f);
            try {
                Bitmap qrCodeBitmap = QrCodeUtils.generateWifiQrCode(widthInPixels, wifiNetwork);
                qrCodeImageView.setImageBitmap(qrCodeBitmap);
            } catch (final WriterException e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    public static class NfcFragment extends Fragment {

        private Button writeTagButton;
        private TextView nfcStatusTextView;

        public NfcFragment() {
        }

        public static NfcFragment newInstance(WifiNetwork wifiNetwork) {
            NfcFragment fragment = new NfcFragment();
            Bundle args = new Bundle();
            args.putSerializable(KEY_WIFI_NETWORK, wifiNetwork);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_nfc, container, false);

            WifiNetwork wifiNetwork = (WifiNetwork) getArguments().getSerializable(KEY_WIFI_NETWORK);

            writeTagButton = (Button) rootView.findViewById(R.id.nfc_write_button);
            writeTagButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((WifiNetworkActivity) getActivity()).enableTagWriteMode();
                }
            });

            nfcStatusTextView = (TextView) rootView.findViewById(R.id.nfc_status);

            if (wifiNetwork.getAuthType() == WifiAuthType.WEP) {
                writeTagButton.setEnabled(false);
                nfcStatusTextView.setText(R.string.error_wep_to_nfc_not_supported);
            } else {
                boolean isNfcAvailable = ((WifiNetworkActivity) getActivity()).isNfcAvailable();
                boolean isNfcEnabled = (((WifiNetworkActivity) getActivity()).isNfcEnabled());

                if (!isNfcAvailable) {
                    setNfcStateAvailable(false);
                } else if (!isNfcEnabled) {
                    setNfcStateEnabled(false);
                }
            }

            return rootView;
        }

        public void setNfcStateEnabled(boolean enabled) {
            writeTagButton.setEnabled(enabled);
            if (enabled) {
                nfcStatusTextView.setText(null);
            } else {
                nfcStatusTextView.setText(R.string.error_turn_nfc_on);
            }
        }

        public void setNfcStateAvailable(boolean available) {
            writeTagButton.setEnabled(available);
            if (available) {
                nfcStatusTextView.setText(null);
            } else {
                nfcStatusTextView.setText(R.string.error_nfc_not_available);
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 1) {
                return NfcFragment.newInstance(wifiNetwork);
            } else {
                return QrCodeFragment.newInstance(wifiNetwork);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.qrcode_fragment_tab_title);
                case 1:
                    return getString(R.string.nfc_fragment_tab_title);
            }
            return null;
        }
    }
}
