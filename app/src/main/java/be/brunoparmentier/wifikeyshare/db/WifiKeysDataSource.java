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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import be.brunoparmentier.wifikeyshare.model.WifiAuthType;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

public class WifiKeysDataSource {

    private static final String TAG = WifiKeysDataSource.class.getSimpleName();

    private SQLiteDatabase database;
    private WifiKeysDbHelper dbHelper;

    public WifiKeysDataSource(Context context) {
        dbHelper = new WifiKeysDbHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    /**
     * Return the Wi-Fi configurations with their key
     * @return the Wi-Fi configurations with their key
     */
    public List<WifiNetwork> getSavedWifiWithKeys() {
        List<WifiNetwork> wifiNetworks = new ArrayList<>();

        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                WifiKeysContract.WifiKeys._ID,
                WifiKeysContract.WifiKeys.COLUMN_NAME_SSID,
                WifiKeysContract.WifiKeys.COLUMN_NAME_AUTH_TYPE,
                WifiKeysContract.WifiKeys.COLUMN_NAME_KEY
        };

        // How you want the results sorted in the resulting Cursor
        //String sortOrder = WifiKeysContract.WifiKeys.COLUMN_NAME_SSID + " DESC";

        Cursor cursor = database.query(
                WifiKeysContract.WifiKeys.TABLE_NAME,   // The table to query
                projection,                             // The columns to return
                null,                                   // The columns for the WHERE clause
                null,                                   // The values for the WHERE clause
                null,                                   // don't group the rows
                null,                                   // don't filter by row groups
                null                                    // The sort order
        );

        if (cursor.moveToFirst()) {
            while (!cursor.isAfterLast()) {
                String ssid = cursor.getString(1);
                WifiAuthType authType = WifiAuthType.valueOf(cursor.getString(2));
                String key = cursor.getString(3);
                wifiNetworks.add(new WifiNetwork(ssid, authType, key, false));

                cursor.moveToNext();
            }
        }
        cursor.close();

        return wifiNetworks;
    }

    public String getWifiKey(String ssid, WifiAuthType authType) {
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] projection = {
                WifiKeysContract.WifiKeys._ID,
                WifiKeysContract.WifiKeys.COLUMN_NAME_SSID,
                WifiKeysContract.WifiKeys.COLUMN_NAME_AUTH_TYPE,
                WifiKeysContract.WifiKeys.COLUMN_NAME_KEY
        };
        String selection = WifiKeysContract.WifiKeys.COLUMN_NAME_SSID + " = ? AND " +
                WifiKeysContract.WifiKeys.COLUMN_NAME_AUTH_TYPE + " = ?";
        String[] selectionArgs = {
                ssid,
                authType.toString()
        };

        // How you want the results sorted in the resulting Cursor
        //String sortOrder = WifiKeysContract.WifiKeys.COLUMN_NAME_SSID + " DESC";

        Cursor cursor = database.query(
                WifiKeysContract.WifiKeys.TABLE_NAME,   // The table to query
                projection,                             // The columns to return
                selection,                              // The columns for the WHERE clause
                selectionArgs,                          // The values for the WHERE clause
                null,                                   // don't group the rows
                null,                                   // don't filter by row groups
                null                                    // The sort order
        );

        if (cursor.moveToFirst()) {
            String key = cursor.getString(3);
            cursor.close();
            return key;
        }

        return null;
    }

    /**
     * Insert the new Wi-Fi network containing the key, returning the primary key value of the new row
     * @param wifiNetwork the Wi-Fi configuration to add
     * @return the primary key value of the newly inserted row
     */
    public long insertWifiKey(WifiNetwork wifiNetwork) {
        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put(WifiKeysContract.WifiKeys.COLUMN_NAME_SSID, wifiNetwork.getSsid());
        values.put(WifiKeysContract.WifiKeys.COLUMN_NAME_AUTH_TYPE, wifiNetwork.getAuthType().toString());
        values.put(WifiKeysContract.WifiKeys.COLUMN_NAME_KEY, wifiNetwork.getKey());

        // Insert the new row, returning the primary key value of the new row
        return database.insert(WifiKeysContract.WifiKeys.TABLE_NAME, null, values);
    }

    public int removeWifiKey(String ssid, WifiAuthType authType) {
        String whereClause = WifiKeysContract.WifiKeys.COLUMN_NAME_SSID + " = ? AND " +
                WifiKeysContract.WifiKeys.COLUMN_NAME_AUTH_TYPE + " = ?";
        String[] whereArgs = {
                ssid,
                authType.toString()
        };

        return database.delete(WifiKeysContract.WifiKeys.TABLE_NAME, whereClause, whereArgs);
    }
}
