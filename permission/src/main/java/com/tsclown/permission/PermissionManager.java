package com.tsclown.permission;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.functions.Consumer;

public class PermissionManager {

    private Dialog mPermissionRationaleDialog;
    private Dialog mPermissionNotGrantDialog;

    private OnDialogButtonClickListener mRationalClickListener;
    private OnDialogButtonClickListener mNotGrantClickListener;

    private FragmentActivity mActivity;
    private Fragment mFragment;

    private String mDialogTitle;

    private String[] mPermissionsArray;
    private String mPermissionNameCombine;
    private PermissionCallback mPermissionCallback;

    private boolean mIsFirstRequestPermission;
    private boolean mIsFirstRequestPermissionShowDialog;

    private PermissionManager(Builder builder) {
        mIsFirstRequestPermission = true;
        mIsFirstRequestPermissionShowDialog = builder.mIsFirstRequestPermissionShowDialog;

        //自定义dialog可不传
        mPermissionRationaleDialog = builder.mPermissionRationaleDialog;
        mPermissionNotGrantDialog = builder.mPermissionNotGrantDialog;
        mRationalClickListener = builder.mRationalClickListener;
        mNotGrantClickListener = builder.mNotGrantClickListener;
        mDialogTitle = builder.mDialogTitle;

        //必传
        mFragment = builder.mFragment;

        mActivity = builder.mActivity;
        mPermissionsArray = builder.mPermissionsArray;
        mPermissionNameCombine = builder.mPermissionNameCombine;
        mPermissionCallback = builder.mPermissionCallback;

        PreferencesHelper.init(mActivity);
    }

    public void setPermissionRationaleDialog(Dialog permissionRationaleDialog) {
        this.mPermissionRationaleDialog = permissionRationaleDialog;
    }

    public void setPermissionNotGrantDialog(Dialog permissionNotGrantDialog) {
        this.mPermissionNotGrantDialog = permissionNotGrantDialog;
    }

    /**
     * 检查是否是第一次请求权限
     */
    @SuppressLint("CheckResult")
    public void request() {
        if (mPermissionsArray == null) {
            return;
        }
        final boolean permissionIsFirstRequest = getPermissionIsFirstRequest();
        if (permissionIsFirstRequest){ //是第一次询问该权限
            mIsFirstRequestPermission = false;
            requestPermissions();
        }else {
            final RxPermissions rxPermissions;
            if (this.mFragment != null) {
                rxPermissions = new RxPermissions(this.mFragment);
            } else {
                rxPermissions = new RxPermissions(this.mActivity);
            }

            rxPermissions.shouldShowRequestPermissionRationale(mActivity, mPermissionsArray).subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean aBoolean) throws Exception {
                    if (aBoolean) { //用户没有勾选不再提醒
                        boolean isGranted = true;
                        for (int i = 0; i < mPermissionsArray.length; i++) {
                            if (!rxPermissions.isGranted(mPermissionsArray[i])) {
                                if (!mIsFirstRequestPermission || mIsFirstRequestPermissionShowDialog) {
                                    showPermissionNotGrantedDialog();
                                }
                                isGranted = false;
                                break;
                            }
                        }

                        if (mPermissionCallback != null){
                            if (isGranted){
                                mPermissionCallback.onPermissionResult(true);
                            }else{
                                mPermissionCallback.onPermissionResult(false);
                            }
                        }

                    } else if (mIsFirstRequestPermission && !mIsFirstRequestPermissionShowDialog) {
                        if (mPermissionCallback != null) {
                            mPermissionCallback.onPermissionRationale();
                        }
                    } else {
                        showPermissionRationaleDialog();
                        if (mPermissionCallback != null) {
                            mPermissionCallback.onPermissionRationale();
                        }
                    }
                    mIsFirstRequestPermission = false;
                }
            });
        }
    }

    @SuppressLint("CheckResult")
    private void requestPermissions() {
        if (mFragment != null){
            new RxPermissions(mFragment).request(mPermissionsArray)
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean granted) throws Exception {
                            if (mPermissionCallback != null) {
                                mPermissionCallback.onPermissionResult(granted);
                            }
                        }
                    });
        }else {
            new RxPermissions(mActivity).request(mPermissionsArray)
                    .subscribe(new Consumer<Boolean>() {
                        @Override
                        public void accept(Boolean granted) throws Exception {
                            if (mPermissionCallback != null) {
                                mPermissionCallback.onPermissionResult(granted);
                            }
                        }
                    });
        }

    }

    public void showPermissionNotGrantedDialog(){
        if (mPermissionNotGrantDialog == null) {
            initDefaultNotGrantDialog(getPermissionNotGrantDes(mPermissionNameCombine));
        }

        if (!mPermissionNotGrantDialog.isShowing()) {
            mPermissionNotGrantDialog.show();
        }
    }

    public void showPermissionRationaleDialog(){
        if (mPermissionRationaleDialog == null) {
            initDefaultRationaleDialog(getPermissionRationaleDes(mPermissionNameCombine));
        }

        if (!mPermissionRationaleDialog.isShowing()) {
            mPermissionRationaleDialog.show();
        }
    }

    @SuppressLint({"CutPasteId"})
    private void initDefaultRationaleDialog(String message) {
        mPermissionRationaleDialog = new CommonDialog(mActivity);
        mPermissionRationaleDialog.setContentView(R.layout.dialog_permission_rationale);
        TextView titleTextView = (TextView)mPermissionRationaleDialog.findViewById(R.id.rationaleTitleTextView);
        titleTextView.setText(mDialogTitle);
        TextView contentTextView = (TextView)mPermissionRationaleDialog.findViewById(R.id.rationaleContentTextView);
        contentTextView.setText(message);
        mPermissionRationaleDialog.setCancelable(false);
        Button allowButton = (Button)mPermissionRationaleDialog.findViewById(R.id.rationaleOpenButton);
        Button cancelButton = (Button)mPermissionRationaleDialog.findViewById(R.id.rationaleCancelButton);
        allowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRationalClickListener != null) {
                    mRationalClickListener.onPositiveClick(mPermissionRationaleDialog);
                } else {
                    PermissionUtil.jumpToAppSetting(mActivity);
                }

                mPermissionRationaleDialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRationalClickListener != null) {
                    mRationalClickListener.onNegativeClick(mPermissionRationaleDialog);
                }

                mPermissionRationaleDialog.dismiss();
            }
        });
    }

    @SuppressLint({"CutPasteId"})
    private void initDefaultNotGrantDialog(String message) {
        mPermissionNotGrantDialog = new CommonDialog(mActivity);
        mPermissionNotGrantDialog.setContentView(R.layout.dialog_permission_not_granted);
        TextView titleTextView = (TextView)mPermissionNotGrantDialog.findViewById(R.id.notGrantedTitleTextView);
        titleTextView.setText(mDialogTitle);
        TextView contentTextView = (TextView)mPermissionNotGrantDialog.findViewById(R.id.notGrantedContentTextView);
        contentTextView.setText(message);
        mPermissionNotGrantDialog.setCancelable(false);
        Button cancelButton = (Button)mPermissionNotGrantDialog.findViewById(R.id.notGrantedCancelButton);
        Button allowButton = (Button)mPermissionNotGrantDialog.findViewById(R.id.notGrantedAllowButton);
        allowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNotGrantClickListener != null) {
                    mNotGrantClickListener.onPositiveClick(mPermissionNotGrantDialog);
                } else {
                    requestPermissions();
                }

                mPermissionNotGrantDialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mNotGrantClickListener != null) {
                    mNotGrantClickListener.onNegativeClick(mPermissionNotGrantDialog);
                }

                mPermissionNotGrantDialog.dismiss();
            }
        });
    }

    /**
     * set.add()true表示元素添加到了Set中，false表示集合中已存在相等的元素,即非第一次请求权限。
     */
    public boolean getPermissionIsFirstRequest() {
        if (mPermissionsArray == null){
            return false;
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < mPermissionsArray.length; i++) {
            sb.append(mPermissionsArray[i]);
        }

        Set<String> stringSet = PreferencesHelper.getStringSet(PreferencesHelper.SP_KEY_PERMISSION_IS_FIRST_REQUEST);
        if (stringSet == null){
            stringSet = new HashSet<>();
        }
        boolean isAdded = stringSet.add(sb.toString());
        PreferencesHelper.putStringSet(PreferencesHelper.SP_KEY_PERMISSION_IS_FIRST_REQUEST, stringSet);
        return isAdded;
    }

    /**
     * 用户点了禁止后不再显示
     * @param permissionNameCombine 权限名字组合，类似于 相机、麦克风
     */
    private String getPermissionRationaleDes(String permissionNameCombine) {
        if (TextUtils.isEmpty(permissionNameCombine)) {
            return "";
        }
        return mActivity.getResources().getString(R.string.permission_not_rationale_hint, permissionNameCombine);
    }

    /**
     * 用户拒绝了权限
     * @param permissionNameCombine 权限名字组合，类似于 相机、麦克风
     */
    private String getPermissionNotGrantDes(String permissionNameCombine) {
        if (TextUtils.isEmpty(permissionNameCombine)) {
            return "";
        }
        return mActivity.getResources().getString(R.string.permission_not_granted_hint, permissionNameCombine);
    }

    private void jumpToAppSetting() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Uri uri = Uri.fromParts("package", mActivity.getPackageName(), null);
        intent.setData(uri);
        mActivity.startActivity(intent);
    }

    public static class Builder {
        private Dialog mPermissionRationaleDialog;
        private Dialog mPermissionNotGrantDialog;
        private PermissionManager.OnDialogButtonClickListener mRationalClickListener;
        private PermissionManager.OnDialogButtonClickListener mNotGrantClickListener;
        private String mDialogTitle;
        private String mPermissionNameCombine;
        private boolean mIsFirstRequestPermissionShowDialog = true;
        private String[] mPermissionsArray;
        private PermissionCallback mPermissionCallback;
        private FragmentActivity mActivity;
        private Fragment mFragment;

        public Builder(Fragment fragment) {
            if (fragment == null) {
                throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
            } else {
                this.mFragment = fragment;
                this.mActivity = fragment.getActivity();
            }
        }

        public Builder(FragmentActivity activity) {
            this.mActivity = activity;
        }

        public PermissionManager.Builder setPermissionRationaleDialog(Dialog permissionRationaleDialog) {
            this.mPermissionRationaleDialog = permissionRationaleDialog;
            return this;
        }

        public PermissionManager.Builder setPermissionNotGrantDialog(Dialog permissionNotGrantDialog) {
            this.mPermissionNotGrantDialog = permissionNotGrantDialog;
            return this;
        }

        public PermissionManager.Builder setRationalClickListener(PermissionManager.OnDialogButtonClickListener rationalClickListener) {
            this.mRationalClickListener = rationalClickListener;
            return this;
        }

        public PermissionManager.Builder setNotGrantClickListener(PermissionManager.OnDialogButtonClickListener notGrantClickListener) {
            this.mNotGrantClickListener = notGrantClickListener;
            return this;
        }

        public PermissionManager.Builder setDialogTitle(String dialogTitle) {
            this.mDialogTitle = dialogTitle;
            return this;
        }

        public PermissionManager.Builder setPermissionArray(String[] permissionsArray) {
            this.mPermissionsArray = permissionsArray;
            return this;
        }

        public PermissionManager.Builder setPermissionNameCombine(String permissionNameCombine) {
            this.mPermissionNameCombine = permissionNameCombine;
            return this;
        }

        public PermissionManager.Builder setPermissionCallback(PermissionCallback permissionCallback) {
            this.mPermissionCallback = permissionCallback;
            return this;
        }

        public PermissionManager.Builder setFirstRequestPermissionShowDialog(boolean firstRequestPermissionShowDialog) {
            this.mIsFirstRequestPermissionShowDialog = firstRequestPermissionShowDialog;
            return this;
        }

        public PermissionManager create() {
            return new PermissionManager(this);
        }
    }

    public interface OnDialogButtonClickListener{
        void onPositiveClick(DialogInterface dialog);
        void onNegativeClick(DialogInterface dialog);
    }
}
