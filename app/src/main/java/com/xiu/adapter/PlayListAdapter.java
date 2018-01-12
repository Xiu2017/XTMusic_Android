package com.xiu.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xiu.entity.Music;
import com.xiu.utils.mApplication;
import com.xiu.xtmusic.R;

import java.util.List;

/**
 * Created by xiu on 2017/12/31.
 */

public class PlayListAdapter extends BaseAdapter{

    private List<Music> list;
    private Context context;
    private mApplication app;

    public PlayListAdapter(List<Music> list, Context context) {
        this.list = list;
        this.context = context;
        this.app = (mApplication) context.getApplicationContext();
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
                view = View.inflate(context, R.layout.layout_playlist_item, null);
                musicItem._id = view.findViewById(R.id._id);
                musicItem.playing = view.findViewById(R.id.playing);
                musicItem.title = view.findViewById(R.id.title);
                musicItem.artist = view.findViewById(R.id.artist);
                view.setTag(musicItem);
            } else {
                musicItem = (MusicItem) view.getTag();
            }

            //信息绑定
            final Music music = list.get(i);
            final String title = music.getTitle();
            musicItem._id.setText(music.get_id() + "");
            musicItem.title.setText(title);
            musicItem.artist.setText(" - "+music.getArtist());

            //解决Item回收导致图标状态显示不正确的问题
            if (app.getIdx() != 0 && app.getIdx()-1 == i) {
                musicItem.playing.setVisibility(View.VISIBLE);
                musicItem.title.setTextColor(context.getResources().getColor(R.color.colorPrimary));
                musicItem.artist.setTextColor(context.getResources().getColor(R.color.colorPrimary));
            } else {
                musicItem.playing.setVisibility(View.GONE);
                musicItem.title.setTextColor(context.getResources().getColor(R.color.list_title));
                musicItem.artist.setTextColor(context.getResources().getColor(R.color.colorText));
            }

            return view;
        }
        return null;
    }

    final class MusicItem {
        ImageView playing;
        TextView _id, title, artist;
    }
}
