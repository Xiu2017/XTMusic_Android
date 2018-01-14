package com.xiu.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.stmt.QueryBuilder;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Music;
import com.xiu.service.MusicService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xiu on 2017/12/27.
 */

public class mApplication extends Application {

    private Intent sIntent;  //音乐服务
    private List<Music> mList;  //正在播放的列表
    private int idx;  //正在播放歌曲编号
    private MusicService mService;  //音乐服务
    private MediaPlayer mp;
    private boolean mobileConnected;  //是否使用移动网络播放

    public List<Music> getmList() {
        return mList;
    }

    public void setmList(List<Music> mList) {
        this.mList = mList;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public MusicService getmService() {
        return mService;
    }

    public void setmService(MusicService mService) {
        this.mService = mService;
    }

    public MediaPlayer getMp() {
        return mp;
    }

    public void setMp(MediaPlayer mp) {
        this.mp = mp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 程序启动的时候开始服务
        sIntent = new Intent(this, MusicService.class);
        startService(sIntent);
    }

    //自定义释放资源
    public void onDestroy() {
        mService.onDestroy();  //释放服务资源
        stopService(sIntent);  //停止服务
    }

    //自定义Activity栈
    private List<Activity> activities = new ArrayList<Activity>();

    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        for (Activity activity : activities) {
            activity.finish();
        }
        onDestroy();
        System.exit(0);
    }

    public boolean isMobileConnected() {
        return mobileConnected;
    }

    public void setMobileConnected(boolean mobileConnected) {
        this.mobileConnected = mobileConnected;
    }
}
