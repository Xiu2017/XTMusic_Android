package com.xiu.xtmusic;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.xiu.adapter.LyricListAdapter;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.utils.CallBack;
import com.xiu.utils.DefaultLrcBuilder;
import com.xiu.utils.FileUtils;
import com.xiu.utils.ImageUtil;
import com.xiu.api.KuGouLrc;
import com.xiu.utils.LrcRow;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.TimeFormatUtil;
import com.xiu.utils.mApplication;
import com.xiu.utils.readTextUtil;

import net.qiujuer.genius.blur.StackBlur;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AlbumActivity extends AppCompatActivity implements View.OnClickListener {

    //全局变量
    private MusicDao dao;
    private List<LrcRow> rows;
    private ListView lrcList;
    private LyricListAdapter lyricListAdapter;
    private ObjectAnimator rotation;
    private LinearLayout dot1, dot2;
    private boolean sbChange;
    private SeekBar currentTime;
    private TextView title, artist, maxTime, minTime, noLrc;
    private Music music;
    private mApplication app;
    private ImageView album, albumbg, playBtn;
    private ViewPager viewPager;
    private List<View> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        initStatusBar();  //初始化沉浸式状态栏
        initViewPager();  //初始化viewPager
        initView();  //初始化视图和元素
        initAnim();  //初始化专辑旋转动画
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

    //初始化专辑旋转动画
    public void initAnim() {
        rotation = ObjectAnimator.ofFloat(album, "rotation", 0.0F, 359.9F);
        rotation.setRepeatCount(-1);
        rotation.setDuration(30000);
        rotation.setInterpolator(new LinearInterpolator());
    }

    //初始化视图和元素
    public void initView() {
        album = findViewById(R.id.album);
        albumbg = findViewById(R.id.albumbg);
        title = findViewById(R.id.title);
        artist = findViewById(R.id.artist);
        maxTime = findViewById(R.id.maxTime);
        minTime = findViewById(R.id.minTime);
        currentTime = findViewById(R.id.currentTime);
        playBtn = findViewById(R.id.playBtn);
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
        dao = new MusicDao(this);
    }

    //初始化viewPager
    public void initViewPager() {
        viewPager = findViewById(R.id.viewPager);
        //查找布局文件用LayoutInflater.inflate
        LayoutInflater inflater = getLayoutInflater();
        View view1 = inflater.inflate(R.layout.layout_album, null);
        View view2 = inflater.inflate(R.layout.layout_lyric, null);
        //将view装入数组
        pages = new ArrayList<View>();
        pages.add(view1);
        pages.add(view2);

        //绑定适配器
        viewPager.setAdapter(mPagerAdapter);
        viewPager.addOnPageChangeListener(mPageChange);

        lrcList = view2.findViewById(R.id.lyricList);
        noLrc = view2.findViewById(R.id.nolrc);
        //lrcList.setOnItemSelectedListener(mItemSelected);
        lrcList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        lrcList.setEnabled(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.back:
                this.finish();
                break;
            case R.id.share:  //分享音乐文件
                if (app.getmList() != null && app.getmList().size() != 0 && app.getIdx() != 0) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    String msg = "分享音乐：" + music.getArtist() + " - " + music.getTitle() + "\n" + music.getPath();
                    if (music.getPath().contains("http://")) {
                        intent.setType("text/plain");
                        intent.putExtra(Intent.EXTRA_TEXT, msg);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(intent, "分享音乐链接"));
                    } else {
                        intent.setType("audio/*");
                        File file = new File(music.getPath());
                        Uri uri = Uri.fromFile(file);
                        intent.putExtra(Intent.EXTRA_STREAM, uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(intent, "分享音乐文件"));
                    }
                }
                break;
        }
    }

    //广播接收
    BroadcastReceiver aBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.PLAY_COMPLETION:
                    //改变样式
                    refresh();
                    break;
                case Msg.CURRENTTIME:
                    int current = intent.getIntExtra("current", 0);
                    currentTime(current);
                    playLyric(current);
                    break;
                case Msg.BUFFERING_UPDATE:
                    bufferingUpdate(intent.getIntExtra("percent", 0));
                    break;
            }
        }
    };

    //更新缓冲进度
    public void bufferingUpdate(int percent) {
        if (music != null) {
            currentTime.setSecondaryProgress(music.getTime() / 100 * percent);
        }
    }

    //歌词滚动
    public void playLyric(int current) {
        if (rows == null || current == 0)
            return;
        for (int i = 0; i < rows.size(); i++) {
            LrcRow row = rows.get(i);
            if (lyricListAdapter != null && current <= row.time) {
                lyricListAdapter.setIdx(i);
                lyricListAdapter.notifyDataSetChanged();
                lrcList.smoothScrollToPosition(i);
                return;
            } else if (i == rows.size() - 1) {
                lyricListAdapter.setIdx(i + 1);
                lyricListAdapter.notifyDataSetChanged();
                lrcList.smoothScrollToPosition(i + 1);
                return;
            }
        }
    }

    //播放控制
    public void playControl(View view) {
        Intent intent = new Intent();
        intent.setAction("sBroadcast");
        switch (view.getId()) {
            case R.id.lastBtn:
                intent.putExtra("what", Msg.PLAY_LAST);
                sendBroadcast(intent);
                break;
            case R.id.playBtn:
                if (app.getmList() != null && app.getmList().size() != 0 && app.getMp() != null) {
                    if (app.getMp().isPlaying()) {
                        app.getMp().pause();
                        playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_play_sel));
                        albumRotate(STOP);
                    } else {
                        app.getMp().start();
                        playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_pause_sel));
                        albumRotate(START);
                    }
                    intent.putExtra("what", Msg.NOTIFICATION_REFRESH);
                    sendBroadcast(intent);
                }
                break;
            case R.id.nextBtn:
                intent.putExtra("what", Msg.PLAY_NEXT);
                sendBroadcast(intent);
                break;
        }
    }

    //初始化歌词
    public void readLyric() {
        //空处理
        if (app.getIdx() == 0) return;
        //提示用户查找歌词
        if (rows != null) {
            rows.clear();
            lyricListAdapter.notifyDataSetChanged();
        }
        String lrc;
        //应用程序保存的歌词路径
        String temp = getApplicationContext().getFilesDir().getAbsolutePath() + "/lyric/" + music.getName();
        final String lrcPath = temp.substring(0, temp.lastIndexOf(".")) + ".lrc";
        noLrc.setText("正在查找歌词...");
        noLrc.setVisibility(View.VISIBLE);
        //歌曲同目录下的歌词路径
        String musicPath = app.getmList().get(app.getIdx() - 1).getPath();
        musicPath = musicPath.substring(0, musicPath.lastIndexOf("."));
        String lyricPath = musicPath + ".lrc";
        //判断歌词文件是否存在
        File file = new File(lyricPath);
        if (!file.exists()) {
            file = new File(lrcPath);
        }
        final DefaultLrcBuilder builder = new DefaultLrcBuilder();
        if (file.exists()) {
            noLrc.setVisibility(View.GONE);
            lrc = readTextUtil.ReadTxtFile(file);
            //解析歌词返回LrcRow集合
            rows = builder.getLrcRows(lrc);
            lyricListAdapter = new LyricListAdapter(rows, AlbumActivity.this, -1);
            lrcList.setAdapter(lyricListAdapter);
            int padding = lrcList.getHeight() / 2;
            lrcList.setPadding(0, padding - 208, 0, padding);
        } else {
            KuGouLrc.searchLrc(music.getTitle(), music.getTime(), new CallBack() {
                @Override
                public void success(final String str) {
                    rows = builder.getLrcRows(str);
                    if (rows.size() == 0) {
                        failed(null);
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            noLrc.setVisibility(View.GONE);
                            lyricListAdapter = new LyricListAdapter(rows, AlbumActivity.this, -1);
                            lrcList.setAdapter(lyricListAdapter);
                            int padding = lrcList.getHeight() / 2;
                            lrcList.setPadding(0, padding - 208, 0, padding);
                        }
                    });
                    FileUtils.TextToFile(lrcPath, str);
                }

                @Override
                public void failed(String str) {
                    //Toast.makeText(AlbumActivity.this, str, Toast.LENGTH_SHORT).show();
                    //根据标题搜不出歌词，尝试根据文件名再搜一次
                    searchLrc(builder, lrcPath);
                }
            });
        }
    }

    //第二次查询歌词
    public void searchLrc(final DefaultLrcBuilder builder, final String lrcPath) {
        String name = music.getName();
        if (name == null) return;
        name = name.substring(0, name.lastIndexOf("."));
        KuGouLrc.searchLrc(name, music.getTime(), new CallBack() {
            @Override
            public void success(final String str) {
                rows = builder.getLrcRows(str);
                if (rows.size() == 0) {
                    failed(null);
                    return;
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        noLrc.setVisibility(View.GONE);
                        lyricListAdapter = new LyricListAdapter(rows, AlbumActivity.this, -1);
                        lrcList.setAdapter(lyricListAdapter);
                        int padding = lrcList.getHeight() / 2;
                        lrcList.setPadding(0, padding - 208, 0, padding);
                    }
                });
                FileUtils.TextToFile(lrcPath, str);
            }

            @Override
            public void failed(String str) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        noLrc.setText("找不到歌词");
                        noLrc.setVisibility(View.VISIBLE);
                        if (lyricListAdapter != null) {
                            rows.clear();
                            lyricListAdapter.notifyDataSetChanged();
                        }
                    }
                });
            }
        });

    }

    //专辑旋转动画控制
    private final static int STOP = 0;
    private final static int START = 1;

    public void albumRotate(int i) {
        ;
        if (i == 0) {
            if (rotation.isRunning() || rotation.isStarted())
                rotation.pause();
        } else {
            if (rotation.isPaused())
                rotation.resume();
            else
                rotation.start();
        }
    }

    //更新界面信息
    Bitmap bitmap = null;

    public void refresh() {
        //耗时操作，异步
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Music> list = app.getmList();
                if (list == null || list.size() == 0 || app.getIdx() == 0) {
                    albumDefault();
                    return;
                }
                music = list.get(app.getIdx() - 1);
                if (music == null) return;
                String innerSDPath = new StorageUtil(AlbumActivity.this).innerSDPath();
                String name = music.getName();
                final String toPath = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
                File file = new File(toPath);
                if (file.exists()) {
                    bitmap = BitmapFactory.decodeFile(toPath);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAlbum(bitmap);
                        }
                    });
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setAlbum(null);
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentTime.setMax(music.getTime());
                        title.setText(music.getTitle());
                        artist.setText(music.getArtist());
                        maxTime.setText(TimeFormatUtil.secToTime(music.getTime() / 1000) + "");
                        currentTime.setMax(music.getTime());
                        currentTime.setSecondaryProgress(music.getTime());

                        //播放按钮的更新
                        if (app.getMp() != null && app.getMp().isPlaying()) {
                            playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_pause_sel));
                            albumRotate(START);
                        } else {
                            playBtn.setImageDrawable(getResources().getDrawable(R.drawable.btm_play_sel));
                            albumRotate(STOP);
                        }
                        readLyric();
                    }
                });
            }
        }).start();
    }

    //设置专辑封面
    public void setAlbum(Bitmap bitmap) {
        if (bitmap != null) {
            album.setImageBitmap(ImageUtil.getimage(bitmap, 500f, 500f));
            bitmap = ImageUtil.getimage(bitmap, 20f, 20f);
            bitmap = StackBlur.blur(bitmap, 4, false);
            albumbg.setImageBitmap(bitmap);
        } else {
            bitmap = dao.getAlbumBitmap(music.getPath(), R.mipmap.album_default);
            album.setImageBitmap(ImageUtil.getimage(bitmap, 500f, 500f));
            bitmap = ImageUtil.getimage(bitmap, 20f, 20f);
            bitmap = StackBlur.blur(bitmap, 4, false);
            albumbg.setImageBitmap(bitmap);
        }
    }

    //设置默认专辑界面
    public void albumDefault() {
        //耗时操作，异步
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currentTime.setMax(0);
                        currentTime.setProgress(0);
                        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.album_default);
                        album.setImageBitmap(bitmap);
                        bitmap = ImageUtil.getimage(bitmap, 20f, 20f);
                        try {
                            bitmap = StackBlur.blur(bitmap, (int) 4, false);
                            albumbg.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }).start();
    }

    //更新时间进度
    public void currentTime(int current) {
        //如果进度条正在拖动，不执行更新
        if (!sbChange) {
            currentTime.setProgress(current);
        }
    }

    //监听seekbar改变
    SeekBar.OnSeekBarChangeListener mSeekBarChange = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (app.getMp() != null) {
                minTime.setText(TimeFormatUtil.secToTime(i / 1000) + "");
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            sbChange = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (app.getmList() != null && app.getmList().size() != 0 && app.getMp() != null) {
                app.getMp().seekTo(currentTime.getProgress());
                sbChange = false;
            }
        }
    };

    //监听viewpager改变
    ViewPager.OnPageChangeListener mPageChange = new ViewPager.OnPageChangeListener() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            int offset = Math.round(positionOffset * 30);
            LinearLayout layout = (LinearLayout) album.getParent();
            switch (position) {
                case 0:
                    //切换到歌词界面时让背景变暗
                    if (offset < 10) {
                        albumbg.setColorFilter(Color.parseColor("#0" + offset + "000000"), PorterDuff.Mode.DARKEN);
                    } else {
                        albumbg.setColorFilter(Color.parseColor("#" + offset + "000000"), PorterDuff.Mode.DARKEN);
                    }
                    //隐藏专辑
                    layout.setAlpha(1 - positionOffset);
                    break;
                case 1:
                    //显示专辑
                    layout.setAlpha(positionOffset);
                    break;
            }
        }

        @Override
        public void onPageSelected(int position) {
            switch (position) {
                case 0:
                    dot1.setBackgroundResource(R.drawable.circle_dot_sel);
                    dot2.setBackgroundResource(R.drawable.circle_dot);
                    break;
                case 1:
                    dot1.setBackgroundResource(R.drawable.circle_dot);
                    dot2.setBackgroundResource(R.drawable.circle_dot_sel);
                    break;
            }
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        currentTime.setOnSeekBarChangeListener(mSeekBarChange);

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(aBroadcast, filter);

        refresh();
    }

    //viewPager数据适配器
    PagerAdapter mPagerAdapter = new PagerAdapter() {
        @Override
        //获取当前窗体界面数
        public int getCount() {
            return pages.size();
        }

        @Override
        //断是否由对象生成界面
        public boolean isViewFromObject(View arg0, Object arg1) {
            return arg0 == arg1;
        }

        //从ViewGroup中移出当前View
        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView(pages.get(position));
            //super.destroyItem(container, position, object);
        }

        //返回一个对象，这个对象表明了PagerAdapter适配器选择哪个对象放在当前的ViewPager中
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(pages.get(position));
            return pages.get(position);
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(aBroadcast);
    }
}
