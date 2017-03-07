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

    // Define our table name and columns.
    private static final String TABLE = "message";
    private static final String INCOMING = "incoming";
    private static final String CONTENT = "content";

    private static final String[] ALL_COLUMNS = new String[]{ID, INCOMING, CONTENT};

    // Uses the buildSql method to construct our table creation SQL.
    public static final String SQL_CREATE = buildSql(
            CREATE, TABLE, WITH_COLUMNS,
            ID, INTEGER, PRIMARY_KEY, AUTO_INCREMENT, NEXT_COLUMN,
            INCOMING, INTEGER, NOT_NULL, NEXT_COLUMN,
            CONTENT, TEXT, NOT_NULL, DONE_COLUMNS);

    private MessageDatabase(Context context) {
        _context = context;
    }

    /**
     * Insert a new message into the database.
     *
     * @param incoming
     * @param content
     * @return
     */
    public long insert(boolean incoming, String content) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(INCOMING, incoming ? TRUE : FALSE );
        cv.put(CONTENT, content);
        long rid = db.insert(TABLE, null, cv);

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }

        return rid;
    }

    /**
     * Fetch the message with the given id from the database.
     *
     * @param id
     * @return
     */
    public Message fetch(long id) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE,
                ALL_COLUMNS,
                ID_EQUALS_PARAM,
                new String[]{String.valueOf(id)},
                null,
                null,
                ID_ASCENDING);

        Message result = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                result = entityFromCursor(cursor);
            }
            cursor.close();
        }

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }

        return result;
    }

    /**
     * List all messages in the database.
     *
     * @return
     */
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
                ID_ASCENDING);

        List<Message> result = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(entityFromCursor(cursor));
            }
            cursor.close();
        }

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }

        return result;
    }

    /**
     * Delete the message with the given id from the database.
     *
     * @param id
     */
    public void delete(Long id) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(TABLE, ID_EQUALS_PARAM, new String[]{String.valueOf(id)});
        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }
    }

    /**
     * Construct a message from the cursor.
     *
     * @param cursor
     * @return
     */
    private Message entityFromCursor(Cursor cursor) {
        long id = cursor.getLong(cursor.getColumnIndex(ID));
        int incoming = cursor.getInt(cursor.getColumnIndex(INCOMING));
        String content = cursor.getString(cursor.getColumnIndex(CONTENT));
        return new Message(id, incoming == TRUE, content);
    }

}
