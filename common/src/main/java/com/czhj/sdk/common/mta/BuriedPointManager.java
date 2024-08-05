package com.czhj.sdk.common.mta;


import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;

import com.czhj.sdk.common.Constants;
import com.czhj.sdk.common.Database.SQLiteMTAHelper;
import com.czhj.sdk.common.ThreadPool.RepeatingHandlerRunnable;
import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.sdk.common.models.Config;
import com.czhj.sdk.common.network.BuriedPointRequest;
import com.czhj.sdk.common.utils.AESUtil;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.VolleyError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.DeflaterOutputStream;


public class BuriedPointManager {

    private static final int MAX_LOGS_COUNT = 5000 * 100;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();// 定义锁对象
    private HashMap<Integer, String> send_value = null;
    private List<String> wait_send_list = null;

    private volatile boolean isSending;
    private static BuriedPointManager sInstance;
    private final Set<Integer> mLogBackList = new HashSet<>();
    private SQLiteDatabase database;
    private int wait_send_size;
    private RepeatingHandlerRunnable repeatingHandlerRunnable;

    private BuriedPointManager() {

    }

    public void start() {

        if (database == null || repeatingHandlerRunnable == null) {
            database = SQLiteMTAHelper.getInstance().getWritableDatabase();
            clearLogDB();
            HandlerThread sendLog = new HandlerThread("sendLog");
            sendLog.start();
            Looper looper = sendLog.getLooper();
            repeatingHandlerRunnable = new RepeatingHandlerRunnable(new Handler(looper)) {
                @Override
                protected void doWork() {
                    try {
                        sendPoint();
                        repeatingHandlerRunnable.startRepeating(Config.sharedInstance().getSend_log_interval() * 1000);

                    } catch (Throwable throwable) {
                        SigmobLog.e("retryFaildTracking error " + throwable.getMessage());
                    }
                }
            };
            repeatingHandlerRunnable.startRepeating(Config.sharedInstance().getSend_log_interval() * 1000);
        }

    }

    public void addWaitSend(String log) {
        if (wait_send_list == null) {
            wait_send_list = new LinkedList<>();
        }

        wait_send_list.add(log);
    }

    public static BuriedPointManager getInstance() {
        synchronized (BuriedPointManager.class) {
            if (sInstance == null) {
                sInstance = new BuriedPointManager();
            }
            return sInstance;
        }
    }


    public void clearLogDB() {
        ThreadPoolFactory.BackgroundThreadPool.getInstance().submit(new Runnable() {
            @Override
            public void run() {
                clearNullLogDB();
                cleanLogsDBByLogCount();
            }
        });
    }

    /**
     * 采用gzip压缩，然后base64编码
     *
     * @param str
     * @return
     * @throws IOException
     */
    public static String deflateAndBase64(String str) throws IOException {

        if (str == null || str.length() == 0) {
            return str;
        }

        // 使用 deflate 压缩字符串
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DeflaterOutputStream deflater = new DeflaterOutputStream(out);
        deflater.write(str.getBytes(Charset.forName("UTF-8")));
        deflater.flush();
        deflater.close();

        // 将压缩后的二进制数据base64成普通文本
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
    }

    private void cleanLogsDBByLogCount() {
        try {
            long numRows = DatabaseUtils.queryNumEntries(this.database, SQLiteMTAHelper.TABLE_POINT);

            if (numRows <= MAX_LOGS_COUNT) {
                return;
            }

            long maxNum = numRows - MAX_LOGS_COUNT;

            clearLogDB(maxNum);

        } catch (Throwable throwable) {
            SigmobLog.e("clearLogDB fail", throwable);
        } finally {

        }
    }

    private synchronized HashMap<Integer, String> getLogs(int maxNum) {

        HashMap<Integer, String> stringList = new HashMap<>();

        Cursor cursor = null;
        try {

            cursor = database.rawQuery("select * from " + SQLiteMTAHelper.TABLE_POINT + " where item not null" + " order by point_id", null);

            int count = 0;
            if (cursor != null && cursor.moveToFirst()) {
                int itemColumn = cursor.getColumnIndex("item");
                int idColumn = cursor.getColumnIndex("point_id");
                int encrypColumn = cursor.getColumnIndex("encryption");

                while (count < maxNum) {
                    String item = cursor.getString(itemColumn);
                    Integer id = cursor.getInt(idColumn);
                    Integer encryption = cursor.getInt(encrypColumn);
                    if (!TextUtils.isEmpty(item)) {
                        if (encryption == 1) {
                            String s = AESUtil.DecryptString(item, Constants.AES_KEY);
                            if (!TextUtils.isEmpty(s)) {
                                stringList.put(id, AESUtil.DecryptString(item, Constants.AES_KEY));
                            }
                        } else {
                            stringList.put(id, item);
                        }
                    } else {

                    }
                    if (!cursor.moveToNext()) {
                        break;
                    }
                    count++;
                }
            }
        } catch (Throwable throwable) {
            SigmobLog.e("getlogs fail", throwable);
        } finally {

            if (cursor != null) {
                cursor.close();
            }

        }

        return stringList;
    }

    private void clearNullLogDB() {
        try {

//            long numRows = DatabaseUtils.queryNumEntries(database, SQLiteMTAHelper.TABLE_POINT);
//            SigmobLog.d("begin numRows " + numRows);

            StringBuilder builder = new StringBuilder();

            builder.append("delete from ");
            builder.append(SQLiteMTAHelper.TABLE_POINT);
            builder.append(" where item is null");
            database.execSQL(builder.toString());

//            numRows = DatabaseUtils.queryNumEntries(database, SQLiteMTAHelper.TABLE_POINT);
//            SigmobLog.d("end numRows " + numRows);

        } catch (Throwable throwable) {
            SigmobLog.e("clearLogDB fail", throwable);
        } finally {

        }
    }

    private void clearLogDB(Set<Integer> ids) {

        try {
            Iterator<Integer> it = ids.iterator();
            if (!it.hasNext()) return;

//            long numRows = DatabaseUtils.queryNumEntries(database, SQLiteMTAHelper.TABLE_POINT);
//            SigmobLog.d("begin numRows " + numRows);

            StringBuilder builder = new StringBuilder();

            builder.append("delete from ");
            builder.append(SQLiteMTAHelper.TABLE_POINT);
            builder.append(" where point_id in ( ");

            for (; ; ) {
                Integer e = it.next();
                builder.append(e);
                if (!it.hasNext()) break;
                builder.append(',').append(' ');
            }
            builder.append(" )");

            database.execSQL(builder.toString());

//            numRows = DatabaseUtils.queryNumEntries(database, SQLiteMTAHelper.TABLE_POINT);
//            SigmobLog.d("end numRows " + numRows);

        } catch (Throwable throwable) {
            SigmobLog.e("clearLogDB fail", throwable);
        } finally {


        }
    }

    private void clearLogDB(long maxNum) {
        try {

//            long numRows = DatabaseUtils.queryNumEntries(database, SQLiteMTAHelper.TABLE_POINT);
//            SigmobLog.d("begin numRows " + numRows);

            StringBuilder builder = new StringBuilder();

            builder.append("delete from ");
            builder.append(SQLiteMTAHelper.TABLE_POINT);
            builder.append(" where point_id in ( ");

            builder.append(" select point_id from ");
            builder.append(SQLiteMTAHelper.TABLE_POINT);
            builder.append(" order by point_id ");
            builder.append(" limit " + maxNum);
            builder.append(" )");

            database.execSQL(builder.toString());

//            numRows = DatabaseUtils.queryNumEntries(database, SQLiteMTAHelper.TABLE_POINT);
//            SigmobLog.d("end numRows " + numRows);

        } catch (Throwable throwable) {
            SigmobLog.e("clearLogDB fail", throwable);
        } finally {


        }
    }

    public String sendPoint() {
        String body = null;

        try {
            readWriteLock.readLock().lock();
            if (isSending || (send_value != null && send_value.size() > 0)) {
                return body;
            }
            StringBuilder jsonStringBuilder = new StringBuilder();

            send_value = getLogs(Config.sharedInstance().getMax_send_log_records());

            if (send_value.size() == 0) {
                return null;
            }

            jsonStringBuilder.append("[");
            Iterator<String> entityIterator = send_value.values().iterator();
            while (entityIterator.hasNext()) {
                String pointEntityJsonString = entityIterator.next();
                jsonStringBuilder.append(pointEntityJsonString);

                if (entityIterator.hasNext()) {
                    jsonStringBuilder.append(",");
                }
            }

            jsonStringBuilder.append("]");

            String dcLog = jsonStringBuilder.toString();

            try {
                SigmobLog.d("dcLog: " + dcLog);
                SigmobLog.d("BPLog_Count: " + send_value.size());
                String rawData = deflateAndBase64(dcLog);
                body = PointEntitySuper.toURLEncoded(rawData);
                sendServer(body, true);
            } catch (IOException e) {
                SigmobLog.e(e.getMessage());
            }
        } catch (Throwable th) {
            SigmobLog.e("sendPoint fail ", th);
        } finally {
            readWriteLock.readLock().unlock();
        }

        return body;
    }

    private String getWaitSendLogs() {
        StringBuilder jsonStringBuilder = new StringBuilder();

        if (wait_send_list != null && wait_send_list.size() > 0) {
            wait_send_size = wait_send_list.size();

            Iterator<String> iterator = wait_send_list.iterator();
            while (iterator.hasNext()) {
                String pointEntityJsonString = iterator.next();
                jsonStringBuilder.append(pointEntityJsonString);
                if (iterator.hasNext()) {
                    jsonStringBuilder.append(",");
                }
            }
        }
        return jsonStringBuilder.toString();
    }

    private void clearLog() {
        readWriteLock.writeLock().lock();
        if (send_value == null || send_value.size() == 0) {
            readWriteLock.writeLock().unlock();
            return;
        }

        clearLogDB(send_value.keySet());
        isSending = false;
        send_value = null;
        readWriteLock.writeLock().unlock();

    }

    private void clearWaitList() {
        if (wait_send_list != null && wait_send_list.size() > 0) {

            if (wait_send_list.size() > wait_send_size) {
                wait_send_list = wait_send_list.subList(wait_send_size, wait_send_list.size() - 1);
            } else {
                wait_send_list.clear();
            }
            wait_send_size = 0;
        }
    }

    private void sendServer(String body, final boolean usedb) {

        isSending = true;
        BuriedPointRequest.BuriedPointSend(body, new BuriedPointRequest.RequestListener() {
            @Override
            public void onSuccess() {
                if (usedb) {
                    ThreadPoolFactory.BackgroundThreadPool.getInstance().submit(new Runnable() {
                        @Override
                        public void run() {
                            clearLog();
                        }
                    });
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                isSending = false;
                send_value = null;
                SigmobLog.e(error.getMessage());
            }
        });
    }

    public Set<Integer> getLogBlackList() {
        return mLogBackList;
    }
}



