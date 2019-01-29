package com.github.xch168.hotfixdemo;


import android.app.Application;
import android.content.Context;

public class App extends Application {

    @Override
    protected void attachBaseContext(Context base)
    {
        super.attachBaseContext(base);

        if (HotfixHelper.hasPatch(base)) {
            HotfixHelper.applyPatch(base);
        }
    }
}
