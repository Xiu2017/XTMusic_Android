package com.xiu.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xiu.entity.Music;
import com.xiu.utils.mApplication;
import com.xiu.xtmusic.MainActivity;
import com.xiu.xtmusic.R;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

/**
 * 音乐列表ListView适配器
 */

public class MusicListAdapter extends BaseAdapter {

    private List<Music> list;  //保存音乐数据的list
    private Context context;  //上下文
    private MainActivity activity;  //MainActivity实例
    private mApplication app;  //应用上下文

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

            //信息绑定
            Music music = list.get(i);
            String title = music.getTitle();

            musicItem.musicNum.setText(String.valueOf(i + 1));
            musicItem.musicTitle.setText(title);
            musicItem.musicArtist.setText(music.getArtist());

            musicItem.musicPath.setPadding(0, 0, 0, 0);

            musicItem.list_item = view.findViewById(R.id.list_item);

            if (music.getPath().contains("http://") || new File(music.getPath()).exists()) {
                if (music.getPath().contains("http://")) {
                    if (music.getPath().contains("qqmusic")) {
                        musicItem.kugou.setImageResource(R.mipmap.qqmusic);
                    } else {
                        musicItem.kugou.setImageResource(R.mipmap.kugou);
                    }
                    //显示大小
                    DecimalFormat df = new DecimalFormat("#0.00M");
                    float temp = music.getSize() / 1024.0f / 1024.0f;
                    //检查缓存
                    if (app.getProxy(context).isCached(music.getPath())) {
                        musicItem.musicPath.setText("已缓存");
                    } else {
                        musicItem.musicPath.setText(df.format(temp));
                    }
                } else {
                    musicItem.kugou.setImageResource(0);
                    musicItem.musicPath.setText("");
                }
                musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activity.clickItem(view);
                    }
                });
            } else {
                musicItem.kugou.setImageResource(R.mipmap.deleted);
                musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Toast.makeText(context, "文件已被删除", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            //让正在播放的歌曲显示播放图标
            if (app.getmList() != null && app.getmList().size() != 0 && app.getIdx() != 0 && app.getIdx() - 1 == i && music.getPath().equals(app.getmList().get(app.getIdx() - 1).getPath())) {
                musicItem.musicNum.setVisibility(View.GONE);
                musicItem.playing.setVisibility(View.VISIBLE);

                if (music.getPath().contains("http://") || new File(music.getPath()).exists()) {
                    musicItem.list_item.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            activity.openAlbum(view);
                        }
                    });
                }

            } else {
                musicItem.musicNum.setVisibility(View.VISIBLE);
                musicItem.playing.setVisibility(View.GONE);
            }

            return view;
        }
        return null;
    }

    class MusicItem {
        LinearLayout list_item;
        ImageView playing, kugou;
        TextView musicNum, musicTitle, musicArtist, musicPath;
    }
}
