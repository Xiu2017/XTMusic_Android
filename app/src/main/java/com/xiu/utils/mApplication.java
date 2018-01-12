package com.xiu.utils;

import android.app.Activity;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.j256.ormlite.stmt.QueryBuilder;
import com.xiu.dao.MusicDao;
import com.xiu.entity.Music;
import com.xiu.service.MusicService;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by xiu on 2017/12/27.
 */

public class mApplication extends Application {

    private MusicDao dao;
    private Intent sIntent;  //音乐服务
    private List<Music> mList;  //正在播放的列表
    private int idx;  //正在播放歌曲编号
    private MusicService mService;  //音乐服务
    private MediaPlayer mp;
    private boolean mobileConnected;  //是否使用移动网络播放

    /**
     * 获取音乐列表
     *
     * @param context 上下文
     */
    public List<Music> getMusicData(Context context) {
        //MusicDao dao = new MusicDao(context);
        if(mList == null){
            mList = new ArrayList<>();
        }else {
            mList.clear();
        }
        String[] columns = {  //要查询的列
                MediaStore.Audio.Media.TITLE,  //标题
                MediaStore.Audio.Media.ARTIST,  //歌手
                MediaStore.Audio.Media.ALBUM,  //专辑
                MediaStore.Audio.Media.DISPLAY_NAME,  //文件名
                MediaStore.Audio.Media.DURATION,  //时长
                MediaStore.Audio.Media.DATA,  //歌曲路径
                MediaStore.Audio.Media._ID , //编号
                MediaStore.Audio.Media.SIZE  //大小
        };
        //要筛选的格式
        String where = MediaStore.Audio.Media.DISPLAY_NAME + " like '%.mp3'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.flac'" +
                " or " + MediaStore.Audio.Media.DISPLAY_NAME + " like '%.m4a'";
        ContentResolver cr = context.getContentResolver();
        if (cr != null) {
            //获取所有歌曲
            Cursor cursor = cr.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    columns, where, null, MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED");
            //MediaStore.Audio.Media.TITLE + " COLLATE LOCALIZED"
            if (null == cursor) {
                return null;
            }

            if (cursor.moveToFirst()) {
                Music music;
                do {
                    String title = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE));
                    String artist = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                    if ("<unknown>".equals(artist)) {
                        artist = "未知";
                    }
                    String album = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
                    if (isRepeat(title, artist, album)) continue;    //去掉重复歌曲

                    String name = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                    //if(!(name.endsWith(".mp3") || name.endsWith(".flac"))) continue;  //跳过不支持的格式

                    int time = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION));
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                    int id = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID));
                    //Long albumId = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
                    long size = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Media.SIZE));

                    music = new Music();
                    music.setTitle(title);
                    music.setArtist(artist);
                    music.setAlbum(album);
                    music.setTime(time);
                    music.setPath(path);
                    music.setName(name);
                    music.set_id(id);
                    music.setSize(size);

                    //匹配父目录
                    String parentPath = path.replace("/" + name, "");
                    parentPath = parentPath.substring(parentPath.lastIndexOf("/") + 1);
                    music.setParentPath(parentPath);

                    mList.add(music);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        List<Music> list = dao.selMusicByDate();
        if(list == null){
            list = new ArrayList<>();
        }
        list.addAll(mList);
        Log.i("size", list.size()+"");
        mList = list;
        return  mList;
    }

    /**
     * 根据音乐名称、艺术家、唱片集来判断是否重复包含了
     *
     * @param title  名称
     * @param artist 艺术家
     * @param album  唱片集
     * @return 是否是重复歌曲
     */
    private boolean isRepeat(String title, String artist, String album) {
        for (Music music : mList) {
            if (title.equals(music.getTitle()) && artist.equals(music.getArtist()) && album.equals(music.getAlbum())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取Bitmap
     *
     * @param defaultRes 默认的图片资源
     * @return Bitmap
     */

    public Bitmap getAlbumBitmap(Context context, String url, int defaultRes) {
        if(url.contains("http://")) return BitmapFactory.decodeResource(context.getResources(), defaultRes);
        Bitmap bitmap = null;
        //能够获取多媒体文件元数据的类
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(url); //设置数据源
            byte[] embedPic = retriever.getEmbeddedPicture(); //得到字节型数据
            if (embedPic != null) {
                bitmap = BitmapFactory.decodeByteArray(embedPic, 0, embedPic.length); //转换为图片
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            retriever.release();
        }
        return bitmap == null ? BitmapFactory.decodeResource(context.getResources(), defaultRes) : bitmap;
    }

    public List<Music> getmList() {
        return mList;
    }

    public void setmList(List<Music> mList) {
        this.mList = mList;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public MusicService getmService() {
        return mService;
    }

    public void setmService(MusicService mService) {
        this.mService = mService;
    }

    public MediaPlayer getMp() {
        return mp;
    }

    public void setMp(MediaPlayer mp) {
        this.mp = mp;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 程序启动的时候开始服务
        dao = new MusicDao(this);
        sIntent = new Intent(this, MusicService.class);
        startService(sIntent);
    }

    //自定义释放资源
    public void onDestroy() {
        mService.onDestroy();  //释放服务资源
        stopService(sIntent);  //停止服务
    }

    /**
     * 自定义Activity栈
     */
    private List<Activity> activities = new ArrayList<Activity>();

    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        for (Activity activity : activities) {
            activity.finish();
        }

        onDestroy();

        System.exit(0);
    }

    public boolean isMobileConnected() {
        return mobileConnected;
    }

    public void setMobileConnected(boolean mobileConnected) {
        this.mobileConnected = mobileConnected;
    }
}
