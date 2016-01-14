package be.brunoparmentier.wifikeyshare.adapters;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import be.brunoparmentier.wifikeyshare.R;
import be.brunoparmentier.wifikeyshare.activities.WifiNetworkActivity;
import be.brunoparmentier.wifikeyshare.model.WifiNetwork;

public class WifiNetworkAdapter extends RecyclerView.Adapter<WifiNetworkAdapter.ViewHolder> {
    private static final String KEY_WIFI_NETWORK = "wifi_network";
    private static final String KEY_WIFI_NEEDS_PASSWORD = "wifi_needs_password";

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

        public ViewHolder(View itemView) {
            super(itemView);

            itemView.setOnClickListener(this);

            ssidTextView = (TextView) itemView.findViewById(R.id.wifi_ssid);
            authTypeTextView = (TextView) itemView.findViewById(R.id.wifi_auth_type);
        }

        @Override
        public void onClick(View view) {
            WifiNetwork wifiNetwork = wifiNetworks.get(getLayoutPosition());
            boolean needsUserInputPassword = (wifiNetwork.isPasswordProtected()
                    && wifiNetwork.getKey().isEmpty());
            Intent wifiIntent = new Intent(context, WifiNetworkActivity.class);
            wifiIntent.putExtra(KEY_WIFI_NETWORK, wifiNetwork);
            wifiIntent.putExtra(KEY_WIFI_NEEDS_PASSWORD, needsUserInputPassword);
            /*ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                    (Activity) context,
                    view.findViewById(R.id.wifi_ssid),
                    context.getString(R.string.transition_name_ssid));
            ActivityCompat.startActivity((Activity) context, wifiIntent, options.toBundle());*/
            context.startActivity(wifiIntent);
        }
    }
}
