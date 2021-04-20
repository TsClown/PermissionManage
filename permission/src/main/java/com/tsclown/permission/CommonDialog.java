package com.tsclown.permission;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public class CommonDialog extends Dialog {
    protected Context mContext;

    public CommonDialog(@NonNull Context context) {
        this(context, R.style.CommonDialog);
    }

    public CommonDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        mContext = context;
        init();
    }

    private void init() {
        setCancelable(true);
        setCanceledOnTouchOutside(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDialog();
    }

    protected void initDialog(){
        Window window = getWindow();
        if (window == null){
            return;
        }

        WindowManager.LayoutParams wmlp = window.getAttributes();
        wmlp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        wmlp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        wmlp.gravity = Gravity.CENTER;
        window.setAttributes(wmlp);
    }

    @Override
    public void setCancelable(boolean cancelable) {
        super.setCancelable(cancelable);
    }

    private void showImmersive(Activity activity){
        Window window = getWindow();
        if (window == null){
            return;
        }

        Window activityWindow = activity.getWindow();
        int systemUiVisibility = activityWindow.getDecorView().getSystemUiVisibility();
        if ((systemUiVisibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == View.SYSTEM_UI_FLAG_FULLSCREEN ||
                (systemUiVisibility & View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) == View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN){
            window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            window.getDecorView().setSystemUiVisibility(
                    activity.getWindow().getDecorView().getSystemUiVisibility());
            super.show();
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        }else {
            super.show();
        }
    }

    public void showImmersiveCheck(){
        if (!(mContext instanceof Activity)){
            super.show();
            return;
        }
        Activity activity = (Activity) mContext;
        showImmersive(activity);
    }
}
