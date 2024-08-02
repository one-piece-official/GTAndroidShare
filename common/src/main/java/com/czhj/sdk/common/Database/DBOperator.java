package com.czhj.sdk.common.Database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.czhj.sdk.common.ThreadPool.BackgroundThreadFactory;
import com.czhj.sdk.logger.SigmobLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DBOperator {

    private static final DBOperator ourInstance = new DBOperator();

    //ArrayBlockingQueue作为其等待队列，队列初始化容量为8
    private final ThreadPoolExecutor executor;
    private final Object lock;

    public static synchronized DBOperator getInstance() {
        return ourInstance;
    }

    private DBOperator() {

        lock = new Object();

        //等待队列，容量为10
        ArrayBlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(10);
        //该线程池核心容量为2，最大容量为5，线程存活时间为1分钟。
        this.executor = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, blockingQueue, new BackgroundThreadFactory());
    }

    /**
     * 查询表中数据的行数
     *
     * @param tableName 表对应的model
     * @param selection 查询条件
     */
    public int count(SQLiteDatabase database, String tableName, String selection) {
        Cursor cursor = null;
        int count = 0;
        try {
            SQLiteDatabase readDb = database;
            cursor = readDb.query(tableName, null, selection, null, null, null, null, null);
            count = cursor.getCount();
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }


    /**
     * 查询表中数据的行数
     *
     * @param tableName 表对应的model
     */
    public int count(SQLiteDatabase database, String tableName) {
        return count(database, tableName, null);
    }


    /**
     * 删除数据
     *
     * @param tableName   表对应的model
     * @param whereClause 删除条件
     */
    public void delete(SQLiteDatabase database, final String tableName, String whereClause, SQLiteLisenter execCallBack) {
        try {
            SQLiteDeleteThread task = new SQLiteDeleteThread(database, tableName, whereClause, execCallBack);
            executor.submit(task);
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
            execCallBack.onFailed(new Error(e.getMessage()));
        }

    }


    public void find(SQLiteDatabase sqLiteDatabase, final String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, DataSQLiteLisenter lisenter) {

        SQLiteThread task = new SQLiteThread(sqLiteDatabase, table, columns, selection, selectionArgs, groupBy, having, orderBy, limit, lisenter);
        executor.submit(task);
    }

    public interface DataSQLiteLisenter {
        void onSuccess(List<Map> list);

        void onFailed(Error error);
    }


    private class SQLiteDeleteThread implements Runnable {

        private final String table;
        private final String whereClause;
        private final SQLiteLisenter callBack;
        private final SQLiteDatabase sqLiteDatabase;

        SQLiteDeleteThread(SQLiteDatabase database, String table, String whereClause, SQLiteLisenter callBack) {
            this.table = table;
            this.sqLiteDatabase = database;
            this.whereClause = whereClause;
            this.callBack = callBack;
        }

        @Override
        public void run() {
            try {
                SQLiteDatabase db = sqLiteDatabase;
                int delC = db.delete(table, whereClause, null);
                if (callBack != null) {
                    callBack.onSuccess(null);
                }
            } catch (Throwable e) {
                if (callBack != null) {
                    callBack.onFailed(new Error(e.getMessage()));
                }
            }
        }
    }


    private class SQLiteThread implements Runnable {

        private final String table;
        private final String[] columns;
        private final String selection;
        private final String[] selectionArgs;
        private final String groupBy;
        private final String having;
        private final String orderBy;
        private final String limit;
        private final SQLiteDatabase sqLiteDatabase;
        private final DataSQLiteLisenter lisenter;


        SQLiteThread(SQLiteDatabase sqLiteDatabase, String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit, DataSQLiteLisenter lisenter) {
            this.table = table;
            this.sqLiteDatabase = sqLiteDatabase;
            this.columns = columns;
            this.selection = selection;
            this.selectionArgs = selectionArgs;
            this.groupBy = groupBy;
            this.having = having;
            this.orderBy = orderBy;
            this.limit = limit;
            this.lisenter = lisenter;
        }

        private List<Map> cursorToList(Cursor cursor) {
            List<Map> list = new ArrayList<>();
            while (cursor.moveToNext()) {
                String[] nameArray = cursor.getColumnNames();
                Map map = new HashMap();
                for (String aNameArray : nameArray) {
                    int index = cursor.getColumnIndex(aNameArray);
                    if (index >= 0) {
                        switch (cursor.getType(index)) {
                            case Cursor.FIELD_TYPE_NULL:
                                break;
                            case Cursor.FIELD_TYPE_INTEGER:
                                long x1 = cursor.getLong(index);
                                map.put(aNameArray, x1);
                                break;
                            case Cursor.FIELD_TYPE_FLOAT:
                                double x2 = cursor.getDouble(index);
                                map.put(aNameArray, x2);
                                break;
                            case Cursor.FIELD_TYPE_STRING:
                                String x3 = cursor.getString(index);
                                map.put(aNameArray, x3);
                                break;
                            case Cursor.FIELD_TYPE_BLOB:
                                byte[] x4 = cursor.getBlob(index);
                                map.put(aNameArray, x4);
                                break;
                            default:
                                break;
                        }
                    }

                }
                list.add(map);
            }
            return list;
        }


        @Override
        public void run() {
            Cursor cursor = null;
            try {
                SQLiteDatabase readDb = sqLiteDatabase;
                cursor = readDb.query(table, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
                List<Map> list = cursorToList(cursor);
                if (lisenter != null) {
                    lisenter.onSuccess(list);
                }
            } catch (Throwable e) {
                if (lisenter != null) {
                    lisenter.onFailed(new Error(e.getMessage()));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }


}
