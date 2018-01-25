package com.xiu.xtmusic;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;

import com.xiu.utils.CheckPermission;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.mApplication;

import java.io.File;
import java.io.IOException;

public class SplashActivity extends Activity {

    private mApplication app;
    private final int SPLASH_DISPLAY_LENGHT = 500;
    private Handler handler;

    //==========权限相关==========//
    private static final int REQUEST_CODE = 0;  //请求码
    private CheckPermission checkPermission;  //检测权限器

    //配置需要取的权限
    static final String[] PERMISSION = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,  // 写入权限
            Manifest.permission.READ_PHONE_STATE,  //电话状态读取权限
            Manifest.permission.INTERNET  //网络访问权限
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        //dao = new MusicDao(this);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
        handler = new Handler();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 延迟SPLASH_DISPLAY_LENGHT时间然后跳转到MainActivity
        //createNoMedia();
        //app.setmList(dao.getMusicData());
        if (checkPermission == null) {
            checkPermission = new CheckPermission(SplashActivity.this);
        }
        if (checkPermission.permissionSet(PERMISSION)) {
            startPermissionActivity();
            finish();
        } else {
            //缺少权限时，进入权限设置页面
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            }, SPLASH_DISPLAY_LENGHT);
        }
    }

    //创建.nomedia
    public void createNoMedia() {
        String innerSD = new StorageUtil(this).innerSDPath();
        String path = innerSD + "/XTMusic/albumImg/.nomedia";
        File file = new File(path);
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //禁用按键事件
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    //进入权限设置页面
    private void startPermissionActivity() {
        PermissionActivity.startActivityForResult(this, REQUEST_CODE, PERMISSION);
    }

    //返回结果回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //拒绝时，没有获取到主要权限，无法运行，关闭页面
        if (requestCode == REQUEST_CODE && resultCode == PermissionActivity.PERMISSION_DENIEG) {
            finish();
        }
    }
}
