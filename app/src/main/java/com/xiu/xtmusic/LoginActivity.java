package com.xiu.xtmusic;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.xiu.entity.User;
import com.xiu.utils.CallBack;
import com.xiu.utils.HttpUtils;
import com.xiu.utils.ServerURL;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText uacc, upwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        initView();  //初始化布局元素
    }

    //初始化布局元素
    public void initView(){
        uacc = findViewById(R.id.uacc);
        upwd = findViewById(R.id.upwd);
    }

    //数据校验
    public void dataTest(View view){
        String acc = uacc.getText().toString();
        String pwd = upwd.getText().toString();
        if(acc == null || pwd == null || acc.length() == 0 || pwd.length() == 0){
            Toast.makeText(this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show();
        }else {
            doLogin(acc, pwd);
        }
    }

    //登录
    public void doLogin(String acc, String pwd){
        String url = ServerURL.baseURL+"/"+ServerURL.login;
        String data = "uacc="+acc+"&upwd="+pwd;
        HttpUtils.doPost(url, data, new CallBack() {
            @Override
            public void success(String str) {
                User user = new Gson().fromJson(str, new TypeToken<User>(){}.getType());
                if(user != null && user.getUno() != 0){
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("user", user);
                    setResult(RESULT_OK, intent);
                    finish();
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, "账号或密码错误", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void failed(String str) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LoginActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    //返回
    public void doBack(View view){
        this.finish();
    }
}
