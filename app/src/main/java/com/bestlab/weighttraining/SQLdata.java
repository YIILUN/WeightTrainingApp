package com.bestlab.weighttraining;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class SQLdata extends SQLiteOpenHelper {
    private final static String DB = "DBHistory.db";
    private final static String TB = "TBHistory";
    private final static int vs = 1;

    public SQLdata(@Nullable Context context, @Nullable String name, @Nullable SQLiteDatabase.CursorFactory factory,int version){
        super(context,DB,null,vs);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String SQL="CREATE TABLE IF NOT EXISTS "+TB+"(_id INTEGER,_date VARCHAR(10),_nowTime VARCHAR(10),_name VARCHAR(10),_pose VARCHAR(10)," +
                                                    "_group VARCHAR(10),_times VARCHAR(10),_weight VARCHAR(10),_mac VARCHAR(10)," +
                                                    "_distanceIn VARCHAR(10),_timeIn VARCHAR(10),_powerIN VARCHAR(10)," +
                                                    "_distanceOut VARCHAR(10),_timeOut VARCHAR(10),_powerOut VARCHAR(10))";
        sqLiteDatabase.execSQL(SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        String SQL = "DROP TABLE'"+TB+"'";//DROP TABLE側除資料表
        sqLiteDatabase.execSQL(SQL);
    }
}
