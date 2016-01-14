package be.brunoparmentier.wifikeyshare;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.View;

public class AddWifiDialog extends AlertDialog {
    private View mView;

    protected AddWifiDialog(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mView = getLayoutInflater().inflate(R.layout.dialog_add_wifi, null);
        setView(mView);
        super.onCreate(savedInstanceState);
    }
}
