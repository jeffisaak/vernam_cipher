package com.aptasystems.vernamcipher.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.aptasystems.vernamcipher.model.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jisaak on 2016-02-25.
 */
public class MessageDatabase extends AbstractDatabase {

    private static MessageDatabase _instance;

    public static MessageDatabase getInstance(Context context) {
        if (_instance == null) {
            _instance = new MessageDatabase(context);
        }
        return _instance;
    }

    private static final String TABLE = "message";
    private static final String ID = "_id";
    private static final String CONTENT = "content";

    private static final String[] ALL_COLUMNS = new String[]{ID, CONTENT};

    public static final String SQL_CREATE = buildSql(
            CREATE, TABLE, WITH_COLUMNS,
            ID, INTEGER, PRIMARY_KEY, AUTO_INCREMENT, NEXT_COLUMN,
            CONTENT, TEXT, NOT_NULL, DONE_COLUMNS);

    private MessageDatabase(Context context) {
        _context = context;
    }

    public long insert(String content) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(CONTENT, content);
        long rid = db.insert(TABLE, null, cv);

        db.close();

        return rid;
    }

    public Message fetch(long id) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE,
                ALL_COLUMNS,
                ID + " = ?",
                new String[]{String.valueOf(id)},
                null,
                null,
                ID + " ASC");

        Message result = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                result = entityFromCursor(cursor);
            }
            cursor.close();
        }

        db.close();

        return result;
    }

    public List<Message> list() {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE,
                ALL_COLUMNS,
                null,
                null,
                null,
                null,
                ID + " ASC");

        List<Message> result = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(entityFromCursor(cursor));
            }
            cursor.close();
        }

        db.close();

        return result;
    }

    public void delete(Integer id) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE, ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    private Message entityFromCursor(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndex(ID));
        String content = cursor.getString(cursor.getColumnIndex(CONTENT));
        Message result = new Message(id, content);
        return result;
    }

}
