package com.xiu.adapter;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiu.entity.Music;
import com.xiu.utils.StorageUtil;
import com.xiu.utils.mApplication;
import com.xiu.xtmusic.MainActivity;
import com.xiu.xtmusic.R;
import com.xiu.xtmusic.SearchActivity;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

/**
 * Created by xiu on 2017/12/31.
 */

public class SearchListAdapter extends BaseAdapter {

    private List<Music> list;
    private Context context;
    private SearchActivity activity;
    private mApplication app;
    private String innerSD;
    private String extSD;

    public SearchListAdapter(List<Music> list, SearchActivity activity) {
        this.list = list;
        this.context = activity;
        this.activity = activity;
        this.app = (mApplication) activity.getApplicationContext();
        innerSD = new StorageUtil(context).innerSDPath();
        extSD = new StorageUtil(context).extSDPath();
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
                musicItem.list_item = view.findViewById(R.id.list_item);
                musicItem.item_menu = view.findViewById(R.id.item_menu);
                musicItem.musicNum = view.findViewById(R.id.musicNum);
                musicItem.playing = view.findViewById(R.id.playing);
                musicItem.musicTitle = view.findViewById(R.id.musicTitle);
                musicItem.musicArtist = view.findViewById(R.id.musicArtist);
                musicItem.musicPath = view.findViewById(R.id.musicPath);
                musicItem.kugou = view.findViewById(R.id.kugou);
                view.setTag(musicItem);
            } else {
                musicItem = (MusicItem) view.getTag();
            }

            musicItem.item_menu.setVisibility(View.GONE);

            //信息绑定
            final Music music = list.get(i);
            final String title = music.getTitle();
            musicItem.musicNum.setText((i + 1) + "");
            musicItem.musicTitle.setText(title);
            musicItem.musicArtist.setText(music.getArtist());

            //显示播放图标
            if (app.getmList() != null && app.getmList().size() != 0 && app.getIdx() != 0) {
                Music m = app.getmList().get(app.getIdx() - 1);
                if (music != null && music.getName() != null && m != null && m.getName() != null) {
                    if (m.getName().equals(music.getName() + "") && music.getSize() == m.getSize()) {
                        musicItem.musicNum.setVisibility(View.GONE);
                        musicItem.playing.setVisibility(View.VISIBLE);
                        musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                activity.openAlbum();
                            }
                        });
                    } else {
                        musicItem.musicNum.setVisibility(View.VISIBLE);
                        musicItem.playing.setVisibility(View.GONE);
                        musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                activity.clickItem(view);
                            }
                        });
                    }
                }
            }

            if (isExist(music)) {
                musicItem.kugou.setImageResource(R.mipmap.phone);
                if(music.getPath().contains(innerSD+"")){
                    musicItem.musicPath.setText(music.getPath().replace(innerSD+"","").replace("/"+music.getName(), ""));
                }else if (music.getPath().contains(extSD+"")){
                    musicItem.musicPath.setText(music.getPath().replace(extSD+"","").replace("/"+music.getName(), ""));
                }else {
                    musicItem.musicPath.setText("");
                }
            } else {
                musicItem.kugou.setImageResource(R.mipmap.kugou);
                //显示大小
                DecimalFormat df = new DecimalFormat("#0.00");
                float temp = music.getSize()/1024.0f/1024.0f;
                musicItem.musicPath.setText(df.format(temp)+"M");
            }

            return view;
        }
        return null;
    }

    final class MusicItem {
        LinearLayout item_menu, list_item;
        ImageView playing, kugou;
        TextView musicNum, musicTitle, musicArtist, musicPath;
    }

    //判断歌曲是否存在本地列表
    public boolean isExist(Music music) {
        if (app.getmList() == null || app.getmList().size() == 0) return false;
        List<Music> list = app.getmList();
        for (int i = 0; i < list.size(); i++) {
            Music m = list.get(i);
            if (!m.getPath().contains("http://") && music.getName().equals(m.getName()) && music.getSize() == m.getSize()) {
                return true;
            }
        }
        return false;
    }

}
