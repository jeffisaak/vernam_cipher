package com.aptasystems.vernamcipher.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by jisaak on 2016-02-25.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "vernam.db";
    private static final int DB_VERSION = 1;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SecretKeyDatabase.SQL_CREATE);
        db.execSQL(MessageDatabase.SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Noop.
    }
}
