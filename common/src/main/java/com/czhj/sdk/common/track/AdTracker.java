package com.czhj.sdk.common.track;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import com.czhj.sdk.common.Database.DBOperator;
import com.czhj.sdk.common.Database.SQLiteBuider;
import com.czhj.sdk.common.Database.SQLiteLisenter;
import com.czhj.sdk.common.Database.SQLiteTrackHelper;
import com.czhj.sdk.logger.SigmobLog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AdTracker implements Serializable {
    private static final long serialVersionUID = 1L;
    private final MessageType mMessageType;
    private final String mEvent;
    private final String mRequest_id;
    private String mSource;
    private Long mId;
    private Long timestamp;
    private Integer retryNum;
    private Integer retryCount;
    private String mUrl;
    private boolean mCalled;
    private String extInfo;

    public AdTracker(final MessageType messageType, final String content, final String event, final String request_id) {
        this.mRequest_id = request_id;
        mMessageType = messageType;
        mUrl = content;
        mEvent = event;
        mSource = "native";
    }

    public static SQLiteBuider.CreateTable createTable() {

        SQLiteBuider.CreateTable.Builder buider = new SQLiteBuider.CreateTable.Builder();

        buider.setTableName(SQLiteTrackHelper.TABLE_TRACK);
        buider.setPrimaryKey("id", "long");

        buider.autoincrement(true);
        Map<String, String> columns = new HashMap<>();

        columns.put("url", "text");
        columns.put("event", "text");
        columns.put("request_id", "text");
        columns.put("timestamp", "long");
        columns.put("source", "text");
        columns.put("retryNum", "int");
        columns.put("extInfo", "text");
        columns.put("messageType", "int");

        buider.setColumns(columns);

        return buider.build();
    }

    public static List<AdTracker> getAdTrackerFromDB(int maxNum, final long expiredMs) {

        List<AdTracker> adEventList = new ArrayList<>();
        SQLiteDatabase db = null;

        Cursor cursor = null;
        try {

            db = SQLiteTrackHelper.getInstance().getReadableDatabase();
            cursor = db.rawQuery("select * from " + SQLiteTrackHelper.TABLE_TRACK + " where timestamp > " + (System.currentTimeMillis() - expiredMs) + " order by id desc" + " limit " + maxNum, null);

            int count = 0;
            if (cursor != null && cursor.moveToFirst()) {
                int urlColumn = cursor.getColumnIndex("url");
                int idColumn = cursor.getColumnIndex("id");
                int eventColumn = cursor.getColumnIndex("event");
                int requestIdColumn = cursor.getColumnIndex("request_id");
                int timestampColumn = cursor.getColumnIndex("timestamp");
                int sourceColumn = cursor.getColumnIndex("source");
                int retryNumColumn = cursor.getColumnIndex("retryNum");
                int extInfoColumn = cursor.getColumnIndex("extInfo");
                int messageTypeColumn = cursor.getColumnIndex("messageType");

                while (count < maxNum) {
                    try {
                        String track_url = cursor.getString(urlColumn);
                        Long id = cursor.getLong(idColumn);
                        String event = cursor.getString(eventColumn);
                        String request_id = cursor.getString(requestIdColumn);
                        Long timeStamp = cursor.getLong(timestampColumn);
                        String source = cursor.getString(sourceColumn);
                        Integer retryNum = cursor.getInt(retryNumColumn);
                        String extInfo = cursor.getString(extInfoColumn);
                        Integer type = cursor.getInt(messageTypeColumn);

                        if (TextUtils.isEmpty(track_url) || id < 0 || TextUtils.isEmpty(event) || TextUtils.isEmpty(request_id)) {
                            continue;
                        }

                        MessageType messageType;

                        switch (type) {
                            case 1: {
                                messageType = MessageType.QUARTILE_EVENT;
                            }
                            break;
                            case 2: {
                                messageType = MessageType.TOBID_TRACKING_URL;
                            }
                            break;
                            case 0:
                            default: {
                                messageType = MessageType.TRACKING_URL;
                            }
                        }

                        AdTracker adTracker = new AdTracker(messageType, track_url, event, request_id);
                        if (adTracker != null) {
                            adTracker.setId(id);
                            adTracker.setRetryCount(retryNum);
                            adTracker.setTimestamp(timeStamp);
                            if (!TextUtils.isEmpty(source)) {
                                adTracker.setSource(source);
                            }
                            adTracker.setExtInfo(extInfo);
                            adEventList.add(adTracker);

                        }

                    } catch (Throwable throwable) {
                        SigmobLog.e("getAdTrackList error", throwable);
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

        return adEventList;
    }

    public void setExtInfo(String extInfo) {
        this.extInfo = extInfo;
    }

    public String getExtInfo() {
        return extInfo;
    }

    public static void cleanExpiredAdTracker(long expiredMs) {

        try {
            String where = "timestamp < " + (System.currentTimeMillis() - expiredMs);

            DBOperator.getInstance().delete(SQLiteTrackHelper.getInstance().getWritableDatabase(), SQLiteTrackHelper.TABLE_TRACK, where, new SQLiteLisenter() {
                @Override
                public void onSuccess(List list) {

                }

                @Override
                public void onFailed(Error error) {
                    SigmobLog.e(error.getMessage());

                }
            });
        } catch (Throwable throwable) {
            SigmobLog.e("cleanExpiredAdTracker error", throwable);
        }

    }

    public static void cleanLimitAdTracker(long maxNum) {

        Cursor cursor = null;
        SQLiteDatabase db = null;
        Long limitId = null;
        try {
            long count = 0;
            db = SQLiteTrackHelper.getInstance().getReadableDatabase();
            cursor = db.rawQuery("select * from " + SQLiteTrackHelper.TABLE_TRACK, null);
            if (cursor != null) {
                count = cursor.getCount();
                cursor.close();
                cursor = null;
            }

            if (count > maxNum) {
                cursor = db.rawQuery("select * from " + SQLiteTrackHelper.TABLE_TRACK + " order by id desc" + " limit " + maxNum, null);

                int idColumn = cursor.getColumnIndex("id");
                if (cursor != null && cursor.moveToLast()) {
                    limitId = cursor.getLong(idColumn);
                    cursor.close();
                    cursor = null;
                }
            }


            if (limitId != null) {
                String where = "id <'" + limitId + "'";
                DBOperator.getInstance().delete(SQLiteTrackHelper.getInstance().getWritableDatabase(), SQLiteTrackHelper.TABLE_TRACK, where, new SQLiteLisenter() {
                    @Override
                    public void onSuccess(List list) {

                    }

                    @Override
                    public void onFailed(Error e) {
                        SigmobLog.e(e.getMessage());

                    }
                });
            }


        } catch (Throwable throwable) {
            SigmobLog.e("cleanLimitAdTracker ", throwable);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    public String getSource() {
        return mSource;
    }

    public void setSource(String mSource) {
        this.mSource = mSource;
    }

    public int getRetryCount() {
        return retryCount;
    }

    private void setRetryCount(int count) {
        retryCount = count;
    }

    public void setRetryCountInc() {
        retryCount++;
    }

    public void updateToDB() {
        try {

            SQLiteBuider.Update.Builder builder = new SQLiteBuider.Update.Builder();
            builder.setTableName(SQLiteTrackHelper.TABLE_TRACK);
            builder.setWhere(" where id=" + mId.toString());

            Map<String, Object> values = new HashMap<>();
            values.put("retryNum", retryCount);
            builder.setColumnValues(values);

            String sql = builder.build().getSql();
            SQLiteTrackHelper.getInstance().transactionWriteExecSQL(SQLiteTrackHelper.getInstance().getWritableDatabase(), sql, null);

        } catch (Throwable th) {
            SigmobLog.e(th.getMessage());
        }

    }

    public void insertToDB(final SQLiteTrackHelper.ExecCallBack execCallBack) {

        try {
            SQLiteBuider.Insert.Builder builder = new SQLiteBuider.Insert.Builder();
            builder.setTableName(SQLiteTrackHelper.TABLE_TRACK);
            Map<String, Object> values = new HashMap<>();
            int type;
            switch (mMessageType) {

                case QUARTILE_EVENT: {
                    type = 1;
                }
                break;
                case TOBID_TRACKING_URL: {
                    type = 2;
                }
                break;
                case TRACKING_URL:
                default: {
                    type = 0;
                }
                break;

            }

            values.put("url", mUrl);
            values.put("request_id", mRequest_id);
            values.put("event", mEvent);
            values.put("source", mSource);
            values.put("retryNum", retryCount);
            values.put("timestamp", System.currentTimeMillis());
            values.put("extInfo", extInfo);
            values.put("messageType", type);

            builder.setColumnValues(values);

            SQLiteTrackHelper.getInstance().insert(SQLiteTrackHelper.getInstance().getWritableDatabase(), builder.build(), new SQLiteTrackHelper.ExecCallBack() {
                @Override
                public void onSuccess() {
                    SigmobLog.d("event: " + mEvent + " url " + mUrl + " mRequest_id: " + mRequest_id + " insert success! ");

                    if (execCallBack != null) {
                        execCallBack.onSuccess();
                    }
                }

                @Override
                public void onFailed(Throwable e) {
                    if (execCallBack != null) {
                        execCallBack.onFailed(e);
                    }
                    SigmobLog.e(e.getMessage());
                }
            });

        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        //end
    }

    public Long getId() {
        return mId;
    }

    private void setId(Long id) {
        mId = id;
    }

    public void deleteDB() {

        if (mId == null) return;
        try {
            String where = "id ='" + mId + "'";
            DBOperator.getInstance().delete(SQLiteTrackHelper.getInstance().getWritableDatabase(), SQLiteTrackHelper.TABLE_TRACK, where, new SQLiteLisenter() {
                @Override
                public void onSuccess(List list) {

                    SigmobLog.d("delete id " + mId);
                }

                @Override
                public void onFailed(Error e) {
                    SigmobLog.e(e.getMessage());

                }
            });
        } catch (Throwable th) {
            SigmobLog.e(th.getMessage());
        }

    }

    public long getTimestamp() {
        if (timestamp == null) {
            return 0;
        }
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public MessageType getMessageType() {
        return mMessageType;
    }

    public Integer getRetryNum() {
        if (retryNum == null) {
            return 0;
        }
        return retryNum;
    }

    public void setRetryNum(Integer retryNum) {
        this.retryNum = retryNum;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {

        mUrl = url;
    }

    public void setTracked() {
        mCalled = true;
    }

    public boolean isTracked() {
        return mCalled;
    }

    public String getEvent() {
        return mEvent;
    }

    public String getRequest_id() {
        return mRequest_id;
    }

    public enum MessageType {TRACKING_URL, QUARTILE_EVENT, TOBID_TRACKING_URL}
}
