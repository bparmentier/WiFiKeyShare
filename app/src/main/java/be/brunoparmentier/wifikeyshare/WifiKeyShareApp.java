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

package be.brunoparmentier.wifikeyshare;

import android.app.Application;

import org.wordpress.passcodelock.AppLockManager;

import be.brunoparmentier.wifikeyshare.activities.ConfirmConnectToWifiNetworkActivity;

public class WifiKeyShareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        AppLockManager.getInstance().enableDefaultAppLockIfAvailable(this);

        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            /* Disable lockscreen for ConfirmConnectToWifiNetworkActivity */
            AppLockManager.getInstance().getCurrentAppLock().setDisabledActivities(
                    new String[]{ ConfirmConnectToWifiNetworkActivity.class.getCanonicalName() });
        }
    }
}
