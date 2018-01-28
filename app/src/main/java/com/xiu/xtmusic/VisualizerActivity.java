package com.xiu.xtmusic;

import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.xiu.customview.CustomVisualizer;
import com.xiu.utils.mApplication;

import java.util.Random;

public class VisualizerActivity extends AppCompatActivity {

    private CustomVisualizer customVisualizer;  //可视化
    private int r, g, b;  //颜色
    private boolean bg;  //背景颜色黑白

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //横屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //去掉标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉信息栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_visualizer);
    }


    //初始化可视化
    public void initVisualizer() {
        mApplication app = (mApplication) getApplicationContext();
        if (app.getMp() != null) {
            customVisualizer = findViewById(R.id.visualizer);
            //设置自定义颜色
            readColor();
            //设置可视化的采样率，10 - 256
            customVisualizer.setDensity(256);
            //绑定MediaPlayer
            customVisualizer.setPlayer(app.getMp());
            //设置长按监听
            customVisualizer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    randomColor();
                    return true;
                }
            });
        }
    }

    //随机颜色
    public void randomColor() {
        r = (int) Math.round(Math.random() * 255);
        g = (int) Math.round(Math.random() * 255);
        b = (int) Math.round(Math.random() * 255);
        customVisualizer.setColor(Color.rgb(r, g, b));
    }

    //保存颜色
    public void saveColor() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("visualizer_color_r", r);
        editor.putInt("visualizer_color_g", g);
        editor.putInt("visualizer_color_b", b);
        editor.commit();
    }

    //读取颜色
    public void readColor() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        r = pref.getInt("visualizer_color_r", 255);
        g = pref.getInt("visualizer_color_g", 255);
        b = pref.getInt("visualizer_color_b", 255);
        customVisualizer.setColor(Color.rgb(r, g, b));
    }

    //背景颜色
    public void bgColor(View view){
        LinearLayout layout = (LinearLayout) view.getParent();
        if(bg){
            bg = false;
            layout.setBackgroundColor(Color.parseColor("#000000"));
        }else {
            bg = true;
            layout.setBackgroundColor(Color.parseColor("#FFFFFF"));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initVisualizer();  //初始化可视化
    }

    @Override
    protected void onPause() {
        super.onPause();
        //释放资源并保存颜色
        if (customVisualizer != null) {
            saveColor();
            customVisualizer.release();
        }
    }
}
