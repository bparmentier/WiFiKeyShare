/*
 * Copyright 2018 David Mawer
 * https://github.com/David-Mawer/OreoWifiPasswords/blob/0d146fd34ce424b8a500a441ff2a1293c3355a33/app/src/main/java/com/pithsoftware/wifipasswords/task/TaskLoadWifiEntries.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package be.brunoparmentier.wifikeyshare.utils;

import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

public class WifiConfigStoreParser {

    public WifiConfigStoreParser() {
        throw new IllegalStateException("Utility class");
    }

    public static List<WifiNetwork> parse(InputStream in)
            throws XmlPullParserException, IOException {
        List<WifiNetwork> wifiNetworks = new ArrayList<>();

        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                if (parser.getName().equalsIgnoreCase("NetworkList")) {
                    wifiNetworks.addAll(readNetworkList(parser));
                }
            }
            return wifiNetworks;
        } finally {
            in.close();
        }
    }

    private static List<WifiNetwork> readNetworkList(XmlPullParser parser)
            throws XmlPullParserException, IOException {
        List<WifiNetwork> result = new ArrayList<>();
        parser.require(XmlPullParser.START_TAG, null, "NetworkList");
        boolean doLoop = true;
        while (doLoop) {
            try {
                parser.next();
                String tagName = parser.getName();
                if (tagName == null) {
                    tagName = "";
                }
                doLoop = (!tagName.equalsIgnoreCase("NetworkList"));
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }

                if (tagName.equals("Network")) {
                    WifiNetwork newWifi = readNetworkEntry(parser);
                    if (newWifi.getSsid().length() != 0) {
                        result.add(newWifi);
                    }
                } else {
                    skip(parser);
                }
            } catch (Exception e) {
                Log.e("LoadData.NetworkList", e.getMessage());
                doLoop = false;
            }
        }
        return result;
    }

    private static WifiNetwork readNetworkEntry(XmlPullParser parser) throws XmlPullParserException, IOException {
        parser.require(XmlPullParser.START_TAG, null, "Network");
        WifiNetwork result = null;
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                continue;
            }
            String tagName = parser.getName();
            // Starts by looking for the entry tag
            if (tagName.equals("WifiConfiguration")) {
                result = readWiFiConfig(parser);
//            } else if (tagName.equals("WifiEnterpriseConfiguration")) {
//                result.setTyp(WifiObject.TYP_ENTERPRISE);
            } else {
                skip(parser);
            }
        }
        return result;
    }

    private static WifiNetwork readWiFiConfig(XmlPullParser parser) {
        String ssid = "";
        WifiAuthType authType = WifiAuthType.OPEN;
        String key = "";

        try {
            while (parser.next() != XmlPullParser.END_TAG) {
                if (parser.getEventType() != XmlPullParser.START_TAG) {
                    continue;
                }
                String tagName = parser.getName();
                String name = parser.getAttributeValue(null, "name");
                if (name.equals("ConfigKey") && !tagName.equalsIgnoreCase("null")) {
                    String configKey = readTag(parser, tagName);
                    String parsedAuthType = configKey.substring(configKey.lastIndexOf("\"") + 1);
                    switch (parsedAuthType) {
                        case "NONE":
                            authType = WifiAuthType.OPEN;
                            break;
                        case "WEP":
                            authType = WifiAuthType.WEP;
                            break;
                        case "WPA_PSK":
                            authType = WifiAuthType.WPA2_PSK;
                            break;
                        case "WPA_EAP":
                            authType = WifiAuthType.WPA2_EAP;
                            break;
                    }
                } else if (name.equals("SSID") && !tagName.equalsIgnoreCase("null")) {
                    ssid = readTag(parser, tagName);
                } else if (name.equals("PreSharedKey") && !tagName.equalsIgnoreCase("null")) {
                    String newKey = readTag(parser, tagName);
                    if (newKey.length() > 0) {
                        key = newKey;
                    }
                } else if (name.equals("WEPKeys") && !tagName.equalsIgnoreCase("null")) {
                    if (tagName.equalsIgnoreCase("string-array")) {
                        try {
                            int numQty = Integer.parseInt(parser.getAttributeValue(null, "num"));
                            int loopQty = 0;
                            while ((parser.next() != XmlPullParser.END_DOCUMENT) && (loopQty < numQty)) {
                                String innerTagName = parser.getName();
                                if ((innerTagName != null) && innerTagName.equalsIgnoreCase("item")) {
                                    loopQty++;
                                    String newKey = parser.getAttributeValue(null, "value");
                                    if (newKey.length() > 0) {
                                        key = newKey.substring(1, newKey.length() - 1);
                                    }
                                }
                            }
                        } catch (Exception error) {
                            parser.getName();
                        }
                    } else {
                        String newPwd = readTag(parser, tagName);
                        if (newPwd.length() > 0) {
                            key = readTag(parser, tagName);
                        }
                    }
                } else {
                    skip(parser);
                }
            }
        } catch (Exception error) {
            Log.e("LoadData.readWiFiConfig", error.getMessage() + "\n\nParser: " + parser.getText());
        }
        return new WifiNetwork(ssid, authType, key, false);
    }

    // Return the text for a specified tag.
    private static String readTag(XmlPullParser parser, String tagName)
            throws IOException, XmlPullParserException {
        parser.require(XmlPullParser.START_TAG, null, tagName);
        String result = "";
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.getText();
            parser.nextTag();
        }
        parser.require(XmlPullParser.END_TAG, null, tagName);
        if (tagName.equalsIgnoreCase("string")
                && Character.toString(result.charAt(0)).equals("\"")
                && Character.toString(result.charAt(result.length() - 1)).equals("\"")) {
            result = result.substring(1, result.length() - 1);
        }
        return result;
    }

    // Skips tags the parser isn't interested in. Uses depth to handle nested tags. i.e.,
    // if the next tag after a START_TAG isn't a matching END_TAG, it keeps going until it
    // finds the matching END_TAG (as indicated by the value of "depth" being 0).
    private static void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
        if (parser.getEventType() != XmlPullParser.START_TAG) {
            throw new IllegalStateException();
        }
        int depth = 1;
        while (depth != 0) {
            switch (parser.next()) {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }
}
