package com.aptasystems.vernamcipher.database;

import android.content.Context;

public abstract class AbstractDatabase {

    protected static final boolean CLOSE_DB_WHEN_COMPLETE = true;

    protected static final int TRUE = 1;
    protected static final int FALSE = 0;

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

    protected static final String ID = "_id";

    protected static final String ID_EQUALS_PARAM = ID + " = ?";
    protected static final String ID_ASCENDING = ID + " ASC";

    protected Context _context;

    /**
     * Builds an SQL statement using an array of items as contents.  All this method really does is
     * concatenate the passed in array, putting spaces between each item.  We use it in a clever way
     * to build SQL that is readable in the code.
     *
     * @param items
     * @return
     */
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
}
