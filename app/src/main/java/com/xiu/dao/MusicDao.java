package com.xiu.dao;

import android.content.Context;
import android.util.Log;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.xiu.entity.Music;

import java.sql.SQLException;
import java.util.List;

/**
 * Created by xiu on 2017/12/15.
 */

public class MusicDao {

    private DbHelper dbHelper;
    private Dao dao;
    public MusicDao(Context context){
        dbHelper= DbHelper.getInstance(context);
        try {
            dao = dbHelper.getDao(Music.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //增
    public boolean addMusic(Music music){
        try {
            dao.create(music);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    //批量增
    public boolean addMusicList(List<Music> list){
        try {
            dao.create(list);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //查
    public List<Music> selMusic(){
        try {
            QueryBuilder builder = dao.queryBuilder();
            builder.orderBy("title", true);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据id查单个
    public Music selMusicById(int id){
        try {
            return (Music) dao.queryForId(id);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据条件查集合
    public List<Music> selMusicByName(String keyword){
        try {
            QueryBuilder builder = dao.queryBuilder();
            builder.where().like("name","%"+keyword+"%");
            builder.where().or().like("title", "%"+keyword+"%");
            builder.where().or().like("artist", "%"+keyword+"%");
            builder.where().or().like("album", "%"+keyword+"%");
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据文件夹查
    public List selMusicByFolder(){
        try {
            QueryBuilder builder = dao.queryBuilder();
            builder.groupBy("parentPath");
            //builder.orderBy("title", true);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //查询前50条播放记录
    public List selMusicTop50(){
        try {
            QueryBuilder builder = dao.queryBuilder();
            builder.limit((long) 50);
            builder.offset((long) 0);
            builder.orderBy("date", false);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据添加时间查询歌曲
    public List selMusicByDate(){
        try {
            QueryBuilder builder = dao.queryBuilder();
            builder.orderBy("date", false);
            return builder.query();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    //根据文件名和时长删除歌曲
    public void delMusicByNameAndTime(String name, long size){
        QueryBuilder builder = dao.queryBuilder();
        try {
            String str = name.replace("'", "''").replace("\"","\"\"");
            builder.where().eq("name", ""+str+"").and().eq("size", size);
            List list = builder.query();
            //if(list != null){
                //Log.d("size", list.size()+"");
                dao.delete(list);
            //}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //删除歌曲
    public boolean delMusicById(Music music){
        try {
            dao.delete(music);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //删除所有歌曲
    public boolean delMusic(){
        try {
            dao.delete(dao.queryForAll());
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //更新音乐信息
    public boolean updMusic(Music music){
        try {
            dao.update(music);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    //查询歌曲是否存在
    public boolean isExist(Music music){
        try{
            QueryBuilder builder = dao.queryBuilder();
            builder.where().like("name",music.getName().replace("'","''").replace("\"","\"\""));
            if(builder.query().size() > 0){
                return true;
            }
        }catch (SQLException e){
            e.printStackTrace();
        }
        return false;
    }

    //将音乐添加到播放记录
    public void addToHistory(Music music){
        music.setDate(System.currentTimeMillis());
        try {
            QueryBuilder builder = dao.queryBuilder();
            builder.where().like("path",music.getPath());
            List<Music> list = builder.query();
            if(list != null && list.size() > 0){
                Music m = list.get(0);
                music.setDate(System.currentTimeMillis());
                dao.update(music);
            }else {
                dao.create(music);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
