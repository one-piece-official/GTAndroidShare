package com.czhj.sdk.common.Database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.util.Log;

import com.czhj.sdk.common.Constants;
import com.czhj.sdk.logger.SigmobLog;

public class SQLiteMTAHelper extends SQLiteOpenHelper {

    public static final String TABLE_POINT = "point";

    private static String DEFAULT_MTA_NAME = Constants.SDK_COMMON_FOLDER + "_mta.db";

    private static final int DATABASE_VERSION = 10;

    private static String create_point_Sql = "CREATE TABLE point ( point_id integer  primary key  ,item text , encryption int);\n";

    // errors are negative, ok is 0, anything else is positive.
    private static final long DB_ERROR_NULL = -6;
    private static final long DB_ERROR_NOT_OPEN = -5;
    private static final long DB_ERROR_READ_ONLY = -4;
    public static final long DB_ERROR_BAD_INPUT = -2;
    public static final long DB_WRITE_ERROR = -1; // from SQLiteDatabase if an error occurred
    private static final long DB_OK = 0;

    private static SQLiteMTAHelper gInstance = null;

    private static SQLiteDatabase writedb = null;

    public SQLiteMTAHelper(final Context context) {
        super(context, DEFAULT_MTA_NAME, null, DATABASE_VERSION);
    }

    public static void initialize(final Context context) {
        if (gInstance == null) {
            synchronized (SQLiteMTAHelper.class) {
                if (gInstance == null) {
                    gInstance = new SQLiteMTAHelper(context);
                }
            }
        }
    }

    public static SQLiteMTAHelper getInstance() {
        return gInstance;
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
        } else {
            db.enableWriteAheadLogging();
        }
    }

    public interface ExecCallBack {
        void onSuccess();

        void onFailed(Throwable e);
    }

    public static void insert(SQLiteDatabase sqLiteDatabase, SQLiteBuider.Insert insert, ExecCallBack callBack) {
        sqLiteDatabase.beginTransaction();


        boolean result = false;
        try {

            SQLiteStatement sqlListStatment = sqLiteDatabase.compileStatement(insert.sql);

            for (int i = 1; i <= insert.columns.size(); i++) {


                String colume = (String) insert.columns.get(i - 1);
                Object value = insert.values.get(colume);

                if (value == null) {
                    sqlListStatment.bindNull(i);
                } else if (value instanceof String) {
                    sqlListStatment.bindString(i, (String) value);
                } else if (value instanceof Double) {
                    sqlListStatment.bindDouble(i, (Double) value);
                } else if (value instanceof Number) {
                    sqlListStatment.bindLong(i, ((Number) value).longValue());
                } else if (value instanceof byte[]) {
                    sqlListStatment.bindBlob(i, (byte[]) value);
                } else {
                    sqlListStatment.bindNull(i);
                }
            }
            sqlListStatment.execute();
            sqLiteDatabase.setTransactionSuccessful();
            result = true;
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
            if (callBack != null) {
                callBack.onFailed(e);
            }
        } finally {
            try {
                sqLiteDatabase.endTransaction();
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
                if (callBack != null) {
                    callBack.onFailed(e);
                }
            }
        }
        if (result && callBack != null) {
            callBack.onSuccess();
        }
    }


    public void transactionWriteExecSQL(SQLiteDatabase sqLiteDatabase, String sql, ExecCallBack callBack) {
        sqLiteDatabase.beginTransaction();

        boolean result = false;
        try {
            sqLiteDatabase.execSQL(sql, new Object[]{});
            sqLiteDatabase.setTransactionSuccessful();
            result = true;
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
            if (callBack != null) {
                callBack.onFailed(e);
            }
        } finally {
            sqLiteDatabase.endTransaction();
        }
        if (result && callBack != null) {
            callBack.onSuccess();
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL(create_point_Sql);

    }

    private static long errorChecks(SQLiteDatabase db) {
        if (db == null) {
            return DB_ERROR_NULL;
        } else if (!db.isOpen()) {
            return DB_ERROR_NOT_OPEN;
        } else if (db.isReadOnly()) {
            return DB_ERROR_READ_ONLY;
        } else {
            return DB_OK;
        }
    }


    @Override
    public void onDowngrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(SQLiteMTAHelper.class.getName(), "Downgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");

        recreateDb(database);


    }

    private void dropFieldWithTable(final SQLiteDatabase database, final String field, final String table) {
        String sql = "alter table " + table + " drop column " + field;

        database.execSQL(sql);
    }

    private void addFieldWithTable(final SQLiteDatabase database, final String field, final String type, final String table) {
        String sql = "alter table " + table + " add " + field + " " + type;

        database.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.w(SQLiteMTAHelper.class.getName(), "Upgrading database from version " + oldVersion + " to " + newVersion + ", which will destroy all old data");
        if (oldVersion == 9) {
            recreateDb(database);
        } else if (newVersion > 8) {
            database.execSQL("ALTER TABLE " + TABLE_POINT + " ADD COLUMN 'encryption' int ");
        }

    }


    public void clearDb() {
        recreateDb(getWritableDatabase());
    }

    private void recreateDb(SQLiteDatabase database) {

        database.execSQL("DROP TABLE IF EXISTS " + TABLE_POINT);

        onCreate(database);

    }

}
