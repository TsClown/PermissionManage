package com.tsclown.permission;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.HashSet;
import java.util.Set;

import io.reactivex.functions.Consumer;

/**
 * PermissionManager permissionManager = new PermissionManager.Builder(this)
 *                 .setPermissionArray(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE
 *                         , Manifest.permission.WRITE_EXTERNAL_STORAGE
 *                         , Manifest.permission.RECORD_AUDIO})
 *                 .setPermissionNameCombine("存储、麦克风")
 *                 .setDialogTitle("帮助")
 *                 .setPermissionCallback(granted -> {
 *                     Log.e("PermissionCallback", "granted" + granted);
 *                 }).create();
 *         permissionManager.request();
 */
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

    private PermissionManager(Builder builder) {
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

    /**
     * 检查是否是第一次请求权限
     */
    @SuppressLint("CheckResult")
    public void request() {
        if (mPermissionsArray == null) {
            return;
        }
        boolean permissionIsFirstRequest = getPermissionIsFirstRequest();
        if (permissionIsFirstRequest){ //是第一次询问该权限
            requestPermissions();
        }else {
            final RxPermissions rxPermissions = new RxPermissions(mActivity);
            rxPermissions.shouldShowRequestPermissionRationale(mActivity, mPermissionsArray).subscribe(new Consumer<Boolean>() {
                @Override
                public void accept(Boolean aBoolean) throws Exception {
                    if (aBoolean) { //用户没有勾选了不再提醒
                        boolean isGranted = false;
                        for (int i = 0; i < mPermissionsArray.length; i++) {
                            isGranted = rxPermissions.isGranted(mPermissionsArray[i]);
                        }
                        if (!isGranted) {
                            PermissionManager.this.showPermissionNotGrantedDialog();
                        }
                    } else { //用户勾选了不再提醒
                        PermissionManager.this.showPermissionRationaleDialog();
                    }
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
        if (mPermissionNotGrantDialog == null){
            mPermissionNotGrantDialog = generateDefaultNotGrantDialog(getPermissionNotGrantDes(mPermissionNameCombine));
        }

        if (!mPermissionNotGrantDialog.isShowing()){
            mPermissionNotGrantDialog.show();
        }
    }

    public void showPermissionRationaleDialog(){
        if (mPermissionRationaleDialog == null){
            mPermissionRationaleDialog = generateDefaultRationaleDialog(getPermissionRationaleDes(mPermissionNameCombine));
        }

        if (!mPermissionRationaleDialog.isShowing()){
            mPermissionRationaleDialog.show();
        }
    }

    private AlertDialog generateDefaultRationaleDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mDialogTitle);
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton(mActivity.getString(R.string.permission_open), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mRationalClickListener != null) {
                    mRationalClickListener.onPositiveClick(dialog, which);
                } else {
                    PermissionManager.this.jumpToAppSetting();
                }
            }
        });
        builder.setNegativeButton(mActivity.getString(R.string.permission_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mRationalClickListener != null) {
                    mRationalClickListener.onNegativeClick(dialog, which);
                }
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    private AlertDialog generateDefaultNotGrantDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mDialogTitle);
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton(mActivity.getString(R.string.permission_allow), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mNotGrantClickListener != null) {
                    mNotGrantClickListener.onPositiveClick(dialog, which);
                } else {
                    PermissionManager.this.requestPermissions();
                }
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(mActivity.getString(R.string.permission_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (mNotGrantClickListener != null) {
                    mNotGrantClickListener.onNegativeClick(dialog, which);
                }
                dialog.dismiss();
            }
        });

        return builder.create();
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

    public static class Builder{
        //自定义dialog可不传
        private Dialog mPermissionRationaleDialog;
        private Dialog mPermissionNotGrantDialog;
        private OnDialogButtonClickListener mRationalClickListener;
        private OnDialogButtonClickListener mNotGrantClickListener;
        private String mDialogTitle;

        //必传
        private String[] mPermissionsArray;
        private String mPermissionNameCombine;
        private PermissionCallback mPermissionCallback;

        private FragmentActivity mActivity;
        private Fragment mFragment;

        public Builder(Fragment fragment){
            if (fragment == null){
                throw new IllegalArgumentException("Can only use lower 16 bits for requestCode");
            }else {
                mFragment = fragment;
                mActivity = fragment.getActivity();
            }
        }

        public Builder(FragmentActivity activity) {
            mActivity = activity;
        }

        public Builder setPermissionRationaleDialog(Dialog permissionRationaleDialog) {
            mPermissionRationaleDialog = permissionRationaleDialog;
            return this;
        }

        public Builder setPermissionNotGrantDialog(Dialog permissionNotGrantDialog) {
            mPermissionNotGrantDialog = permissionNotGrantDialog;
            return this;
        }

        public Builder setRationalClickListener(OnDialogButtonClickListener rationalClickListener) {
            mRationalClickListener = rationalClickListener;
            return this;
        }

        public Builder setNotGrantClickListener(OnDialogButtonClickListener notGrantClickListener) {
            mNotGrantClickListener = notGrantClickListener;
            return this;
        }

        public Builder setDialogTitle(String dialogTitle) {
            this.mDialogTitle = dialogTitle;
            return this;
        }

        public Builder setPermissionArray(String[] permissionsArray) {
            mPermissionsArray = permissionsArray;
            return this;
        }

        public Builder setPermissionNameCombine(String permissionNameCombine) {
            mPermissionNameCombine = permissionNameCombine;
            return this;
        }

        public Builder setPermissionCallback(PermissionCallback permissionCallback) {
            mPermissionCallback = permissionCallback;
            return this;
        }

        public PermissionManager create(){
            return new PermissionManager(this);
        }
    }

    public interface OnDialogButtonClickListener{
        void onPositiveClick(DialogInterface dialog, int which);
        void onNegativeClick(DialogInterface dialog, int which);
    }
}
