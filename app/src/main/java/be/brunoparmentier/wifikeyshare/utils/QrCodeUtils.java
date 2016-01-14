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
        output.append("S:").append(ssid).append(';');
        if (authType != null && authType != WifiAuthType.OPEN) {
            maybeAppend(output, "T:", authType.toString());
        }
        maybeAppend(output, "P:", key);
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
}
