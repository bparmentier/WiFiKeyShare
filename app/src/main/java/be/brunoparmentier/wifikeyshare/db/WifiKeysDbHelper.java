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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Database helper class that manages database operations (creation/upgrade/deletion)
 */
public class WifiKeysDbHelper extends SQLiteOpenHelper {
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "wifi_keys_db";

    private static final String TEXT_TYPE = " TEXT";
    private static final String COMMA_SEP = ",";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + WifiKeysContract.WifiKeys.TABLE_NAME + " (" +
                    WifiKeysContract.WifiKeys._ID + " INTEGER PRIMARY KEY," +
                    WifiKeysContract.WifiKeys.COLUMN_NAME_SSID + TEXT_TYPE + COMMA_SEP +
                    WifiKeysContract.WifiKeys.COLUMN_NAME_AUTH_TYPE + TEXT_TYPE + COMMA_SEP +
                    WifiKeysContract.WifiKeys.COLUMN_NAME_KEY + TEXT_TYPE +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + WifiKeysContract.WifiKeys.TABLE_NAME;

    public WifiKeysDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }
}
