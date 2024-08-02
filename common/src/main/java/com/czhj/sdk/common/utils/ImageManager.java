package com.czhj.sdk.common.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.LruCache;
import android.widget.ImageView;

import com.czhj.sdk.common.ThreadPool.BackgroundThreadFactory;
import com.czhj.sdk.common.ThreadPool.ThreadPoolFactory;
import com.czhj.sdk.logger.SigmobLog;
import com.czhj.volley.toolbox.StringUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ImageManager {

    /**
     * 获取对象的单例模式
     */
    private static ImageManager instance;
    private Context mContext;
    private final String cachePath = "SigImageCache";
    /**
     * 构建出线程池，4条线程
     */
    private ExecutorService executorService = ThreadPoolFactory.getFixIOExecutor();
    /**
     * 内存储存图片的集合 使用lrucache缓存图片,这里不能申明在方法里，不然会被覆盖掉 4兆的大小作为缓存 SoftReference
     * 为软引用、内存不足时，系统自动回收。
     */
    private LruCache<String, Bitmap> imageCache = new LruCache<String, Bitmap>(1024 * 1024 * 4);
    private Handler handler = new Handler(Looper.getMainLooper());
    private File customCachePath;
    private ImageView mImageView;

    public ImageManager(Context context) {
        mContext = context.getApplicationContext();
    }

    private static ImageManager getInstance(Context context) {

        if (instance == null) {
            synchronized (ImageManager.class) {
                if (instance == null) {
                    instance = new ImageManager(context);

                }
            }
        }
        return instance;
    }

    /***
     * 初始化对象
     *
     * @param context
     * @return
     */
    public static ImageManager with(Context context) {

        ImageManager imageManager = getInstance(context);
        return imageManager;
    }

    public ImageManager customCachePath(File cachePath) {
        customCachePath = cachePath;
        return this;
    }

    /***
     * 加载图片的url地址，返回RequestCreator对象
     *
     * @param url
     * @return
     */
    public RequestCreatorRunnable load(String url) {
        return new RequestCreatorRunnable(url);
    }

    /***
     * 读取缓存路径目录
     *
     * @return
     */
    private File getCacheDir() {
        if (customCachePath != null && customCachePath.isDirectory() && customCachePath.exists()) {

            return customCachePath;
        } else {
            // 获取保存的文件夹路径
            File file;
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                // 有SD卡就保存到SD卡
                file = new File(mContext.getExternalCacheDir(), cachePath);
            } else {
                // 没有就保存到内部存储
                file = new File(mContext.getCacheDir(), cachePath);
            }
            if (!file.exists()) {
                file.mkdirs();
            }
            return file;
        }
    }

    public void clearCache() {

        try {
            File[] files = FileUtil.orderByDate(getCacheDir().getAbsolutePath());

            files = FileUtil.clearCacheFileByCount(files, 100);

            if (files == null) {
                SigmobLog.i("native ad file list is null");
            } else {
                SigmobLog.i("native ad file remain num: " + files.length);
            }
        } catch (Throwable throwable) {

            SigmobLog.e("clean native ad file error", throwable);
        }

    }

    public void getBitmap(final String imgUrl, final BitmapLoadedListener listener) {
        if (TextUtils.isEmpty(imgUrl)) {
            return;
        }
        final String url = StringUtil.getUrl(imgUrl);
        // 1.去内存之中在找
        Bitmap reference = imageCache.get(url);
        Bitmap cacheBitmap = null;
        if (reference != null) {
            cacheBitmap = reference;
            // 有图片就显示图片
            listener.onBitmapLoaded(cacheBitmap);
            return;
        }
        // 2.去本地硬盘中找
        // 从url中获取文件名字
        String fileName = url.substring(url.lastIndexOf("/") + 1);
        File file = new File(getCacheDir(), Md5Util.md5(fileName));
        // 确保路径没有问题
        if (file.exists() && file.length() > 0) {
            // 返回图片
            cacheBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
        }
        if (cacheBitmap != null) {
            // 保存到内存中去
            imageCache.put(url, cacheBitmap);
            listener.onBitmapLoaded(cacheBitmap);
            return;
        }
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                URL loadUrl = null;
                try {
                    loadUrl = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) loadUrl.openConnection();
                    // 设置请求方式
                    conn.setRequestMethod("GET");
                    // 设置请求时间
                    conn.setConnectTimeout(2000);

                    if (conn.getResponseCode() == 200) {
                        InputStream is = conn.getInputStream();
                        // 获取到图片进行显示
                        final Bitmap bm = BitmapFactory.decodeStream(is);
                        conn.disconnect();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                listener.onBitmapLoaded(bm);
                            }
                        });
                        // 3.1保存到内存中
                        imageCache.put(url, bm);
                        // 3.2保存到磁盘
                        // 从url中获取文件名字
                        String fileName = url.substring(url.lastIndexOf("/") + 1);
                        // 获取存储路径
                        File file = new File(getCacheDir(), Md5Util.md5(fileName));
                        FileOutputStream os = new FileOutputStream(file);
                        // 将图片转换为文件进行存储
                        bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                    }
                } catch (Exception e) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onBitmapLoadFailed();
                        }
                    });
                }
            }
        });
    }

//    public static Bitmap decodeBitmapFromFile(String pathName, int reqWidth, int reqHeight) {
//        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeFile(pathName, options);
//        // 调用上面定义的方法计算inSampleSize值
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//        // 使用获取到的inSampleSize值再次解析图片
//        options.inJustDecodeBounds = false;
//        return BitmapFactory.decodeFile(pathName, options);
//    }
//
//    public static Bitmap decodeBitmapFromStream(InputStream is, int reqWidth, int reqHeight) {
//        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
//        final BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        BitmapFactory.decodeStream(is, null, options);
//        // 调用上面定义的方法计算inSampleSize值
//        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
//        // 使用获取到的inSampleSize值再次解析图片
//        options.inJustDecodeBounds = false;
//        return BitmapFactory.decodeStream(is, null, options);
//    }
//
//    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
//        // 源图片的高度和宽度
//        final int height = options.outHeight;
//        final int width = options.outWidth;
//        int inSampleSize = 1;
//        if (height > reqHeight || width > reqWidth) {
//            // 计算出实际宽高和目标宽高的比率
//            final int heightRatio = Math.round((float) height / (float) reqHeight);
//            final int widthRatio = Math.round((float) width / (float) reqWidth);
//            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
//            // 一定都会大于等于目标的宽和高。
//            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
//        }
//        return inSampleSize;
//    }


    public interface BitmapLoadedListener {
        void onBitmapLoaded(Bitmap bitmap);

        void onBitmapLoadFailed();
    }

    /**
     * 创建者
     */
    public class RequestCreatorRunnable implements Runnable {

        String url;
        int holderResId;
        int errorResId;
        ImageView imageView;

        // 初始化图片的url地址
        public RequestCreatorRunnable(String url) {

            this.url = StringUtil.getUrl(url);
        }

        /***
         * 设置默认图片，占位图片
         *
         * @param holderResId
         * @return
         */
        public RequestCreatorRunnable placeholder(int holderResId) {
            this.holderResId = holderResId;
            return this;
        }

        /***
         * 发生错误加载的图片
         *
         * @param errorResId
         * @return
         */
        public RequestCreatorRunnable error(int errorResId) {

            this.errorResId = errorResId;
            return this;
        }

        public void into(ImageView imageView) {
            this.imageView = imageView;
            //设置占位图片
            if (holderResId != 0 && this.imageView != null) {
                this.imageView.setImageResource(holderResId);
            }
            if (TextUtils.isEmpty(url)) {
                return;
            }
            // 1.去内存之中在找
            Bitmap reference = imageCache.get(url);
            Bitmap cacheBitmap;
            if (reference != null) {
                cacheBitmap = reference;
                // 有图片就显示图片
                imageView.setImageBitmap(cacheBitmap);
                return;
            }

            // 2.去本地硬盘中找
            Bitmap diskBitmap = getBitmapFile();
            if (diskBitmap != null) {
                // 本地磁盘有就显示图片
                imageView.setImageBitmap(diskBitmap);
                // 保存到内存中去
                imageCache.put(url, diskBitmap);
                return;
            }
            // 3.连接网络请求数据
            executorService.submit(this);
            return;

        }

        @Override
        public void run() {
            // 子线程
            // 处理网络请求
            URL loadUrl;
            try {
                loadUrl = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) loadUrl.openConnection();
                // 设置请求方式
                conn.setRequestMethod("GET");
                // 设置请求时间
                conn.setConnectTimeout(2000);
                if (conn.getResponseCode() == 200) {
                    InputStream is = conn.getInputStream();
                    // 获取到图片进行显示
                    final Bitmap bm = BitmapFactory.decodeStream(is);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // 主线程
                            imageView.setImageBitmap(bm);
                        }
                    });
                    // 3.1保存到内存中
                    imageCache.put(url, bm);
                    // 3.2保存到磁盘
                    // 从url中获取文件名字setImageBitmap
                    String fileName = url.substring(url.lastIndexOf("/") + 1);
                    // 获取存储路径
                    File file = new File(getCacheDir(), Md5Util.md5(fileName));
                    FileOutputStream os = new FileOutputStream(file);
                    // 将图片转换为文件进行存储
                    bm.compress(Bitmap.CompressFormat.PNG, 100, os);
                } else {
                    // 联网失败，显示失败图片
                    showError();
                }
            } catch (FileNotFoundException e) {

            } catch (Exception e) {
                e.printStackTrace();
                // 显示错误的图片
                showError();
            }

        }

        /***
         * 获取文件中的图片
         *
         * @return
         */
        private Bitmap getBitmapFile() {
            // 从url中获取文件名字
            String fileName = url.substring(url.lastIndexOf("/") + 1);
            File file = new File(getCacheDir(), Md5Util.md5(fileName));
            // 确保路径没有问题
            if (file.exists() && file.length() > 0) {
                // 返回图片
                return BitmapFactory.decodeFile(file.getAbsolutePath());

            } else {

                return null;
            }
        }

        /***
         * 显示错误的图片
         */
        private void showError() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (errorResId != 0 && imageView != null) {
                        imageView.setImageResource(errorResId);
                    }
                }
            });
        }
    }

}