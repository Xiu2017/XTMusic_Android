package com.xiu.dao;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.xiu.entity.Music;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by xiu on 2017/12/15.
 */

public class DbHelper extends OrmLiteSqliteOpenHelper {

    private static final String DATABASE_NAME = "xtmusic";
    private static final int VERSION = 1;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    public DbHelper(Context context, String databaseName, SQLiteDatabase.CursorFactory factory, int databaseVersion) {
        super(context, databaseName, factory, databaseVersion);
    }

    @Override
    public void onCreate(SQLiteDatabase database, ConnectionSource connectionSource) {
        try {
            TableUtils.createTableIfNotExists(connectionSource, Music.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            TableUtils.dropTable(connectionSource, Music.class, true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //===================提供单例模式DbHelper对象提供服务=========================//
    private static DbHelper dbHelper;

    public static synchronized DbHelper getInstance(Context context) {
        if (dbHelper == null) {
            dbHelper = new DbHelper(context);
        }
        return dbHelper;
    }

    //==================构建一个Dao栈，统一管理Dao=======================//
    private Map<String, Dao> daos = new HashMap<String, Dao>();

    public Dao getDaos(Class cls) throws SQLException {
        String className = cls.getName();//获取类名
        if (!daos.containsKey(className)) {
            daos.put(className, super.getDao(cls));
        }
        return daos.get(className);
    }

    //重写DbHelper的close方法
    @Override
    public void close() {
        super.close();
        Iterator it = daos.keySet().iterator();
        while (it.hasNext()) {
            Dao dao = (Dao) it.next();
            dao = null;
        }
    }
}
