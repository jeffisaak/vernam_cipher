package com.aptasystems.vernamcipher.database;

import android.content.Context;

public abstract class AbstractDatabase {

    protected static final String CREATE = "CREATE TABLE";
    protected static final String WITH_COLUMNS = "(";
    protected static final String DONE_COLUMNS = ");";
    protected static final String INTEGER = "INTEGER";
    protected static final String TEXT = "TEXT";
    protected static final String BLOB = "BLOB";
    protected static final String PRIMARY_KEY = "PRIMARY KEY";
    protected static final String AUTO_INCREMENT = "AUTOINCREMENT";
    protected static final String NOT_NULL = "NOT NULL";
    protected static final String NEXT_COLUMN = ",";

    protected static String buildSql(String... items) {
        StringBuilder resultBuilder = new StringBuilder();
        for (String item : items) {
            if (resultBuilder.length() > 0) {
                resultBuilder.append(" ");
            }
            resultBuilder.append(item);
        }
        return resultBuilder.toString();
    }

    protected Context _context;

}
