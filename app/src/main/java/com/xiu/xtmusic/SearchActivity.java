package com.xiu.xtmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xiu.adapter.SearchListAdapter;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.entity.MusicList;
import com.xiu.utils.CallBack;
import com.xiu.utils.FileUtils;
import com.xiu.utils.KuGouMusic;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.mApplication;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener,
        TextView.OnEditorActionListener {

    private boolean isLoadlist;
    private boolean isLoadPath;
    private int page;
    private ProgressBar loadlist;
    private mApplication app;
    private MusicDao dao;
    private List<Music> list;
    private BaseAdapter adapter;
    private ListView seaList;
    private EditText keywork;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        app = (mApplication) getApplicationContext();
        app.addActivity(this);
        dao = new MusicDao(this);
        initStatusBar();
        initView();
        list = new ArrayList<>();
        adapter = new SearchListAdapter(list, SearchActivity.this);
        seaList.setAdapter(adapter);

        page = 1;

        seaList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {
                switch (i) {
                    case SCROLL_STATE_IDLE:
                        if (isListViewReachBottomEdge(absListView) && list.size() > 0 && !isLoadlist) {
                            isLoadlist = true;
                            loadlist.setVisibility(View.VISIBLE);
                            page++;
                            searchMusic(absListView);
                        }
                        break;
                }
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
            }
        });
    }

    //初始化布局元素
    private void initView() {
        seaList = findViewById(R.id.searchList);
        keywork = findViewById(R.id.keyword);
        loadlist = findViewById(R.id.loadlist);
        keywork.setOnEditorActionListener(this);
    }

    //初始化沉浸式状态栏
    private void initStatusBar() {
        Window win = getWindow();
        //KITKAT也能满足，只是SYSTEM_UI_FLAG_LIGHT_STATUS_BAR（状态栏字体颜色反转）只有在6.0才有效
        win.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);//透明状态栏
        // 状态栏字体设置为深色，SYSTEM_UI_FLAG_LIGHT_STATUS_BAR 为SDK23增加
        win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);

        // 部分机型的statusbar会有半透明的黑色背景
        win.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        win.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            win.setStatusBarColor(Color.TRANSPARENT);// SDK21
        }
    }


    //广播接收
    BroadcastReceiver seaBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra("what", 0)) {
                case Msg.SEARCH_RESULT:
                    final MusicList musicList = intent.getParcelableExtra("list");
                    if (musicList == null || musicList.getList() == null || musicList.getList().size() == 0) {
                        Toast.makeText(SearchActivity.this, "没有更多的数据了", Toast.LENGTH_SHORT).show();
                        loadlist.setVisibility(View.GONE);
                        return;
                    }
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            list.addAll(musicList.getList());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    adapter.notifyDataSetChanged();
                                    loadlist.setVisibility(View.GONE);
                                }
                            });
                        }
                    }).start();
                    isLoadlist = false;
                    break;
                case Msg.GET_MUSIC_PATH:
                    isLoadPath = false;
                    final Music music = intent.getParcelableExtra("music");
                    int idx = isExist(music);
                    dao.addToHistory(music);
                    app.getmList().clear();
                    app.setmList(dao.getMusicData());
                    Intent broadcast = new Intent();
                    broadcast.setAction("sBroadcast");
                    broadcast.putExtra("what", Msg.PLAY_KUGOU_MUSIC);
                    broadcast.putExtra("idx", idx);
                    context.sendBroadcast(broadcast);
                    adapter.notifyDataSetChanged();
                    saveLrcAlbum(music);
                    break;
                case Msg.GET_MUSIC_ERROR:
                    isLoadPath = false;
                    loadlist.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "拉取歌曲链接失败", Toast.LENGTH_SHORT).show();
                    break;
                case Msg.SEARCH_ERROR:
                    isLoadlist = false;
                    loadlist.setVisibility(View.GONE);
                    Toast.makeText(SearchActivity.this, "获取列表失败", Toast.LENGTH_SHORT).show();
                    break;
                case Msg.PLAY_COMPLETION:  //播放完成
                    //改变样式
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    };

    //保存歌词和专辑
    public void saveLrcAlbum(final Music music){
        //保存歌词
        new Thread(new Runnable() {
            @Override
            public void run() {
                String temp = getApplicationContext().getFilesDir().getAbsolutePath() + "/lyric/" + music.getName();
                final String lrcPath = temp.substring(0, temp.lastIndexOf(".")) + ".lrc";
                String lrc = music.getLyric();
                File file = new File(lrcPath);
                if(!file.exists() && lrc != null){
                    FileUtils.TextToFile(lrcPath, lrc);
                    Music m = music;
                    m.setLyric(null);
                    dao.addToHistory(m);
                }
            }
        }).start();

        //保存专辑
        new Thread(new Runnable() {
            @Override
            public void run() {
                String innerSDPath = new StorageUtil(SearchActivity.this).innerSDPath();
                String name = music.getName();
                final String toPath = innerSDPath + "/XTMusic/AlbumImg/" + name.substring(0, name.lastIndexOf(".")) + ".jpg";
                File file = new File(toPath);
                if (!file.exists() && music.getAlbumPath() != null) {
                    FileUtils.downLoadFile(music.getAlbumPath(), toPath, new CallBack() {
                        @Override
                        public void success(String str) {
                            //刷新通知栏
                            Intent intent = new Intent();
                            intent.setAction("sBroadcast");
                            intent.putExtra("what", Msg.NOTIFICATION_REFRESH);
                            sendBroadcast(intent);
                        }
                        @Override
                        public void failed(String str) {
                        }
                    });
                }
            }
        }).start();
    }

    //判断歌曲是否存在本地列表
    public int isExist(Music music) {
        if (app.getmList() == null || app.getmList().size() == 0) return 1;
        List<Music> list = app.getmList();
        for (int i = 0; i < list.size(); i++) {
            Music m = list.get(i);
            if (music.getName().equals(m.getName()) && music.getSize() == m.getSize()) {
                return i + 1;
            }
        }
        return 1;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.list_item:
                clickItem(view);
                break;
        }
    }

    //去到专辑界面
    public void openAlbum() {
        Intent intent = new Intent(this, AlbumActivity.class);
        startActivity(intent);
    }


    //搜索歌曲
    public void searchMusic(View view) {
        loadlist.setVisibility(View.VISIBLE);
        String str = keywork.getText().toString();
        if (str.replaceAll(" ", "").length() == 0) {
            loadlist.setVisibility(View.GONE);
            Toast.makeText(this, "请输入搜索内容", Toast.LENGTH_SHORT).show();
            return;
        }
        new KuGouMusic(this).search(str, page);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //处理并播放歌曲
    public void clickItem(View view) {
        if (!isLoadPath) {
            isLoadPath = true;
            new KuGouMusic(this).musicUrl(getMusicByNum(view));
        }
    }


    //根据音乐编号获取音乐
    public Music getMusicByNum(View view) {
        LinearLayout layout = (LinearLayout) view;
        TextView textView = layout.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        return list.get(musicNum - 1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(seaBroadcast, filter);

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //注销广播接收者
        unregisterReceiver(seaBroadcast);
    }

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_SEARCH || (keyEvent != null && keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
            page = 1;
            list.clear();
            searchMusic(textView);
            return true;
        }
        return false;
    }

    public boolean isListViewReachBottomEdge(final AbsListView listView) {
        boolean result = false;
        if (listView.getLastVisiblePosition() == (listView.getCount() - 1)) {
            final View bottomChildView = listView.getChildAt(listView.getLastVisiblePosition() - listView.getFirstVisiblePosition());
            result = (listView.getHeight() >= bottomChildView.getBottom());
        }
        return result;
    }
}
