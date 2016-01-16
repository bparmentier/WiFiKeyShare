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

package be.brunoparmentier.wifikeyshare.db;

import android.provider.BaseColumns;

/**
 * WifiKeys database definition
 */
public final class WifiKeysContract {

    /* Empty constructor to prevent accidentally instantiating the contract class */
    public WifiKeysContract() {}

    /* Inner class that defines the table contents */
    public static abstract class WifiKeys implements BaseColumns {
        public static final String TABLE_NAME = "wifi_keys";
        public static final String COLUMN_NAME_SSID = "ssid";
        public static final String COLUMN_NAME_AUTH_TYPE = "auth_type";
        public static final String COLUMN_NAME_KEY = "key";
    }

}
