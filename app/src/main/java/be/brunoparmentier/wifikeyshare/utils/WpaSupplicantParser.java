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

import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

public class WpaSupplicantParser {

    private static final String TAG = WpaSupplicantParser.class.getSimpleName();

    public static List<WifiNetwork> parse(String networkString) {
        List<WifiNetwork> wifiNetworks = new ArrayList<>();
        String[] networkSections = networkString.split("network=");
        for (int i = 1; i < networkSections.length; i++) {
            String networkSection = networkSections[i];
            String name = networkName(networkSection);
            WifiAuthType authType = networkType(networkSection);
            String password = networkPassword(networkSection);
            wifiNetworks.add(new WifiNetwork(name, authType, password, false));
        }

        return wifiNetworks;
    }

    private static String networkName(String networkSection) {
        if (hasToken(networkSection, "ssid")) {
            return parseToken(networkSection, "ssid");
        }

        return "";
    }

    private static WifiAuthType networkType(String networkSection) {
        if (hasToken(networkSection, "wep_key0")) {
            return WifiAuthType.WEP;
        } else if (hasToken(networkSection, "psk")) {
            return WifiAuthType.WPA2_PSK;
        } else if (hasToken(networkSection, "key_mgmt")
                && parseToken(networkSection, "key_mgmt").startsWith("WPA-EAP")) {
            return WifiAuthType.WPA2_EAP;
        } else {
            return WifiAuthType.OPEN;
        }
    }

    private static String networkPassword(String networkSection) {
        switch (networkType(networkSection)) {
            case WPA_PSK:
            case WPA2_PSK:
                return parseToken(networkSection, "psk");
            case WEP:
                return parseToken(networkSection, "wep_key0");
            default:
                return "";
        }
    }

    private static boolean hasToken(String networkSection, String tokenName) {
        return tokenLines(networkSection, tokenName).size() > 0;
    }

    private static List<String> tokenLines(String networkSection, String tokenName) {
        List<String> lines = new ArrayList<>();
        String[] tokenLines = networkSection.split("\n");
        for (String line : tokenLines) {
            if (line.trim().startsWith(tokenName)) {
                lines.add(line.trim());
            }
        }

        return lines;
    }

    private static String parseToken(String networkSection, String tokenName) {
        //Log.d(TAG, networkSection);
        if (hasToken(networkSection, tokenName)) {
            List<String> tokenLines = tokenLines(networkSection, tokenName);
            return tokenLines.get(0).split("=")[1].replace("\"", "");
        }

        return "";
    }
}
