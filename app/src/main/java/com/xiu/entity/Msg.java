package com.xiu.entity;

/**
 * Created by xiu on 2017/12/15.
 */

public class Msg {

    public static final int SCAN_FINISH = 1001;  //扫描完成

    public static final int PLAY_COMPLETION = 1002;  //播放完成
    public static final int PLAY_LAST = 1012;  //上一首
    public static final int PLAY_PAUSE = 1003;  //暂停&恢复
    public static final int PLAY_NEXT = 1004;  //下一首
    public static final int PLAY = 1005;  //播放指定歌曲

    public static final int CURRENTTIME = 1006;  //歌曲进度

    public static final int CALL_STATECHANGED = 1007;  //电话状态改变
    public static final int CALL_IDLETOOFFHOOK = 1008;  //接听
    public static final int CALL_RINGING = 1010;  //响铃

    public static final int ALBUM_CURRENTTIME = 1011;  //专辑界面的歌曲进度
    public static final int ALBUM_GETMUSIC = 1013;  //专辑界面请求歌曲信息
    public static final int ALBUM_TIMECHANGE = 1014;  //专辑界面请求进度调节

    public static final int NOTIFICATION_REFRESH = 1015;  //刷新通知栏

    public static final int TIMER_EXIT = 1016;  //定时退出
    public static final int EXIT = 1017;  //退出
    public static final int TIMER_CLEAR = 1018;  //清除定时器

    public static final int WITER_EXTSD = 1019;  //外置SD卡写入权限

    public static final int BUFFERING_UPDATE = 1020;  //缓冲进度
    public static final int SEARCH_RESULT = 1021;  //搜索结果
    public static final int GET_MUSIC_PATH = 1022;  //获取到路径
    public static final int PLAY_KUGOU_MUSIC = 1023;  //无条件插队播放音乐

    public static final int RESULT_LOGIN = 2001;  //activity登录数据返回

    public static final int MUSIC_UPLOAD = 2002;  //歌曲上传
}
