package com.aptasystems.vernamcipher.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.aptasystems.vernamcipher.model.SecretKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jisaak on 2016-02-25.
 */
public class SecretKeyDatabase extends AbstractDatabase {

    private static SecretKeyDatabase _instance;

    public static SecretKeyDatabase getInstance(Context context) {
        if (_instance == null) {
            _instance = new SecretKeyDatabase(context);
        }
        return _instance;
    }

    // Define our table name and columns.
    private static final String TABLE = "secretKey";
    private static final String NAME = "name";
    private static final String COLOUR = "colour";
    private static final String DESCRIPTION = "description";
    private static final String KEY = "key";
    private static final String BYTES_REMAINING = "bytesRemaining";

    private static final String[] ALL_COLUMNS = new String[]{ID, NAME, COLOUR, DESCRIPTION, KEY, BYTES_REMAINING};
    private static final String[] ALL_COLUMNS_BUT_KEY = new String[]{ID, NAME, COLOUR, DESCRIPTION, BYTES_REMAINING};

    // Uses the buildSql method to construct our table creation SQL.
    public static final String SQL_CREATE = buildSql(
            CREATE, TABLE, WITH_COLUMNS,
            ID, INTEGER, PRIMARY_KEY, AUTO_INCREMENT, NEXT_COLUMN,
            NAME, TEXT, NOT_NULL, NEXT_COLUMN,
            COLOUR, INTEGER, NEXT_COLUMN,
            DESCRIPTION, TEXT, NEXT_COLUMN,
            KEY, BLOB, NOT_NULL, NEXT_COLUMN,
            BYTES_REMAINING, INTEGER, NOT_NULL, DONE_COLUMNS);

    private SecretKeyDatabase(Context context) {
        _context = context;
    }

    public long insert(String name, int colour, String description, byte[] keyData) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(NAME, name);
        cv.put(COLOUR, colour);
        cv.put(DESCRIPTION, description);
        cv.put(KEY, keyData);
        cv.put(BYTES_REMAINING, keyData.length);
        long rid = db.insert(TABLE, null, cv);

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }

        return rid;
    }

    /**
     * Insert a new key into the database.
     *
     * @param id
     * @param includeKey
     * @return
     */
    public SecretKey fetch(long id, boolean includeKey) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE,
                includeKey ? ALL_COLUMNS : ALL_COLUMNS_BUT_KEY,
                ID_EQUALS_PARAM,
                new String[]{String.valueOf(id)},
                null,
                null,
                ID_ASCENDING);

        SecretKey result = null;
        if (cursor != null) {
            cursor.moveToFirst();
            if (cursor.moveToFirst()) {
                result = entityFromCursor(cursor, includeKey);
            }
            cursor.close();
        }

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }

        return result;
    }

    /**
     * List the secret keys in the database.
     *
     * @param includeKey
     * @return
     */
    public List<SecretKey> list(boolean includeKey) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query(
                TABLE,
                includeKey ? ALL_COLUMNS : ALL_COLUMNS_BUT_KEY,
                null,
                null,
                null,
                null,
                ID_ASCENDING);

        List<SecretKey> result = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                result.add(entityFromCursor(cursor, includeKey));
            }
            cursor.close();
        }

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }

        return result;
    }

    /**
     * Delete the secret key with the given id from the database.
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
     * Update the secret key with new data.
     *
     * @param id
     * @param newKey
     */
    public void updateKey(Long id, byte[] newKey, int bytesRemaining) {
        DBHelper dbHelper = new DBHelper(_context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(KEY, newKey);
        cv.put(BYTES_REMAINING, bytesRemaining);
        db.update(TABLE, cv, ID_EQUALS_PARAM, new String[]{String.valueOf(id)});

        if( CLOSE_DB_WHEN_COMPLETE ) {
            db.close();
        }
    }

    /**
     * Build a secret key object from the cursor.
     *
     * @param cursor
     * @param includeKey
     * @return
     */
    private SecretKey entityFromCursor(Cursor cursor, boolean includeKey) {
        long id = cursor.getLong(cursor.getColumnIndex(ID));
        String name = cursor.getString(cursor.getColumnIndex(NAME));
        int colour = cursor.getInt(cursor.getColumnIndex(COLOUR));
        String description = cursor.getString(cursor.getColumnIndex(DESCRIPTION));
        int bytesRemaining = cursor.getInt(cursor.getColumnIndex(BYTES_REMAINING));
        byte[] key = null;
        if (includeKey) {
            key = cursor.getBlob(cursor.getColumnIndex(KEY));
        }
        return new SecretKey(id, name, colour, description, key, bytesRemaining);
    }

}
