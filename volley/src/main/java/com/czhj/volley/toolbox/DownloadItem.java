package com.czhj.volley.toolbox;

import com.czhj.volley.VolleyError;

public class DownloadItem {
    public String url;
    public FileType type;
    public String filePath;
    public String md5;
    public long size;
    public long networkMs;
    public int status;
    public boolean userRange = true;
    public String message;
    public VolleyError error;

    public enum FileType {
        VIDEO(1),
        PICTURE(2),
        FILE(3),
        APK(8),
        OTHER(9),
        ZIP_FILE(10),
        MRAID_VIDEO(11);


        private int mType;

        FileType(int type) {
            mType = type;
        }

        public int getType() {
            return mType;
        }
    }
}
