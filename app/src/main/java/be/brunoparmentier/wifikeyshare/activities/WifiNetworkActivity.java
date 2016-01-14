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

import android.app.Activity;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.wifi.WifiConfiguration;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.WriterException;

import java.security.InvalidKeyException;

import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;
import be.brunoparmentier.wifikeyshare.utils.NfcUtils;
import be.brunoparmentier.wifikeyshare.utils.QrCodeUtils;

public class WifiNetworkActivity extends AppCompatActivity {

    private static final String TAG = WifiNetworkActivity.class.getSimpleName();

    private static final String KEY_WIFI_NETWORK = "wifi_network";
    private static final String KEY_WIFI_NEEDS_PASSWORD = "wifi_needs_password";

    private WifiNetwork wifiNetwork;
    private boolean isInWriteMode;
    private NfcAdapter nfcAdapter;
    private AlertDialog writeTagDialog;
    private int screenWidth;

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

        if (getIntent().getBooleanExtra(KEY_WIFI_NEEDS_PASSWORD, true)) {
            buildWifiPasswordDialog();
        }

        writeTagDialog = new AlertDialog.Builder(this)
                .setTitle("Write to tag")
                .setMessage("Scan a tag to write the Wi-Fi configuration")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        writeTagDialog.hide();
                    }
                })
                .setCancelable(false)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        disableTagWriteMode();
                    }

                })
                .create();

        isInWriteMode = false;
        getSupportActionBar().setTitle(wifiNetwork.getSsid());
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    void buildWifiPasswordDialog() {
        final LayoutInflater inflater = getLayoutInflater();
        final View wifiPasswordDialogLayout = inflater.inflate(R.layout.dialog_wifi_password, null);

        final TextInputLayout wifiPasswordWrapper = (TextInputLayout) wifiPasswordDialogLayout.findViewById(R.id.wifi_key_wrapper);
        final EditText passwordEditText = (EditText) wifiPasswordDialogLayout.findViewById(R.id.wifi_key);
        setPasswordRestrictions(passwordEditText);

        final AlertDialog wifiPasswordDialog = new AlertDialog.Builder(this)
                .setTitle("Wi-Fi password needed")
                .setMessage("Enter the Wi-Fi password for " + wifiNetwork.getSsid() + ":")
                .setView(wifiPasswordDialogLayout)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // this method gets overriden after we show the dialog
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
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
                        if (isValidPasswordLength(editable.toString())) {
                            wifiPasswordWrapper.setError(null);
                        }
                    } catch (final InvalidKeyException e) {
                        wifiPasswordWrapper.setError(e.getMessage());
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
                    if (isValidPasswordLength(passwordEditText.getText().toString())) {
                        wifiPasswordWrapper.setError(null);
                        wifiNetwork.setKey(passwordEditText.getText().toString());

                        // Get screen width
                        DisplayMetrics dm = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(dm);
                        int width = (int) (dm.widthPixels * 0.5);

                        // Update QR code image
                        FragmentManager fm = getSupportFragmentManager();
                        QrCodeFragment qrCodeFragment = (QrCodeFragment) fm.getFragments().get(0);
                        qrCodeFragment.updateQrCode(width, wifiNetwork);

                        wifiPasswordDialog.dismiss();
                    }
                } catch (InvalidKeyException e) {
                    wifiPasswordWrapper.setError(e.getMessage());
                }
            }
        });
    }

    // TODO: check characters validity (printable ASCII, etc.)
    private boolean isValidPasswordLength(String password) throws InvalidKeyException {
        int passwordLength = password.length();

        if (wifiNetwork.getAuthType() == WifiAuthType.WEP) {
            if (passwordLength != 5 && passwordLength != 13) {
                throw new InvalidKeyException("WEP password must be 5 or 13 characters");
            }
        } else { // WPA
            if ((passwordLength >= 5 && passwordLength < 8) || passwordLength > 63) {
                throw new InvalidKeyException("WPA password must be between 8 and 63 characters");
            }
        }

        return true;
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

    @Override
    protected void onPause() {
        super.onPause();
        stopForegroundDispatch(this, nfcAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupForegroundDispatch(this, nfcAdapter);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter  The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[3];
        String[][] techList = new String[][]{};

        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        filters[1] = new IntentFilter();
        filters[1].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters[1].addCategory(Intent.CATEGORY_DEFAULT);
        filters[2] = new IntentFilter();
        filters[2].addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filters[2].addCategory(Intent.CATEGORY_DEFAULT);

        try {
            filters[0].addDataType("*/*");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, e.getMessage());
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter  The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    private void enableTagWriteMode() {
        isInWriteMode = true;
        writeTagDialog.show();
    }

    private void disableTagWriteMode() {
        isInWriteMode = false;
        writeTagDialog.hide();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (isInWriteMode) {
            /* Write tag */
            Log.d(TAG, "Writing tag");
            String action = intent.getAction();
            if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                    || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

                if (NfcUtils.writeTag(wifiNetwork, tag)) {
                    Toast.makeText(this, "Successfully wrote to NFC tag", Toast.LENGTH_LONG)
                            .show();
                }
                disableTagWriteMode();
            }
        } else {
            /* Read tag */
            Log.d(TAG, "Reading tag");

            WifiConfiguration wifiConfiguration = NfcUtils.readTag(tag);

            if (wifiConfiguration == null) {
                Log.d(TAG, "Not a Wi-Fi configuration tag");
            } else {
                Log.d(TAG, wifiConfiguration.toString());
            }

            Toast.makeText(this, "NFC tag read", Toast.LENGTH_LONG).show();
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
        if (id == R.id.action_settings) {
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

            DisplayMetrics dm = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(dm);
            int width = (int) (dm.widthPixels * 0.5);

            updateQrCode(width, wifiNetwork);

            return rootView;
        }

        public void updateQrCode(int width, WifiNetwork wifiNetwork) {
            try {
                Bitmap qrCodeBitmap = QrCodeUtils.generateWifiQrCode(width, wifiNetwork);
                qrCodeImageView.setImageBitmap(qrCodeBitmap);
            } catch (final WriterException e) {
                Log.d(TAG, e.getMessage());
            }
        }
    }

    public static class NfcFragment extends Fragment {

        public NfcFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
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

            Button writeNfcButton = (Button) rootView.findViewById(R.id.nfc_write_button);
            writeNfcButton.setText("Write");
            writeNfcButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((WifiNetworkActivity) getActivity()).enableTagWriteMode();
                }
            });

            return rootView;
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
                    return "QR code";
                case 1:
                    return "NFC";
            }
            return null;
        }
    }
}
