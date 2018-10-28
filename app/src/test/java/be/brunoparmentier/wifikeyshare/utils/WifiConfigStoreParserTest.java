/*
 * WiFiKeyShare. Share Wi-Fi passwords with QR codes or NFC tags.
 * Copyright (C) 2018 Bruno Parmentier <dev@brunoparmentier.be>
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package be.brunoparmentier.wifikeyshare.utils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class WifiConfigStoreParserTest {

    private final List<WifiNetwork> expectedWifiNetworks = new ArrayList<>();

    @Before
    public void setUp() {
        expectedWifiNetworks.add(new WifiNetwork(
                "test1", WifiAuthType.OPEN, "", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "test2", WifiAuthType.WEP, "test", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "test3", WifiAuthType.WPA2_PSK, "test1234", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "test4", WifiAuthType.WPA2_PSK, "ABCDEFGHI0123456789JKLMNOP", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "test5", WifiAuthType.WPA2_PSK, "\";/\\=#}`,\\\"$4<d)=%", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "test6", WifiAuthType.WPA2_EAP, "", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "test7", WifiAuthType.WPA2_EAP, "", false));
        expectedWifiNetworks.add(new WifiNetwork(
                "a\"=$\\o(#=>™•,k8", WifiAuthType.OPEN, "", false));


    }

    @Test
    public void parse() throws XmlPullParserException, IOException {
        InputStream wifiConfigStoreFileStream =
                getClass().getClassLoader().getResourceAsStream("test_WifiConfigStore.xml");

        List<WifiNetwork> parsedWifiNetworks = WifiConfigStoreParser.parse(wifiConfigStoreFileStream);
        assertEquals(expectedWifiNetworks.size(), parsedWifiNetworks.size());
        for (int i = 0; i < expectedWifiNetworks.size(); i++) {
            assertEquals(expectedWifiNetworks.get(i), parsedWifiNetworks.get(i));
        }
    }
}
