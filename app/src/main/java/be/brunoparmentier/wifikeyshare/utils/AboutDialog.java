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

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import be.brunoparmentier.wifikeyshare.BuildConfig;
import be.brunoparmentier.wifikeyshare.R;

/**
 * About dialog with HTML-formatted TextView and clickable links
 */
public class AboutDialog extends AlertDialog {

    private String aboutMessage;

    public AboutDialog(Context context) {
        super(context);
        setTitle(R.string.title_about);
        buildAboutMessage();
        setMessage(""); // will be set in @AboutDialog#show
        setButton(BUTTON_POSITIVE, getContext().getString(R.string.action_close), new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dismiss();
            }
        });
    }

    private void buildAboutMessage() {
        String appVersion = BuildConfig.VERSION_NAME;
        aboutMessage = String.format(getContext().getString(R.string.message_about_description), appVersion)
                + getContext().getString(R.string.message_about_license_title)
                + getContext().getString(R.string.message_about_license)
                + getContext().getString(R.string.message_about_credits_title)
                + getContext().getString(R.string.message_about_credits);
    }

    @Override
    public void show() {
        super.show();
        /* Get TextView from the original AlertDialog layout */
        TextView aboutContent = (TextView) findViewById(android.R.id.message);
        aboutContent.setText(Html.fromHtml(aboutMessage));
        aboutContent.setMovementMethod(LinkMovementMethod.getInstance()); // can click on links
    }
}
