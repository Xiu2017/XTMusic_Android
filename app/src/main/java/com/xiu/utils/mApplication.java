package com.xiu.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;

import com.chibde.visualizer.CircleBarVisualizer;
import com.chibde.visualizer.LineBarVisualizer;
import com.chibde.visualizer.LineVisualizer;
import com.danikula.videocache.HttpProxyCacheServer;
import com.xiu.customview.CustomVisualizer;
import com.xiu.entity.Music;
import com.xiu.service.MusicService;

import java.util.ArrayList;
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
    private HttpProxyCacheServer proxy;  //缓存
    private int playlist = 0;  //当前播放的列表
    private int playmode = 0;  //播放模式

    //缓存开源框架：https://github.com/danikula/AndroidVideoCache
    //获取Proxy
    public static HttpProxyCacheServer getProxy(Context context) {
        mApplication app = (mApplication) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    //Proxy设置
    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer.Builder(this)
                .maxCacheSize(1024 * 1024 * 512)  //512M 缓存
                .maxCacheFilesCount(100)  //最多缓存100首歌曲
                .build();
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

    public boolean isMobileConnected() {
        return mobileConnected;
    }

    public void setMobileConnected(boolean mobileConnected) {
        this.mobileConnected = mobileConnected;
    }

    public int getPlaylist() {
        return playlist;
    }

    public void setPlaylist(int playlist) {
        this.playlist = playlist;
    }

    public int getPlaymode() {
        return playmode;
    }

    public void setPlaymode(int playmode) {
        this.playmode = playmode;
    }
}
