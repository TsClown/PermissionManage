package com.tsclown.permissionmanage;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.tsclown.permission.PermissionCallback;
import com.tsclown.permission.PermissionManager;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.textView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request();
            }
        });
    }

    public void request(){
        PermissionManager permissionManager = new PermissionManager.Builder(this)
                .setDialogTitle("权限申请")
                .setPermissionArray(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
                .setPermissionNameCombine("存储")
                .setFirstRequestPermissionShowDialog(true)
                .setPermissionCallback(new PermissionCallback() {
                    @Override
                    public void onPermissionResult(boolean granted) {
                        Toast.makeText(MainActivity.this, "权限申请" + granted, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationale() {
                        Toast.makeText(MainActivity.this, "不再提醒", Toast.LENGTH_SHORT).show();
                    }
                }).create();

        permissionManager.request();
    }
}
