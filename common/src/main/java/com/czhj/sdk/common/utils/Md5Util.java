package com.czhj.sdk.common.utils;

import com.czhj.sdk.logger.SigmobLog;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public final class Md5Util {

    private static final String TAG = Md5Util.class.getSimpleName();
    private static final int STREAM_BUFFER_LENGTH = 1024;

    /**
     * 对字符串做md5
     *
     * @param s 目标字符串
     */
    public static String md5(String s) {
        if (s == null) return null;
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        try {
            byte[] bytes = s.getBytes("UTF-8");
            // 获得MD5摘要算法的 MessageDigest 对象
            MessageDigest digest = MessageDigest.getInstance("md5");
            // 使用指定的字节更新摘要
            digest.update(bytes);
            //获得密文
            byte[] md = digest.digest();
            // 把密文转换成十六进制的字符串形式
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return null;
    }


    public static String fileMd5(String filepath) {
        if (filepath == null || !new File(filepath).exists()) return null;
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            fileInputStream = new FileInputStream(filepath);
            MessageDigest messageDigest = MessageDigest.getInstance("md5");
            // 使用DigestInputStream
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
            byte[] buffer = new byte[STREAM_BUFFER_LENGTH];
            while (digestInputStream.read(buffer) > 0) ;
            // 获取最终的MessageDigest
            messageDigest = digestInputStream.getMessageDigest();
            // 拿到结果，也是字节数组，包含16个元素
            byte[] resultByteArray = messageDigest.digest();
            // 同样，把字节数组转换成字符串
            return bytesToHexString(resultByteArray);
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
            try {
                if (digestInputStream != null) digestInputStream.close();
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return null;
    }


    /**
     * 文件md5
     *
     * @param file 目标文件
     */
    public static String fileMd5(File file) {
        if (file == null) return null;
        if (!file.isFile()) return null;
        MessageDigest digest = null;
        FileInputStream in = null;
        byte buffer[] = new byte[2048];
        int len;
        try {
            digest = MessageDigest.getInstance("md5");
            in = new FileInputStream(file);
            while ((len = in.read(buffer, 0, STREAM_BUFFER_LENGTH)) != -1) {
                digest.update(buffer, 0, len);
            }
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
            return null;
        } finally {
            try {
                if (digest != null) digest.clone();
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
            try {
                if (in != null) in.close();
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return bytesToHexString(digest.digest());
    }


    /**
     * 字节数组转为字符串
     *
     * @param src 目标字节数组
     */
    private static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return null;
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv);
        }
        return stringBuilder.toString();
    }


}
