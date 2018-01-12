package com.xiu.xtmusic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.xiu.adapter.MusicListAdapter;
import com.xiu.adapter.SearchListAdapter;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.entity.MusicList;
import com.xiu.utils.KuGouMusic;
import com.xiu.utils.mApplication;

import java.lang.reflect.Method;
import java.util.List;

public class SearchActivity extends AppCompatActivity implements View.OnClickListener, TextView.OnEditorActionListener {

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
        dao = new MusicDao(this);
        seaList = findViewById(R.id.searchList);
        keywork = findViewById(R.id.keyword);
        keywork.setOnEditorActionListener(this);
        initStatusBar();
        initView();
    }

    //初始化布局元素
    private void initView() {
        keywork = findViewById(R.id.keyword);
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
                    MusicList musicList = intent.getParcelableExtra("list");
                    list = musicList.getList();
                    adapter = new SearchListAdapter(list, SearchActivity.this);
                    seaList.setAdapter(adapter);
                    break;
                case Msg.GET_MUSIC_PATH:
                    Music music = intent.getParcelableExtra("music");
                    Log.i("msg", music.getPath());
                    dao.addToHistory(music);
                    app.getMusicData(SearchActivity.this);
                    Intent broadcast = new Intent();
                    broadcast.setAction("sBroadcast");
                    broadcast.putExtra("what", Msg.PLAY_KUGOU_MUSIC);
                    context.sendBroadcast(broadcast);
                    break;
            }
        }
    };

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.searchBtn:
                searchMusic(view);
                break;
            case R.id.list_item:
                clickItem(view);
                break;
        }
    }

    //搜索歌曲
    public void searchMusic(View view){
        String str = keywork.getText().toString();
        if (str.length() == 0) {
            return;
        }
        new KuGouMusic(this).search(str);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //imm.hideSoftInputFromWindow(view, InputMethodManager.HIDE_NOT_ALWAYS);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    //处理并播放歌曲
    public void clickItem(View view){
        new KuGouMusic(this).musicUrl(getMusicByNum(view));
    }


    //根据音乐编号获取音乐
    public Music getMusicByNum(View view) {
        LinearLayout layout = (LinearLayout) view;
        TextView textView = layout.findViewById(R.id.musicNum);
        int musicNum = Integer.parseInt(textView.getText().toString());
        return list.get(musicNum-1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        //动态注册一个广播接收者
        IntentFilter filter = new IntentFilter();
        filter.addAction("sBroadcast");
        registerReceiver(seaBroadcast, filter);
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
            searchMusic(textView);
            return true;
        }
        return false;
    }
}
