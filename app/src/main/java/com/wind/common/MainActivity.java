package com.wind.common;

import android.app.Activity;
import android.os.Bundle;

import com.czhj.sdk.common.ClientMetadata;


public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        ClientMetadata.getInstance().initialize(this);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}