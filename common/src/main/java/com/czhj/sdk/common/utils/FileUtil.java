package com.czhj.sdk.common.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.czhj.sdk.logger.SigmobLog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public final class FileUtil {
    /**
     * 删除目录及目录下的文件
     *
     * @param dir 要删除的目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public static boolean deleteDirectory(String dir) {
        try {
            // 如果dir不以文件分隔符结尾，自动添加文件分隔符
            if (!dir.endsWith(File.separator)) dir = dir + File.separator;
            File dirFile = new File(dir);
            // 如果dir对应的文件不存在，或者不是一个目录，则退出
            if ((!dirFile.exists()) || (!dirFile.isDirectory())) {
                SigmobLog.d("删除目录失败：" + dir + "不存在！");
                return false;
            }
            boolean flag = true;
            // 删除文件夹中的所有文件包括子目录
            File[] files = dirFile.listFiles();
            for (File file : files) {
                // 删除子文件
                if (file.isFile()) {
                    flag = deleteFile(file.getAbsolutePath());
                    if (!flag) break;
                }
                // 删除子目录
                else if (file.isDirectory()) {
                    flag = deleteDirectory(file.getAbsolutePath());
                    if (!flag) break;
                }
            }
            if (!flag) {
                return false;
            }
            // 删除当前目录
            if (dirFile.delete()) {
                SigmobLog.d("删除目录" + dir + "成功！");
                return true;
            } else {
                return false;
            }

        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }

        return false;

    }


    public static String getRealFilePath(Context context, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null) data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Images.ImageColumns.DATA}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


    /**
     * 删除单个文件
     *
     * @param fileName 要删除的文件的文件名
     * @return 单个文件删除成功返回true，否则返回false
     */
    public static boolean deleteFile(String fileName) {


        try {
            SecurityManager checker = new SecurityManager();

            checker.checkDelete(fileName);
            File file = new File(fileName);
            // 如果文件路径所对应的文件存在，并且是一个文件，则直接删除
            if (file.exists() && file.isFile()) {
                if (file.delete()) {
                    SigmobLog.d("删除单个文件" + fileName + "成功！");
                    return true;
                } else {
                    SigmobLog.d("删除单个文件" + fileName + "失败！");
                    return false;
                }
            } else {
                SigmobLog.d("删除单个文件失败：" + fileName + "不存在！");
                return false;
            }
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return false;
    }


    /***
     * 获取文件扩展名
     * @param filename
     * @return 返回文件扩展名
     */
    public static String getExtensionName(String filename) {
        if ((filename != null) && (filename.length() > 0)) {
            int dot = filename.lastIndexOf('.');
            if ((dot > -1) && (dot < (filename.length() - 1))) {
                return filename.substring(dot + 1);
            }
        }
        return filename;
    }

    public static void writeToBuffer(byte[] content, String path) {
        FileOutputStream fos = null;
        ObjectOutput objectOutput = null;

        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            fos = new FileOutputStream(file);

            fos.write(content);
            SigmobLog.d("writeCache :" + file.getName());
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
                if (null != objectOutput) {
                    objectOutput.close();
                }
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        }
    }

    public static boolean writeToCache(InputStream inputStream, String path) {
        FileOutputStream fos = null;
        File file = null;
        try {
            file = new File(path);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            fos = new FileOutputStream(file);

            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[1024];
            int bytesRead;
            long total = 0;
            while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                total += bytesRead;
                fos.write(buffer, 0, bytesRead);
            }
            fos.getFD().sync();
            if (total == 0) {
                file.delete();
            }
            SigmobLog.d("writeCache :" + file.getName());
            return true;
        } catch (Throwable e) {
            if (file != null && file.exists()) {
                file.delete();
            }
            SigmobLog.e(e.getMessage());
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return false;
    }


    public static boolean writeToCache(Object object, String path) {
        FileOutputStream fos = null;
        ObjectOutput objectOutput = null;

        try {
            File file = new File(path);
            if (!file.getParentFile().exists()) file.getParentFile().mkdirs();

            fos = new FileOutputStream(file);
            objectOutput = new ObjectOutputStream(fos);
            if (objectOutput != null) {
                objectOutput.writeObject(object);
            }
            SigmobLog.d("writeCache :" + file.getName());
            return true;
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        } finally {
            try {
                if (null != fos) {
                    fos.close();
                }
                if (null != objectOutput) {
                    objectOutput.close();
                }
            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return false;
    }

    public static Object readFromCache(String path) {
        File file = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object object = null;

        try {
            //文件路径最好是灵活配置
            file = new File(path);
            if (!file.exists()) {
                return null;
            }
            //这里还要判断文件是否存在
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            if (null != ois) {
                //获取到WenkuBanner对象
                object = ois.readObject();

            }

        } catch (Throwable e) {
            if (file != null && file.exists()) {
                file.delete();
            }

            SigmobLog.e(e.getMessage());
        } finally {
            try {
                if (null != fis) {
                    fis.close();

                }
                if (null != ois) {
                    ois.close();
                }

            } catch (Throwable e) {
                SigmobLog.e(e.getMessage());
            }
        }
        return object;
    }

    public static byte[] readBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return buffer;
    }


    /**
     * 读取指定文件的输出
     *
     * @param path
     */
    public static String readFileToString(File path) {
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(path), 8192);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append("\n").append(line);
            }
            bufferedReader.close();
            return sb.toString();
        } catch (Throwable e) {
            SigmobLog.e(e.getMessage());
        }
        return null;
    }


    public static File[] orderByDate(String filePath) {
        File file = new File(filePath);
        File[] fs = file.listFiles();
        if (fs != null) {
            Arrays.sort(fs, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    long diff = f1.lastModified() - f2.lastModified();
                    if (diff > 0) return 1;
                    else if (diff == 0) return 0;
                    else return -1;
                }

                public boolean equals(Object obj) {
                    return true;
                }

            });
        }

        return fs;
    }


    public static File[] clearCacheFileByCount(File[] files, int count) {

        if (files == null || files.length == 0) return null;
        ArrayList<File> fileList = new ArrayList<>(Arrays.asList(files));


        for (int i = 0; i < files.length; i++) {

            if (fileList.size() <= count) {
                break;
            }

            File file = files[i];
            if (file.exists()) {
                file.delete();
                fileList.remove(file);
                SigmobLog.d("file delete " + file.getName());
            }
        }

        return fileList.toArray(new File[0]);
    }
}
