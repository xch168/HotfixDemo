package com.github.xch168.hotfixdemo;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView mTitleView;

    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitleView = findViewById(R.id.title);
        Title title = new Title();
        mTitleView.setText(title.getTitle());
    }

    public void loadPatch(View view) {

        dialog = new ProgressDialog(this);
        dialog.show();

        HotfixHelper.loadPatch(this, new HotfixHelper.OnPatchLoadListener() {

            @Override
            public void onSuccess() {
                dialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "加载成功", Toast.LENGTH_SHORT).show();
                    }
                });

            }

            @Override
            public void onFailure() {
                dialog.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });
    }
}
