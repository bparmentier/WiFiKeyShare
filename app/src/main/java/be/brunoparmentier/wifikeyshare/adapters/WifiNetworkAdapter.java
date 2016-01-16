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

package be.brunoparmentier.wifikeyshare.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.activities.WifiNetworkActivity;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

public class WifiNetworkAdapter extends RecyclerView.Adapter<WifiNetworkAdapter.ViewHolder> {

    private static final String KEY_WIFI_NETWORK = "wifi_network";
    private static final String KEY_NETWORK_ID = "network_id";
    private static final int PASSWORD_REQUEST = 1;

    private Context context;
    private List<WifiNetwork> wifiNetworks;

    public WifiNetworkAdapter(Context context, List<WifiNetwork> wifiNetworks) {
        this.context = context;
        this.wifiNetworks = wifiNetworks;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View wifiNetworkView = inflater.inflate(R.layout.item_wifi_network, parent, false);

        return new ViewHolder(wifiNetworkView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final WifiNetwork wifiNetwork = wifiNetworks.get(position);

        TextView ssidTextView = holder.ssidTextView;
        ssidTextView.setText(wifiNetwork.getSsid());

        TextView authTypeTextView = holder.authTypeTextView;
        authTypeTextView.setText(wifiNetwork.getAuthType().toString());

        ImageView keyImageView = holder.keyImageView;
        if (wifiNetwork.isPasswordProtected()) {
            if (wifiNetwork.needsPassword()) {
                keyImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_key_missing));
            } else {
                keyImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_key));
            }
        }
    }

    @Override
    public int getItemCount() {
        return wifiNetworks.size();
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public TextView ssidTextView;
        public TextView authTypeTextView;
        public ImageView keyImageView;

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            ssidTextView = (TextView) itemView.findViewById(R.id.wifi_ssid);
            authTypeTextView = (TextView) itemView.findViewById(R.id.wifi_auth_type);
            keyImageView = (ImageView) itemView.findViewById(R.id.wifi_key_icon);
        }

        @Override
        public void onClick(View view) {
            WifiNetwork wifiNetwork = wifiNetworks.get(getLayoutPosition());
            Intent wifiIntent = new Intent(context, WifiNetworkActivity.class);
            wifiIntent.putExtra(KEY_WIFI_NETWORK, wifiNetwork);
            if (wifiNetwork.needsPassword()) {
                wifiIntent.putExtra(KEY_NETWORK_ID, getLayoutPosition());
                ((Activity) context).startActivityForResult(wifiIntent, PASSWORD_REQUEST);
            } else {
                context.startActivity(wifiIntent);
            }
        }
    }
}
