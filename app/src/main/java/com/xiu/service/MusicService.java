package com.xiu.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.utils.ImageUtil;
import com.xiu.utils.NetworkState;
import com.xiu.utils.mApplication;
import com.xiu.xtmusic.AlbumActivity;
import com.xiu.xtmusic.MainActivity;
import com.xiu.xtmusic.R;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by xiu on 2017/12/27.
 */

public class MusicService extends Service implements MediaPlayer.OnBufferingUpdateListener{

    private MusicDao dao;
    private boolean soonExit;
    private boolean timerCk;
    private TimerTask task;
    private long time;
    private Timer timer;
    private NotificationManager manager;
    private int mCurrentState = TelephonyManager.CALL_STATE_IDLE;
    private int mOldState = TelephonyManager.CALL_STATE_IDLE;
    private boolean interrupt;  //记录歌曲被打断的状态
    private mApplication app;
    private MediaPlayer mp;

    //广播接收
    BroadcastReceiver sBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //耳机拔出监听
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(action)) {
                if (intent.getIntExtra("state", 0) == 0) {
                    if (mp != null && mp.isPlaying()) {
                        mp.pause();
                        senRefresh();
                    }
                }
            } else {
                switch (intent.getIntExtra("what", 0)) {
                    case Msg.PLAY:  //播放
                        play();
                        break;
                    case Msg.PLAY_LAST:  //播放上一首
                        lastNum();
                        play();
                        break;
                    case Msg.PLAY_NEXT:  //播放下一首
                        nextNum();
                        play();
                        break;
                    case Msg.PLAY_PAUSE:  //播放&暂停
                        playPause();
                        break;
                    case Msg.NOTIFICATION_REFRESH:  //更新状态栏
                        musicNotification();
                        break;
                    case Msg.TIMER_EXIT:  //定时退出
                        time = intent.getIntExtra("time", 0);
                        timerCk = intent.getBooleanExtra("checked", false);
                        if (time != 0) {
                            Date date = new Date();
                            date.setTime(System.currentTimeMillis()+time);
                            time = date.getTime();
                            if(timer != null){
                                timer.cancel();
                                task.cancel();
                            }
                            timer = new Timer();
                            task = new Task();
                            timer.schedule(task, date);
                        }
                        break;
                    case Msg.TIMER_CLEAR:
                        if(timer != null){
                            timer.cancel();
                            task.cancel();
                            timerCk = false;
                            soonExit = false;
                            time = 0;
                            Toast.makeText(MusicService.this, "定时器已取消", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case Msg.PLAY_KUGOU_MUSIC:
                        app.setIdx(1);
                        play();
                        break;
                }
            }
        }
    };

    //暂停&恢复
    public void playPause() {
        if (mp == null) {
            return;
        } else if (mp.isPlaying()) {
            mp.pause();
        } else {
            mp.start();
        }
        musicNotification();
        senRefresh();
    }

    //hander定时发送广播
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == Msg.CURRENTTIME) {
                Intent broadcast = new Intent("sBroadcast");
                broadcast.putExtra("what", Msg.CURRENTTIME);
                broadcast.putExtra("current", mp.getCurrentPosition());
                if(timer != null){
                    broadcast.putExtra("time", time);
                }
                sendBroadcast(broadcast);
            }
        }
    };

    //播放
    public void play() {
        if(app.getIdx() == 0) return;
        Music music = app.getmList().get(app.getIdx() - 1);
        if((music.getPath().contains("http://") && !testNetwork()) && !new File(music.getPath()).exists()){
            nextNum();
            play();
            //Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mp == null)
            mp = new MediaPlayer();
        app.setMp(mp);
        try {
            mp.reset();
            mp.setDataSource(music.getPath());
            mp.prepareAsync();
            mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mp.start();
                    senRefresh();  //通知activity更新信息
                    musicNotification();  //更新状态栏信息
                }
            });
            mp.setOnBufferingUpdateListener(this);
            mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    if(soonExit){
                        exitApp();
                    }else {
                        nextNum();
                        senRefresh();
                        play();
                    }
                }
            });
            //addToHistory(music);  //将音乐添加到最近播放列表
            if(music.getPath().contains("http://")){
                Toast.makeText(this, "正在缓冲音乐", Toast.LENGTH_SHORT).show();
                senRefresh();  //通知activity更新信息
                musicNotification();  //更新状态栏信息
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "无法播放该歌曲", Toast.LENGTH_SHORT).show();
        }
    }



    //网络状态检测
    public boolean testNetwork(){
        //Log.d("net", NetworkState.GetNetype(this)+"");
        switch (NetworkState.GetNetype(this)){
            //返回值 -1：没有网络  1：WIFI网络2：wap网络3：net网络
            case -1:
                Toast.makeText(this, "当前没有网络连接", Toast.LENGTH_SHORT).show();
                return false;
            case 1:
                if(!NetworkState.isNetworkConnected(this)){
                    Toast.makeText(this, "网络连接不可用", Toast.LENGTH_SHORT).show();
                    return false;
                }else {
                    return true;
                }
            case 2:
            case 3:
                if(!NetworkState.isMobileConnected(this)){
                    Toast.makeText(this, "网络连接不可用", Toast.LENGTH_SHORT).show();
                    return false;
                }else if(!app.isMobileConnected()) {
                    app.setMobileConnected(true);
                    Toast.makeText(this, "当前正在使用移动网络播放，请注意您的流量哦！", Toast.LENGTH_LONG).show();
                    return true;
/*                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage("使用移动网络播放将会消耗大量流量，是否继续？");
                    builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            app.setMobileConnected(true);
                            dialogInterface.dismiss();
                            play();
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
                    dialog.show();*/
                }else if(app.isMobileConnected()){
                    return true;
                }
        }
        return false;
    }

    //将音乐添加到最近播放列表
    public void addToHistory(Music music){
        dao.addToHistory(music);
    }

    //通知activity更新信息
    public void senRefresh() {
        Intent mBroadcast = new Intent();
        mBroadcast.setAction("sBroadcast");
        mBroadcast.putExtra("what", Msg.PLAY_COMPLETION);
        sendBroadcast(mBroadcast);
    }

    //用于定时发送音乐播放进度
    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (mp != null && app.getIdx() != 0) {
                handler.sendEmptyMessage(Msg.CURRENTTIME);
            }
            handler.postDelayed(this, 100);
        }
    };

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        Intent aBroadcast = new Intent();
        aBroadcast.setAction("sBroadcast");
        aBroadcast.putExtra("what", Msg.BUFFERING_UPDATE);
        aBroadcast.putExtra("percent", i);
        sendBroadcast(aBroadcast);
    }

    //用于定时退出
    class Task extends TimerTask{
        @Override
        public void run() {
            if(timerCk){
                soonExit = true;
            }else {
                exitApp();
            }
        }
    };

    //计算下一首音乐编号
    public void nextNum() {
        if (app.getIdx() < app.getmList().size()) {
            app.setIdx(app.getIdx() + 1);
        } else {
            app.setIdx(1);
        }
    }

    //计算上一首音乐编号
    public void lastNum() {
        if (app.getIdx() > 1) {
            app.setIdx(app.getIdx() - 1);
        } else {
            app.setIdx(app.getmList().size());
        }
    }

    //通知栏通知
    public void musicNotification() {
        if (app.getIdx() == 0) return;
        Music music = app.getmList().get(app.getIdx() - 1);
        Notification notification = new Notification();
        notification.icon = R.mipmap.ic_launcher;
        notification.flags = Notification.FLAG_NO_CLEAR;
        //点击播放按钮发出的广播
        Intent broadcastPlay = new Intent("sBroadcast");
        broadcastPlay.putExtra("what", Msg.PLAY_PAUSE);
        PendingIntent contentIntents1 = PendingIntent.getBroadcast(this, 0, broadcastPlay, PendingIntent.FLAG_UPDATE_CURRENT);
        //点击下一首按钮发出的广播
        Intent broadcastNext = new Intent("sBroadcast");
        broadcastNext.putExtra("what", Msg.PLAY_NEXT);
        PendingIntent contentIntents2 = PendingIntent.getBroadcast(this, 1, broadcastNext, PendingIntent.FLAG_UPDATE_CURRENT);
        //为按钮绑定点击事件
        RemoteViews views = new RemoteViews(getPackageName(), R.layout.activity_notification);
        views.setOnClickPendingIntent(R.id.playBtn, contentIntents1);
        views.setOnClickPendingIntent(R.id.nextBtn, contentIntents2);
        //点击通知返回专辑界面
        Intent intent = new Intent(this, AlbumActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        //为通知绑定数据
        views.setTextViewText(R.id.title, music.getTitle());
        views.setTextViewText(R.id.singer, music.getArtist());
        Bitmap bitmap = app.getAlbumBitmap(this, music.getPath(), R.mipmap.ic_launcher);
        views.setImageViewBitmap(R.id.albumImg, ImageUtil.getimage(bitmap, 100f, 100f));
        //播放按钮的更新
        if (app.getMp() != null && app.getMp().isPlaying()) {
            views.setImageViewResource(R.id.playBtn, R.mipmap.pause_red);
        } else {
            views.setImageViewResource(R.id.playBtn, R.mipmap.play_red);
        }
        notification.contentView = views;
        notification.contentIntent = pendingIntent;
        manager.notify(1, notification);
    }

    //电话状态改变，进行暂停&恢复操作
    private PhoneStateListener myPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            super.onCallStateChanged(state, incomingNumber);
            mOldState = mCurrentState;
            switch (state) {
                case TelephonyManager.CALL_STATE_IDLE:
                    mCurrentState = TelephonyManager.CALL_STATE_IDLE;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    mCurrentState = TelephonyManager.CALL_STATE_OFFHOOK;
                    break;
                case TelephonyManager.CALL_STATE_RINGING:
                    mCurrentState = TelephonyManager.CALL_STATE_RINGING;
                    break;
            }

            int status = 0;
            if ((mOldState == TelephonyManager.CALL_STATE_IDLE || mOldState == TelephonyManager.CALL_STATE_RINGING) && mCurrentState == TelephonyManager.CALL_STATE_OFFHOOK) {
                //拨打&&接通
                status = Msg.CALL_IDLETOOFFHOOK;
            } else if (mCurrentState == TelephonyManager.CALL_STATE_RINGING) {
                //响铃
                status = Msg.CALL_RINGING;
            }
            callStateChanged(status);
        }

        //电话状态改变，控制暂停播放
        public void callStateChanged(int status) {
            //接听&响铃 -- 暂停
            if (status == Msg.CALL_IDLETOOFFHOOK || status == Msg.CALL_RINGING) {
                if (mp != null && mp.isPlaying()) {
                    interrupt = true;
                    mp.pause();
                }
                //挂断 -- 如果是被打断，则恢复播放，否则不进行操作
            } else {
                if (mp != null && interrupt) {
                    interrupt = false;
                    SystemClock.sleep(1000);
                    mp.start();
                }
            }
        }
    };

    //完全退出应用
    public void exitApp() {
        app.onTerminate();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        app = (mApplication) getApplicationContext();
        app.setmService(this);
        dao = new MusicDao(this);

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        filter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(sBroadcast, filter);

        //注册电话监听
        TelephonyManager tm = (TelephonyManager) getSystemService(Service.TELEPHONY_SERVICE);
        tm.listen(myPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);

        //获取服务
        manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        //定时刷新播放进度
        runnable.run();
    }

    //销毁时
    @Override
    public void onDestroy() {
        super.onDestroy();
        //注销广播
        try {
            unregisterReceiver(sBroadcast);
        } catch (Exception e) {
            Log.i("onDestroy", "sBroadcast已被清除");
        }
        //清除通知
        manager.cancel(1);
        //移除hander回调函数和消息
        handler.removeCallbacksAndMessages(null);
        //释放MediaPlay
        if (mp != null) {

            mp.reset();
            mp.release();
            mp = null;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
