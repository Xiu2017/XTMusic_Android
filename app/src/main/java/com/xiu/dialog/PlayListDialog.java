package com.xiu.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.xiu.adapter.PlayListAdapter;
import com.xiu.entity.Music;
import com.xiu.utils.mApplication;
import com.xiu.xtmusic.R;

import java.util.List;

/**
 * Created by xiu on 2017/12/31.
 */

public class PlayListDialog extends Dialog{

    private Context context;
    private List<Music> list;
    private mApplication app;

    public PlayListDialog(@NonNull Context context, List<Music> list, mApplication app) {
        super(context, R.style.CustomDialog);
        this.context = context;
        this.list = list;
        this.app = app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_play_list);
    }

    @Override
    public void show() {
        super.show();
        /**
         * 设置宽度全屏，要设置在show的后面
         */
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        layoutParams.gravity = Gravity.BOTTOM;
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;

        getWindow().getDecorView().setPadding(0, 0, 0, 0);
        getWindow().setAttributes(layoutParams);

        //为listview绑定数据
        ListView playList = getWindow().getDecorView().findViewById(R.id.pList);
        PlayListAdapter playListAdapter = new PlayListAdapter(list, getContext());
        playList.setAdapter(playListAdapter);
        //定位到正在播放的歌曲
        playList.setSelection(app.getIdx()-4);
    }

    //下滑关闭dialog
    private float startY;
    private float moveY = 0;
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        View view = getWindow().getDecorView();
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                moveY = ev.getY() - startY;
                view.scrollBy(0, -(int) moveY);
                startY = ev.getY();
                if (view.getScrollY() > 0) {
                    view.scrollTo(0, 0);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (view.getScrollY() < -this.getWindow().getAttributes().height / 4 && moveY > 0) {
                    this.dismiss();
                }
                view.scrollTo(0, 0);
                break;
        }
        return super.onTouchEvent(ev);
    }
}
