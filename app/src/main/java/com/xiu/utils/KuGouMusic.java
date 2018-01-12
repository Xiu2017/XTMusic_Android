package com.xiu.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.xiu.entity.Msg;
import com.xiu.entity.Music;
import com.xiu.entity.MusicList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by xiu on 2018/1/11.
 */

public class KuGouMusic{
    private static Context context;

    public KuGouMusic(Context context) {
        this.context = context;
    }

    //查询列表
    //http://mobilecdn.kugou.com/api/v3/search/song?format=jsonp&keyword=搜索内容&page=1&pagesize=10&showtype=1&callback=kgJSONP238513750
    public void search(String keywork) {
        final MusicList musicList = new MusicList();
        final List<Music> list = new ArrayList<>();
        String searchUrl = "http://mobilecdn.kugou.com/api/v3/search/song?format=jsonp&keyword=" + keywork +
                "&page=1&pagesize=30&showtype=1";//&callback=kgJSONP238513750";

        //构建一个请求对象
        Request request = new Request.Builder().url(searchUrl).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient().newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("call error", "出错," + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //通过response得到服务器响应内容
                String str = response.body().string();
                str = str.substring(1, str.length()-1);
                try {
                    JSONArray json = new JSONObject(str)
                            .getJSONObject("data")
                            .getJSONArray("info");
                    for(int i = 0; i < json.length(); i++){
                        JSONObject obj = json.getJSONObject(i);
                        Music music = new Music();
                        music.setSize(obj.getLong("filesize"));
                        music.setTitle(obj.getString("songname_original"));
                        music.setArtist(obj.getString("singername"));
                        music.setPath(obj.getString("hash"));
                        //52Log.d("hash", music.getPath());
                        list.add(music);
                    }

                    musicList.setList(list);

                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("sBroadcast");
                    kBroadcast.putExtra("what", Msg.SEARCH_RESULT);
                    kBroadcast.putExtra("list", musicList);
                    context.sendBroadcast(kBroadcast);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.d("call result", str);
            }
        });
    }

    //获取歌曲链接
    //http://m.kugou.com/app/i/getSongInfo.php?hash=2b616f6ab9f8655210fd823b900085cc&cmd=playInfo
    public void musicUrl(final Music music){
        String hash = music.getPath();
        String url = "http://m.kugou.com/app/i/getSongInfo.php?hash="+hash+"&cmd=playInfo";
        //构建一个请求对象
        Request request = new Request.Builder().url(url).build();
        //构建一个Call对象
        okhttp3.Call call = new OkHttpClient().newCall(request);
        //异步执行请求
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("call error", "出错," + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //通过response得到服务器响应内容
                String str = response.body().string();
                Log.d("str", str);
                try {
                    JSONObject json = new JSONObject(str);
                    Log.i("json", json.toString());
                    music.setPath(json.getString("url"));
                    music.setTime(json.getInt("timeLength")*1000);
                    music.setName(music.getArtist()+" - "+music.getTitle()+".mp3");
                    music.setAlbum("未知");

                    Intent kBroadcast = new Intent();
                    kBroadcast.setAction("sBroadcast");
                    kBroadcast.putExtra("what", Msg.GET_MUSIC_PATH);
                    kBroadcast.putExtra("music", music);
                    context.sendBroadcast(kBroadcast);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
