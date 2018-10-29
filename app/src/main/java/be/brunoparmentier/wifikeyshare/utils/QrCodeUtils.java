/*
 * WiFiKeyShare. Share Wi-Fi passwords with QR codes or NFC tags.
 * Copyright (C) 2016, 2018 Bruno Parmentier <dev@brunoparmentier.be>
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

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

/**
 * Utility class containing functions to generate QR codes
 */
public class QrCodeUtils {

    /**
     * Generate a QR code containing the given Wi-Fi configuration
     *
     * @param width the width of the QR code
     * @param wifiNetwork the Wi-Fi configuration
     * @return a bitmap representing the QR code
     * @throws WriterException if the Wi-Fi configuration cannot be represented in the QR code
     */
    public static Bitmap generateWifiQrCode(int width, WifiNetwork wifiNetwork) throws WriterException {
        int height = width;
        com.google.zxing.Writer writer = new QRCodeWriter();
        String wifiString = getWifiString(wifiNetwork);

        BitMatrix bitMatrix = writer.encode(wifiString, BarcodeFormat.QR_CODE, width, height);
        Bitmap imageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                imageBitmap.setPixel(i, j, bitMatrix.get(i, j) ? Color.BLACK : Color.WHITE);
            }
        }

        return imageBitmap;
    }

    /**
     * Generate a Wi-Fi configuration string formatted as follows (proposed by ZXing):
     *
     *     WIFI:T:WPA;S:mynetwork;P:mypass;;
     *
     * See: https://github.com/zxing/zxing/wiki/Barcode-Contents#wifi-network-config-android
     *
     * @param wifiNetwork the Wi-Fi configuration to encode
     * @return the generated string encoding the Wi-Fi configuration
     */
    private static String getWifiString(WifiNetwork wifiNetwork) {
        String ssid = wifiNetwork.getSsid();
        WifiAuthType authType = wifiNetwork.getAuthType();
        String key = wifiNetwork.getKey();
        boolean isHidden = wifiNetwork.isHidden();

        StringBuilder output = new StringBuilder(100);
        output.append("WIFI:");
        output.append("T:");
        if (authType == WifiAuthType.OPEN) {
            output.append("nopass");
        } else if (authType == WifiAuthType.WEP) {
            output.append("WEP");
        } else {
            output.append("WPA"); // FIXME: support EAP?
        }
        output.append(";");
        maybeAppend(output, "P:", escapeMecard(key));
        output.append("S:").append(escapeMecard(ssid)).append(';');
        if (isHidden) {
            maybeAppend(output, "H:", "true");
        }
        output.append(';');

        return output.toString();
    }

    private static void maybeAppend(StringBuilder output, String prefix, String value) {
        if (value != null && !value.isEmpty()) {
            output.append(prefix).append(value).append(';');
        }
    }

    private static String escapeMecard(String input) {
        return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace(":", "\\:");
    }
}
