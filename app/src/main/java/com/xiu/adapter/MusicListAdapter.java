package com.xiu.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiu.entity.Music;
import com.xiu.utils.TimeFormatUtil;
import com.xiu.utils.mApplication;
import com.xiu.xtmusic.MainActivity;
import com.xiu.xtmusic.R;

import java.io.File;
import java.util.List;

/**
 * Created by xiu on 2017/12/31.
 */

public class MusicListAdapter extends BaseAdapter{

    private List<Music> list;
    private Context context;
    private MainActivity activity;
    private mApplication app;

    public MusicListAdapter(List<Music> list, MainActivity activity) {
        this.list = list;
        this.context = activity;
        this.activity = activity;
        this.app = (mApplication) activity.getApplicationContext();
    }
    @Override
    public int getCount() {
        if (list != null) {
            return list.size();
        }
        return 0;
    }
    @Override
    public Object getItem(int i) {
        if (list != null) {
            return list.get(i);
        }
        return null;
    }
    @Override
    public long getItemId(int i) {
        if (list != null) {
            return i;
        }
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (list != null) {
            MusicItem musicItem;
            if (view == null) {
                musicItem = new MusicItem();
                view = View.inflate(context, R.layout.layout_list_item, null);
                musicItem._id = view.findViewById(R.id._id);
                musicItem.musicNum = view.findViewById(R.id.musicNum);
                musicItem.playing = view.findViewById(R.id.playing);
                musicItem.musicTitle = view.findViewById(R.id.musicTitle);
                musicItem.musicArtist = view.findViewById(R.id.musicArtist);
                //musicItem.musicTime = view.findViewById(R.id.musicTime);
                view.setTag(musicItem);
            } else {
                musicItem = (MusicItem) view.getTag();
            }

            //信息绑定
            final Music music = list.get(i);
            final String title = music.getTitle();
            musicItem._id.setText(music.get_id() + "");
            musicItem.musicNum.setText((i + 1) + "");
            musicItem.musicTitle.setText(title);
            musicItem.musicArtist.setText(music.getArtist());
            musicItem.list_item = view.findViewById(R.id.list_item);
            if(music.getPath().contains("http://") || new File(music.getPath()).exists()){
                musicItem.list_item.setAlpha(1);
                musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.clickItem(view);
                    }
                });
            }else {
                musicItem.list_item.setAlpha(0.3f);
                musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(context, "文件已被删除", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            //musicItem.musicTime.setText(TimeFormatUtil.secToTime(music.getTime() / 1000));

            //解决Item回收导致图标状态显示不正确的问题
            if (app.getIdx() != 0 && app.getIdx()-1 == i && music.getTitle() == app.getmList().get(app.getIdx()-1).getTitle()) {
                musicItem.musicNum.setVisibility(View.GONE);
                musicItem.playing.setVisibility(View.VISIBLE);
            } else {
                musicItem.musicNum.setVisibility(View.VISIBLE);
                musicItem.playing.setVisibility(View.GONE);
            }

            return view;
        }
        return null;
    }

    final class MusicItem {
        LinearLayout list_item;
        ImageView playing;
        TextView _id, musicNum, musicTitle, musicArtist; //musicTime;
    }
}
