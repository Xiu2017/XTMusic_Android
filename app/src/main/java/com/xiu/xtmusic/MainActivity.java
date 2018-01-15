package com.xiu.xtmusic;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaScannerConnection;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.webkit.MimeTypeMap;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.xiu.adapter.MusicListAdapter;
import com.xiu.dao.MusicDao;
import com.xiu.dialog.ItemMenuDialog;
import com.xiu.dialog.MusicInfoDialog;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.utils.AudioUtil;
import com.xiu.utils.CallBack;
import com.xiu.utils.FileUtils;
import com.xiu.utils.ImageUtil;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.TimeFormatUtil;
import com.xiu.utils.mApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    //全局变量
    private boolean isTimer;
    private boolean update;
    private MusicDao dao;
    private PopupMenu popup;
    private MenuItem timerItem;
    private Dialog dialog;
    private ContentResolver contentResolver;
    private MyObserver observer;
    private ProgressBar currentTime;
    private Intent sBroadcast;
    private TextView title, artist, musicSize, musicName;
    private ImageView playBtn, album, hunt, mainMenu;
    private mApplication app;
    private List<Music> list;
    private BaseAdapter adapter;
    private ListView musicList;
    private LinearLayout emptyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dao = new MusicDao(this);

        initStatusBar();  //初始化沉浸式状态栏
        initView();  //初始化布局元素
        initList();  //初始化列表
        initRegister();  //注册内容观察者,当媒体数据库发生变化时,更新音乐列表
    }

    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();

        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
        win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }

    //注册内容观察者
    public void initRegister() {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        observer = new MyObserver(new Handler());
        contentResolver = getContentResolver();
        contentResolver.registerContentObserver(uri, true, observer);
    }

    //初始化列表
    public void initList() {
        list = new ArrayList<Music>();
        adapter = new MusicListAdapter(list, MainActivity.this);
        musicList.setAdapter(adapter);
        musicList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case 1:
                        hunt.clearAnimation();
                        hunt.setVisibility(View.VISIBLE);
                        break;
                    case 0:
                        AlphaAnimation alphaAnimation = (AlphaAnimation) AnimationUtils.loadAnimation(MainActivity.this, R.anim.hunt_push_out);
                        hunt.startAnimation(alphaAnimation);
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });
    }

    //初始化布局元素
    public void initView() {
        playBtn = findViewById(R.id.playBtn);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        album = findViewById(R.id.album);
        mainMenu = findViewById(R.id.mainMenu);
        currentTime = findViewById(R.id.currentTime);
        musicList = findViewById(R.id.musicList);
        emptyList = findViewById(R.id.emptyList);
        hunt = findViewById(R.id.hunt);
        musicSize = findViewById(R.id.musicSize);
        musicName = findViewById(R.id.musicName);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
    }

    //构建PopupMenu菜单
    public void createMenu() {
        if (popup == null) {
            popup = new PopupMenu(this, mainMenu);
            popup.getMenuInflater().inflate(R.menu.menu, popup.getMenu());
            popup.setOnMenuItemClickListener(menuClick);
        }
        popup.show();
    }

    //菜单点击事件
    PopupMenu.OnMenuItemClickListener menuClick = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.menu_update:
                    Toast.makeText(MainActivity.this, "正在更新本地音乐", Toast.LENGTH_SHORT).show();
                    updateMediaRepertory();
                    break;
                case R.id.menu_timing:
                    timerItem = menuItem;
                    if (!isTimer) {
                        timerSelect();
                    } else {
                        timerClear();
                    }
                    break;
                case R.id.menu_about:
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intent);
                    break;
                case R.id.menu_exit:
                    exitApp();
                    break;
            }
            return true;
        }
    };

    //清除定时器
    public void timerClear() {
        isTimer = false;
        timerItem.setTitle("定时退出");
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.TIMER_CLEAR);
        sendBroadcast(sBroadcast);
    }

    //选择定时退出时间
    public void timerSelect() {
        //自定义控件
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        final View view = getLayoutInflater().inflate(R.layout.layout_time_dialog, null);
        final TimePicker timePicker = view.findViewById(R.id.time_picker);
        //初始化时间
        timePicker.setCurrentHour(0);
        timePicker.setCurrentMinute(20);
        timePicker.setIs24HourView(true);
        //设置time布局
        builder.setView(view);
        builder.setTitle("设置定时退出时间");
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                int mHour = timePicker.getCurrentHour();
                int mMinute = timePicker.getCurrentMinute();
                int time = (mHour * 60 + mMinute) * 60 * 1000;

                CheckBox timerCk = view.findViewById(R.id.timerCk);
                boolean checked = timerCk.isChecked();

                timerExit(time, checked);
                Toast.makeText(MainActivity.this, (mHour * 60 + mMinute) + "分钟后将退出应用", Toast.LENGTH_LONG).show();
                dialog.cancel();
            }
        });
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }

    //通知Service开启定时退出任务
    public void timerExit(int time, boolean checked) {
        isTimer = true;
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.TIMER_EXIT);
        sBroadcast.putExtra("time", time);
        sBroadcast.putExtra("checked", checked);
        sendBroadcast(sBroadcast);
    }

    //广播接收
    BroadcastReceiver mBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.PLAY_COMPLETION:  //播放完成
                    //改变样式
                    refresh();
                    break;
                case Msg.CURRENTTIME:  //刷新进度
                    currentTime(intent.getIntExtra("current", 0));
                    long time = intent.getLongExtra("time", 0);
                    if (time != 0) {
                        refreshTime(time);
                    }
                    break;
            }

            //下载进度
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) {
                DownloadManager.Query query = new DownloadManager.Query();
                //在广播中取出下载任务的id
                query.setFilterById(mTaskId);
                Cursor c = manager.query(query);
                if (c.moveToFirst()) {
                    Toast.makeText(MainActivity.this, "下载完成，音乐保存在/XTMusic/Music目录下", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    //刷新剩余退出时间
    public void refreshTime(long time) {
        time = time - System.currentTimeMillis();
        if (isTimer) {
            if (time > 0) {
                timerItem.setTitle("取消定时器（" + TimeFormatUtil.secToTime((int) (time / 1000)) + "）");
            }
        }
    }

    //点击事件
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.list_item:  //点击了item
                clickItem(view);
                break;
            case R.id.hunt:  //点击了定位歌曲
                musicList.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
                musicList.setSelection(app.getIdx() > 5 ? app.getIdx() - 5 : 0);
                break;
            case R.id.mainMenu:  //点击了菜单
                createMenu();
                break;
            case R.id.item_menu:  //点击了item菜单
                LinearLayout layout = (LinearLayout) view.getParent();
                TextView num = layout.findViewById(R.id.musicNum);
                Music music = getMusicByNum(view);
                dialog = new ItemMenuDialog(this, music, num.getText().toString());
                dialog.show();
                break;
            case R.id.menu_download:  //下载音乐
                dialog.dismiss();
                downloadMusic(getMusicByNum(view));
                break;
            case R.id.menu_share:  //分享音乐
                dialog.dismiss();
                shareMusic(getMusicByNum(view));
                break;
            case R.id.menu_ring:  //设为铃声
                dialog.dismiss();
                setRing(view);
                break;
            case R.id.menu_info:
                dialog.dismiss();
                dialog = new MusicInfoDialog(this, getMusicByNum(view));
                dialog.show();
                break;
            case R.id.menu_delete:  //删除歌曲
                dialog.dismiss();
                delMusic(getMusicByNum(view));
                break;
            case R.id.search:  //搜索在线歌曲
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                startActivity(intent);
                break;
        }
    }

    //下载音乐
    long mTaskId;

    public void downloadMusic(Music music) {
        //下载链接校验，防止用户操作过快
        if (!music.getPath().contains("http://")) return;

        Toast.makeText(this, "开始下载\"" + music.getArtist() + " - " + music.getTitle() + "\"，请在通知栏查看下载进度", Toast.LENGTH_SHORT).show();
        //创建下载任务,downloadUrl就是下载链接
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(music.getPath()));
        //指定下载路径和下载文件名
        request.setDestinationInExternalPublicDir("/XTMusic/Music", music.getArtist() + " - " + music.getTitle() + ".mp3");
        //获取下载管理器
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        //将下载任务加入下载队列，否则不会进行下载
        mTaskId = downloadManager.enqueue(request);

        registerReceiver(mBroadcast, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    //设置手机铃声
    public void setRing(View view) {
        Music ring = getMusicByNum(view);
        String fromPath = ring.getPath();
        String toPath = new StorageUtil(this).innerSDPath() + "/XTMusic/Ringtone";
        FileUtils.delAllFile(toPath);
        toPath = toPath + "/XTMusicRingtone" + System.currentTimeMillis();
        FileUtils.copyFile(fromPath, toPath);
        new AudioUtil(this).setRing(RingtoneManager.TYPE_RINGTONE, toPath, ring.getTitle());
    }

    //删除歌曲
    public void delMusic(final Music music) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (music.getPath().contains("http://")) {
            builder.setMessage("确定要移除歌曲吗？");
        } else {
            builder.setMessage("确定要将歌曲从本地删除吗？");
        }
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String extSD = new StorageUtil(MainActivity.this).extSDPath();
                boolean isUrl = music.getPath().contains("http://");
                File file = null;
                if (!isUrl) {
                    file = new File(music.getPath());
                    if (file.exists()) {
                        if (music.getPath().contains(extSD + "")) {
                            Toast.makeText(MainActivity.this, "暂不支持删除外置SD卡文件", Toast.LENGTH_SHORT).show();
                        } else {
                            file.delete();
                        }
                    }
                }
                int idx = app.getIdx() - 1;
                int delIdx = list.indexOf(music);
                list.clear();
                app.getmList().remove(music);
                list.addAll(app.getmList());
                if (delIdx == idx) {
                    app.setIdx(delIdx);
                    playNext();
                } else if (delIdx < idx) {
                    app.setIdx(idx);
                }

                String innerSDPath = new StorageUtil(MainActivity.this).innerSDPath();
                String name = music.getName();
                final String path = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
                file = new File(path);
                if (file.exists()) {
                    file.delete();
                }

                //从数据库和媒体库中删除
                dao.delMusicById(music);
                getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media.DATA + "= \"" + music.getPath() + "\"",
                        null);
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    //根据音乐编号获取音乐
    public Music getMusicByNum(View view) {
        LinearLayout layout1 = (LinearLayout) view.getParent();
        TextView textView = layout1.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        return app.getmList().get(musicNum - 1);
    }

    //分享音乐
    public void shareMusic(Music music) {
        File file = new File(music.getPath());
        if (!file.exists()) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        String msg = "分享音乐：" + music.getArtist() + " - " + music.getTitle() + "\n" + music.getPath();
        if (music.getPath().contains("http://")) {
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, msg);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "分享音乐链接"));
        } else {
            intent.setType("audio/*");
            Uri uri = Uri.fromFile(file);
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(Intent.createChooser(intent, "分享音乐文件"));
        }
    }

    //播放控制
    public void playControl(View view) {
        switch (view.getId()) {
            case R.id.playBtn:
                playPause(view);
                break;
            case R.id.playNext:
                playNext();
                break;
        }
    }

    //下一首
    public void playNext() {
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.PLAY_NEXT);
        sendBroadcast(sBroadcast);
    }

    //暂停&播放
    public void playPause(View view) {
        if (app.getmList() != null && app.getmList().size() != 0 && app.getMp() != null) {
            if (app.getMp().isPlaying()) {
                app.getMp().pause();
                playBtn.setImageResource(R.mipmap.play_red);
            } else {
                app.getMp().start();
                playBtn.setImageResource(R.mipmap.pause_red);
            }
        }
        sBroadcast = new Intent();
        sBroadcast.setAction("sBroadcast");
        sBroadcast.putExtra("what", Msg.NOTIFICATION_REFRESH);
        sendBroadcast(sBroadcast);
    }

    //点击了item
    public void clickItem(View view) {
        //获取要播放歌曲的编号
        TextView textView = view.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        sendPlay(musicNum);
    }

    //通知服务播放音乐
    public void sendPlay(int musicNum) {
        //如果点击的是正在播放的歌曲，直接去到专辑界面
        if (musicNum == app.getIdx()) {
            openAlbum(null);
            //否则播放歌曲
        } else {
            app.setIdx(musicNum);
            //通知服务播放音乐
            sBroadcast = new Intent();
            sBroadcast.setAction("sBroadcast");
            sBroadcast.putExtra("what", Msg.PLAY);
            sendBroadcast(sBroadcast);
        }
    }

    //改变样式
    Bitmap bitmap;

    public void refresh() {
        if (app.getmList() == null || app.getmList().size() == 0 || app.getIdx() == 0) {
            if (app.getmList() != null && app.getmList().size() == 0 && app.getMp() != null && app.getMp().isPlaying()) {
                app.getMp().pause();
                emptyList.setVisibility(View.VISIBLE);
                sBroadcast = new Intent();
                sBroadcast.setAction("sBroadcast");
                sBroadcast.putExtra("what", Msg.NOTIFICATION_REFRESH);
                sendBroadcast(sBroadcast);
            }
            title.setText("炫听音乐");
            artist.setText("炫酷生活");
            album.setImageResource(R.mipmap.ic_launcher);
            playBtn.setImageResource(R.mipmap.play_red);
            currentTime.setMax(0);
            currentTime.setProgress(0);
            adapter.notifyDataSetChanged();
            return;
        }
        final Music music = app.getmList().get(app.getIdx() - 1);
        title.setText(music.getTitle());
        artist.setText(music.getArtist());
        musicSize.setText(music.getSize() + "");
        musicName.setText(music.getName() + "");
        String innerSDPath = new StorageUtil(this).innerSDPath();
        String name = music.getName();
        final String toPath = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
        File file = new File(toPath);
        if (file.exists()) {
            bitmap = BitmapFactory.decodeFile(toPath);
            setAlbum(music.getPath(), bitmap);
        } else if (music.getAlbumPath() != null) {
            FileUtils.downLoadFile(music.getAlbumPath(), toPath, new CallBack() {
                @Override
                public void success(String str) {
                    bitmap = BitmapFactory.decodeFile(toPath);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAlbum(music.getPath(), bitmap);
                        }
                    });
                }

                @Override
                public void failed(String str) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAlbum(music.getPath(), null);
                        }
                    });
                }
            });
        } else {
            setAlbum(music.getPath(), null);
        }
        currentTime.setMax(music.getTime());

        //播放按钮的更新
        if (app.getMp() != null && app.getMp().isPlaying()) {
            playBtn.setImageResource(R.mipmap.pause_red);
        } else {
            playBtn.setImageResource(R.mipmap.play_red);
        }

        list.clear();
        list.addAll(app.getmList());
        adapter.notifyDataSetChanged();

        if (list.size() == 0)
            emptyList.setVisibility(View.VISIBLE);
        else
            emptyList.setVisibility(View.GONE);
    }

    //设置专辑封面
    public void setAlbum(String path, Bitmap bitmap) {
        if (bitmap != null) {
            album.setImageBitmap(ImageUtil.getimage(bitmap, 100f, 100f));
        } else {
            bitmap = dao.getAlbumBitmap(path, R.mipmap.ic_launcher);
            album.setImageBitmap(ImageUtil.getimage(bitmap, 100f, 100f));
        }
    }

    //重置当前播放歌曲编号
    public void resetNumber() {
        if (app.getIdx() == 0) return;
        for (int i = 0; i < list.size(); i++) {
            Music music = list.get(i);
            TextView name = findViewById(R.id.musicName);
            TextView size = findViewById(R.id.musicSize);
            if (name.getText().toString().equals(music.getName()) && size.getText().toString().equals(music.getSize() + "")) {
                app.setIdx(i + 1);
                break;
            }
        }
    }

    //去到专辑界面
    public void openAlbum(View view) {
        Intent intent = new Intent(this, AlbumActivity.class);
        startActivity(intent);
    }

    //更新列表
    public void updateList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //更新曲库
                        list.clear();
                        app.setmList(dao.getMusicData());
                        list.addAll(app.getmList());
                        resetNumber();
                        refresh();
                        musicList.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL, 0, 0, 0));
                        musicList.setSelection(app.getIdx() > 5 ? app.getIdx() - 5 : 0);

                        if (list.size() == 0)
                            emptyList.setVisibility(View.VISIBLE);
                        else
                            emptyList.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    //更新媒体库
    public void updateMediaRepertory() {
        if (update) {  //防止重复点击
            return;
        }
        update = true;
        StorageUtil util = new StorageUtil(this);
        String innerSD = util.innerSDPath();
        String extSD = util.extSDPath();
        String[] mimeTypes = new String[]{
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("mp3"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("flac"),
                MimeTypeMap.getSingleton().getMimeTypeFromExtension("m4a")
        };
        String[] path;
        if (extSD == null) {
            path = new String[]{
                    innerSD
            };
        } else {
            path = new String[]{
                    innerSD,
                    extSD
            };
        }
        MediaScannerConnection.scanFile(this, path, mimeTypes,
                new MediaScannerConnection.MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {
                    }

                    @Override
                    public void onScanCompleted(final String s, Uri uri) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                update = false;
                            }
                        });
                    }
                });
    }

    //更新时间进度
    public void currentTime(int current) {
        List<Music> list = app.getmList();
        if (list != null && list.size() > 0 && app.getIdx() > 0) {
            int time = list.get(app.getIdx() - 1).getTime();
            if (currentTime.getMax() != time) ;
            currentTime.setMax(time);
            currentTime.setProgress(Math.round(current));
        }
    }

    //让播放器后台运行
    @Override
    public void onBackPressed() {
        moveTaskToBack(false);
    }

    //完全退出应用
    public void exitApp() {
        //先返回上一层，然后启用线程丝滑的退出
        new Thread(new Runnable() {
            @Override
            public void run() {
                SystemClock.sleep(300);
                app.onTerminate();
            }
        }).start();
        onBackPressed();
    }

    //==========权限相关==========//
    @Override
    protected void onResume() {
        super.onResume();

        refreshList();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(mBroadcast, filter);

        //恢复布局
        refresh();
    }

    //同步最新列表
    public void refreshList() {
        List<Music> mList = app.getmList();
        if (mList == null || mList.size() == 0) {
            updateList();
        } else if (mList.size() != list.size()) {
            list.clear();
            list.addAll(mList);
            adapter.notifyDataSetChanged();
        } else {
            for (Music music : mList) {
                if (!music.getPath().contains("http://")) {
                    File file = new File(music.getPath());
                    if (!file.exists()) {
                        updateList();
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //注销广播接收者
        unregisterReceiver(mBroadcast);
    }

    //销毁时释放资源
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    //返回结果回调
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //拒绝时，没有获取到主要权限，无法运行，关闭页面
        if (resultCode == RESULT_OK && requestCode == Msg.WITER_EXTSD) {
            Uri treeUri = data.getData();
            if (treeUri != null) {
                //持久化权限
                getContentResolver().takePersistableUriPermission(treeUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                //保存treeUri
                SharedPreferences sharedPreferences = getSharedPreferences("xtmusic", Context.MODE_PRIVATE); //私有数据
                SharedPreferences.Editor editor = sharedPreferences.edit();//获取编辑器
                editor.putString("treeUri", treeUri.toString());
                editor.commit();
                Toast.makeText(MainActivity.this, "获取外置SD卡读写权限成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "获取权限失败", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //内容观察者,观察媒体数据库的变化,实时更新音乐列表
    class MyObserver extends ContentObserver {
        public MyObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            updateList();
        }
    }
}
