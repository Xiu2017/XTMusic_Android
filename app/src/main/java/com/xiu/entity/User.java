package com.xiu.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户
 * Created by xiu on 2017/11/9.
 */
public class User implements Serializable{

    private int uno;  //编号[主键]
    private String uacc;  //账户
    private String upwd;  //密码
    private String ucookie; //用户cookie数据
    private int ustatus = 1;  //用户状态：0停用  1启用

    public User() {
    }

    public User(int uno, String uacc, String upwd, String ucookie, int ustatus) {
        this.uno = uno;
        this.uacc = uacc;
        this.upwd = upwd;
        this.ucookie = ucookie;
        this.ustatus = ustatus;
    }

    public int getUno() {
        return uno;
    }

    public void setUno(int uno) {
        this.uno = uno;
    }

    public String getUacc() {
        return uacc;
    }

    public void setUacc(String uacc) {
        this.uacc = uacc;
    }

    public String getUpwd() {
        return upwd;
    }

    public void setUpwd(String upwd) {
        this.upwd = upwd;
    }

    public int getUstatus() {
        return ustatus;
    }

    public void setUstatus(int ustatus) {
        this.ustatus = ustatus;
    }

    public String getUcookie() {
        return ucookie;
    }

    public void setUcookie(String ucookie) {
        this.ucookie = ucookie;
    }
}
