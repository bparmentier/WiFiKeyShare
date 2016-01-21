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

package be.brunoparmentier.wifikeyshare.utils;

import android.net.wifi.WifiConfiguration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.BitSet;

import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

/**
 * Utility class containing functions to read/write NFC tags with Wi-Fi configurations
 */
public class NfcUtils {
    private static final String TAG = NfcUtils.class.getSimpleName();

    public static final String NFC_TOKEN_MIME_TYPE = "application/vnd.wfa.wsc";
    /*
     * ID into configuration record for SSID and Network Key in hex.
     * Obtained from WFA Wi-Fi Simple Configuration Technical Specification v2.0.5.
     */
    public static final short CREDENTIAL_FIELD_ID = 0x100E;

    public static final short NETWORK_INDEX_FIELD_ID = 0x1026;
    public static final byte NETWORK_INDEX_DEFAULT_VALUE = (byte) 0x1;

    public static final short SSID_FIELD_ID = 0x1045;

    public static final short AUTH_TYPE_FIELD_ID = 0x1003;
    public static final short AUTH_TYPE_EXPECTED_SIZE = 2;
    public static final short AUTH_TYPE_OPEN = 0;
    public static final short AUTH_TYPE_WPA_PSK = 0x0002;
    public static final short AUTH_TYPE_WPA_EAP = 0x0008;
    public static final short AUTH_TYPE_WPA2_EAP = 0x0010;
    public static final short AUTH_TYPE_WPA2_PSK = 0x0020;

    public static final short ENC_TYPE_FIELD_ID = 0x100F;
    public static final short ENC_TYPE_NONE = 0x0001;
    public static final short ENC_TYPE_WEP = 0x0002; // deprecated
    public static final short ENC_TYPE_TKIP = 0x0004; // deprecated -> only with mixed mode (0x000c)
    public static final short ENC_TYPE_AES = 0x0008; // includes CCMP and GCMP
    public static final short ENC_TYPE_AES_TKIP = 0x000c; // mixed mode

    public static final short NETWORK_KEY_FIELD_ID = 0x1027;
    // WPA2-personal (passphrase): 8-63 ASCII characters
    // WPA2-personal: 64 hex characters

    public static final short MAC_ADDRESS_FIELD_ID = 0x1020;

    public static final int MAX_SSID_SIZE_BYTES = 32;
    public static final int MAX_MAC_ADDRESS_SIZE_BYTES = 6;
    public static final int MAX_NETWORK_KEY_SIZE_BYTES = 64;

    /**
     * Write the given Wi-Fi configuration to the NFC tag.
     *
     * The tag is NDEF-formatted with the application/vnd.wfa.wsc MIME type
     *
     * @param wifiNetwork the Wi-Fi configuration to write to the tag
     * @param tag the tag to write
     * @return true if the configuration has successfully been written to the tag, false otherwise
     */
    public static boolean writeTag(WifiNetwork wifiNetwork, Tag tag) {
        return NfcUtils.writeTag(generateNdefMessage(wifiNetwork), tag);
    }

    /**
     * Generate an NDEF message containing the given Wi-Fi configuration
     *
     * @param wifiNetwork the Wi-Fi configuration to convert
     * @return an NDEF message containing the given Wi-Fi configuration
     */
    public static NdefMessage generateNdefMessage(WifiNetwork wifiNetwork) {
        byte[] payload = generateNdefPayload(wifiNetwork);

        NdefRecord mimeRecord = new NdefRecord(
                NdefRecord.TNF_MIME_MEDIA,
                NfcUtils.NFC_TOKEN_MIME_TYPE.getBytes(Charset.forName("US-ASCII")),
                new byte[0],
                payload);
        return new NdefMessage(new NdefRecord[]{mimeRecord});
    }

    private static byte[] generateNdefPayload(WifiNetwork wifiNetwork) {
        String ssid = wifiNetwork.getSsid();
        short ssidSize = (short) ssid.getBytes().length;

        short authType;
        switch (wifiNetwork.getAuthType()) {
            case WPA_PSK:
                authType = AUTH_TYPE_WPA_PSK;
                break;
            case WPA2_PSK:
                authType = AUTH_TYPE_WPA2_PSK;
                break;
            case WPA_EAP:
                authType = AUTH_TYPE_WPA_EAP;
                break;
            case WPA2_EAP:
                authType = AUTH_TYPE_WPA2_EAP;
                break;
            default:
                authType = AUTH_TYPE_OPEN;
                break;
        }

        /*short encType;
        switch (wifiNetwork.getEncType()) {
            case WEP:
                encType = ENC_TYPE_WEP;
                break;
            case TKIP:
                encType = ENC_TYPE_TKIP;
                break;
            case AES:
                encType = ENC_TYPE_AES;
                break;
            case AES_TKIP:
                encType = ENC_TYPE_AES_TKIP;
                break;
            default:
                encType = ENC_TYPE_NONE;
                break;
        }*/

        String networkKey = wifiNetwork.getKey();
        short networkKeySize = (short) networkKey.getBytes().length;

        byte[] macAddress = new byte[MAX_MAC_ADDRESS_SIZE_BYTES];
        for (int i = 0; i < MAX_MAC_ADDRESS_SIZE_BYTES; i++) {
            macAddress[i] = (byte) 0xff;
        }

        /* Fill buffer */

        int bufferSize = 39 + ssidSize + networkKeySize; // size of required credential attributes

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        buffer.putShort(CREDENTIAL_FIELD_ID);
        buffer.putShort((short) (bufferSize - 4));

        buffer.putShort(NETWORK_INDEX_FIELD_ID);
        buffer.putShort((short) 1);
        buffer.put(NETWORK_INDEX_DEFAULT_VALUE);

        buffer.putShort(SSID_FIELD_ID);
        buffer.putShort(ssidSize);
        buffer.put(ssid.getBytes());

        buffer.putShort(AUTH_TYPE_FIELD_ID);
        buffer.putShort((short) 2);
        buffer.putShort(authType);

        buffer.putShort(ENC_TYPE_FIELD_ID);
        buffer.putShort((short) 2);
        buffer.putShort(ENC_TYPE_NONE); // FIXME

        buffer.putShort(NETWORK_KEY_FIELD_ID);
        buffer.putShort(networkKeySize);
        buffer.put(networkKey.getBytes());

        buffer.putShort(MAC_ADDRESS_FIELD_ID);
        buffer.putShort((short) MAX_MAC_ADDRESS_SIZE_BYTES);
        buffer.put(macAddress);

        return buffer.array();
    }

    /**
     * Write the given NDEF message to the NFC tag
     * TODO: throw exception on error
     *
     * @param message the NDEF message to write
     * @param tag the NFC tag
     * @return true if the NDEF message was successfully written to the tag, false otherwise
     */
    private static boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Log.d(TAG, "Error: tag not writable");
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Log.d(TAG, "Error: tag too small");
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static WifiConfiguration readTag(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) {
            Log.d(TAG, "NDEF not supported");
            return null;
        }

        NdefMessage ndefMessage = ndef.getCachedNdefMessage();
        if (ndefMessage == null) {
            Log.d(TAG, "ndefMessage is null");
            return null;
        }
        return NfcUtils.parse(ndefMessage);
    }

    /**
     * Parse an NDEF message and return the corresponding Wi-Fi configuration
     *
     * Source: http://androidxref.com/6.0.1_r10/xref/packages/apps/Nfc/src/com/android/nfc/NfcWifiProtectedSetup.java
     *
     * @param message the NDEF message to parse
     * @return a WifiConfiguration extracted from the NDEF message
     */
    private static WifiConfiguration parse(NdefMessage message) {
        NdefRecord[] records = message.getRecords();
        for (NdefRecord record : records) {
            if (new String(record.getType()).equals(NFC_TOKEN_MIME_TYPE)) {
                ByteBuffer payload = ByteBuffer.wrap(record.getPayload());
                while (payload.hasRemaining()) {
                    short fieldId = payload.getShort();
                    short fieldSize = payload.getShort();
                    if (fieldId == CREDENTIAL_FIELD_ID) {
                        return parseCredential(payload, fieldSize);
                    }
                }
            }
        }
        return null;
    }

    private static WifiConfiguration parseCredential(ByteBuffer payload, short size) {
        int startPosition = payload.position();
        WifiConfiguration result = new WifiConfiguration();
        while (payload.position() < startPosition + size) {
            short fieldId = payload.getShort();
            short fieldSize = payload.getShort();
            // sanity check
            if (payload.position() + fieldSize > startPosition + size) {
                return null;
            }
            switch (fieldId) {
                case SSID_FIELD_ID:
                    byte[] ssid = new byte[fieldSize];
                    payload.get(ssid);
                    result.SSID = "\"" + new String(ssid) + "\"";
                    break;
                case NETWORK_KEY_FIELD_ID:
                    if (fieldSize > MAX_NETWORK_KEY_SIZE_BYTES) {
                        return null;
                    }
                    byte[] networkKey = new byte[fieldSize];
                    payload.get(networkKey);
                    result.preSharedKey = "\"" + new String(networkKey) + "\"";
                    break;
                case AUTH_TYPE_FIELD_ID:
                    if (fieldSize != AUTH_TYPE_EXPECTED_SIZE) {
                        // corrupt data
                        return null;
                    }
                    short authType = payload.getShort();
                    populateAllowedKeyManagement(result.allowedKeyManagement, authType);
                    break;
                default:
                    // unknown / unparsed tag
                    payload.position(payload.position() + fieldSize);
                    break;
            }
        }
        if (result.preSharedKey != null && result.SSID != null) {
            return result;
        }
        return null;
    }

    private static void populateAllowedKeyManagement(BitSet allowedKeyManagement, short authType) {
        if (authType == AUTH_TYPE_WPA_PSK || authType == AUTH_TYPE_WPA2_PSK) {
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        } else if (authType == AUTH_TYPE_WPA_EAP || authType == AUTH_TYPE_WPA2_EAP) {
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
        } else if (authType == AUTH_TYPE_OPEN) {
            allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
    }
}