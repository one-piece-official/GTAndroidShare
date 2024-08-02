package com.czhj.volley.toolbox;

import android.text.TextUtils;

import com.czhj.volley.Response;
import com.czhj.volley.DefaultRetryPolicy;
import com.czhj.volley.NetworkResponse;
import com.czhj.volley.Request;
import com.czhj.volley.VolleyError;
import com.czhj.volley.VolleyLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Its purpose is provide a big file download impmenetation, suport continuous transmission
 * on the breakpoint download if server-side enable 'Content-Range' Header.
 * for example:
 * 		execute a request and submit header like this : Range=bytes=1000- (1000 means the begin point of the file).
 * 		response return a header like this Content-Range=bytes 1000-1895834/1895835, that's continuous transmission,
 * 		also return Accept-Ranges=bytes tell us the server-side supported range transmission.
 *
 * This request will stay longer in the thread which dependent your download file size,
 * that will fill up your thread poll as soon as possible if you launch many request,
 * if all threads is busy, the high priority request such as
 * might waiting long time, so don't use it alone.
 * FileDownloader maintain a download task queue, let's set the maximum parallel request count, the rest will await.
 *
 * By the way, this request priority was {@link Priority#LOW}, higher request will jump ahead it.
 */
public class FileDownloadRequest extends Request<DownloadItem> {
    private File mStoreFile;
    private File mTemporaryFile;

    private DownloadItem mDownloadItem;
    private long tempFileSize = 0;
    /**
     * Lock to guard mListener as it is cleared on cancel() and read on delivery.
     */
    private final Object mLock = new Object();



    public  interface FileDownloadListener {

         void onSuccess(DownloadItem item);

         void onCancel(DownloadItem item);

         void onErrorResponse(DownloadItem item);

         void downloadProgress(DownloadItem item, long totalSize, long readSize);

    }


    private FileDownloadRequest.FileDownloadListener mListener = null;


    public void setListener(FileDownloadListener mListener) {
        this.mListener = mListener;
    }

    public FileDownloadRequest(DownloadItem item,  FileDownloadRequest.FileDownloadListener listener) {
        super(Method.GET,item.url, null);
        mStoreFile = new File(item.filePath);

        if(mStoreFile.getParentFile() != null && !mStoreFile.getParentFile().exists())
            mStoreFile.getParentFile().mkdirs();

        if(mStoreFile.exists()){
            mStoreFile.delete();
        }

        mDownloadItem = item;
        mTemporaryFile = new File(item.filePath + ".tmp");

        if(!item.userRange){
            mTemporaryFile.delete();
        }
        mListener = listener;
        // Turn the retries frequency greater.
        setRetryPolicy(new DefaultRetryPolicy(10*1000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        setShouldCache(false);
        VolleyLog.d("FileDownloadRequest()  [ %s ], url = [%s]",item.filePath,item.url);
    }

    @Override
    public Map<String, String> getHeaders() {

        Map<String,String> headers =  new HashMap<>();
        tempFileSize =  mTemporaryFile.length();
        headers.put("Connection","Keep-Alive");
        headers.put("Accept-Encoding","gzip");
       headers.put("Range","bytes="+ tempFileSize+ "-");
       addMarker("Range,bytes="+ tempFileSize+"-");
       return headers;
    }

    /** Ignore the response content, just rename the TemporaryFile to StoreFile. */
    @Override
    protected Response<DownloadItem> parseNetworkResponse(NetworkResponse response) {
        mDownloadItem.networkMs = response.networkTimeMs;

        if (!isCanceled()) {
            Map<String, String> headers = FileDownloadNetwork.convertHeaders(response.allHeaders);
            long length = 0;
            if(headers.containsKey("Transfer-Encoding")&& headers.get("Transfer-Encoding").equalsIgnoreCase("chunked")){
                length =  mTemporaryFile.length();
            }else if(headers.containsKey("content-length")){
                length  =  Long.parseLong(headers.get("content-length")) + tempFileSize;
            }

            mDownloadItem.size = length;
            if (mTemporaryFile.canRead() && mTemporaryFile.length()>0 && (mTemporaryFile.length() == length || length == 0)) {

                if(HttpHeaderParser.isGzipContent(headers)) {
                    InputStream in = null;
                    FileOutputStream out = null;
                    int readlength;
                    boolean result;

                    try {

                        in = new GZIPInputStream(new FileInputStream(mTemporaryFile));

                        out = new FileOutputStream(mStoreFile);
                        byte[] buffer = new byte[4096];

                        while ((readlength = in.read(buffer, 0, 4096)) != -1)
                            out.write(buffer, 0, readlength);

                        result = true;
                    } catch (IOException e) {
                        VolleyLog.e(e.getMessage());
                        result = false;
                    } finally {
                        if(in != null) {
                            try {
                                in.close();
                            } catch (IOException e) {
                                VolleyLog.e(e.getMessage());
                            }
                        }
                        if(out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                VolleyLog.e(e.getMessage());
                            }
                        }
                    }
                    mTemporaryFile.delete();
                    if(result)
                        return Response.success(mDownloadItem,null);
                    else
                        return Response.error(new VolleyError("error gzip unzip the download temporary file!"));

                } else {
                    if (mTemporaryFile.renameTo(mStoreFile)) {
                        return Response.success(mDownloadItem, null);
                    } else {
                        return Response.error(new VolleyError("Can't rename the download temporary file!"));
                    }

                }
            } else if(mStoreFile.canRead() && mStoreFile.length() == length){
                return Response.success(mDownloadItem, null);
            } else{
                return Response.error(new VolleyError("Download temporary file was invalid!"+mTemporaryFile.getAbsolutePath()));
            }
        }
        return Response.error(new VolleyError("Request was Canceled!"));
    }

    @Override
    protected void deliverResponse(DownloadItem item) {
        mDownloadItem.status = 1;
        mListener.onSuccess(item);
    }

    @Override
    public void deliverError(VolleyError error) {

        mDownloadItem.status = 0;
        if(mStoreFile.exists()){
            mStoreFile.delete();
        }
        if(mTemporaryFile.exists()){
            mTemporaryFile.delete();
        }

        mDownloadItem.error = error;
        mListener.onErrorResponse(mDownloadItem);
    }

    /**
     * In this method, we got the Content-Length, with the TemporaryFile length,
     * we can calculate the actually size of the whole file, if TemporaryFile not exists,
     * we'll take the store file length then compare to actually size, and if equals,
     * we consider this download was already done.
     * We used {@link RandomAccessFile} to continue download, when download success,
     * the TemporaryFile will be rename to StoreFile.
     */
    public byte[] handleRawResponse(HttpResponse response) throws IOException {

            InputStream in = null;
            RandomAccessFile tmpFileRaf =null;

            Map<String, String> headers = FileDownloadNetwork.convertHeaders(response.getHeaders());
            boolean isSupportRange = HttpHeaderParser.isSupportRange(headers);
            long downloadedSize = 0;
            long fileSize = response.getContentLength();

            if(headers.containsKey("Transfer-Encoding") && headers.get("Transfer-Encoding").equalsIgnoreCase("chunked")){
                VolleyLog.d("Response doesn't present Content-Length!");
            }else if(fileSize >= 0 && headers.containsKey("Content-Length")) {

                if (fileSize == 0 && mStoreFile.exists() && mStoreFile.length() == fileSize) {
                    mStoreFile.renameTo(mTemporaryFile);
                    response.getContent().close();
                    return new byte[0];

                }

                if (isSupportRange) {
                    downloadedSize = mTemporaryFile.length();
                    fileSize += downloadedSize;

                    // Verify the Content-Range Header, to ensure temporary file is part of the whole file.
                    // Sometime, temporary file length add response content-length might greater than actual file length,
                    // in this situation, we consider the temporary file is invalid, then throw an exception.
                    String realRangeValue = HttpHeaderParser.getHeader(headers, "Content-Range");
                    VolleyLog.d("Content-Range %s", realRangeValue);
                    // response Content-Range may be null when "Range=bytes=0-"
                    if (!TextUtils.isEmpty(realRangeValue)) {
                        String assumeRangeValue = "bytes " + downloadedSize + "-" + (fileSize - 1);
                        if (TextUtils.indexOf(realRangeValue, assumeRangeValue) == -1) {
                            response.getContent().close();
                            mTemporaryFile.delete();
                            throw new IllegalStateException("The Content-Range Header is invalid Assume[" + assumeRangeValue + "] vs Real[" + realRangeValue + "], "
                                    + "has remove the temporary file [" + mTemporaryFile + "].");
                        }
                    }
                }

            }else {
                downloadedSize = mTemporaryFile.length();
            }


            try {

                tmpFileRaf = new RandomAccessFile(mTemporaryFile, "rw");

                if(tmpFileRaf != null){
                    if (isSupportRange) {

                        tmpFileRaf.seek(downloadedSize);
                    } else {
                        // If not, truncate the temporary file then start download from beginning.
                        tmpFileRaf.setLength(0);
                        downloadedSize = 0;
                    }
                    in = response.getContent();


                    byte[] buffer = new byte[8 * 1024]; // 6K buffer
                    int offset;

                    while ((offset = in.read(buffer)) != -1) {


                        if (isCanceled()) {
                            VolleyLog.v(mDownloadItem.url +" download  is cancel");
                            break;
                        }

                        tmpFileRaf.write(buffer, 0, offset);

                        downloadedSize += offset;

                        if (mListener != null){
                            try {
                                mListener.downloadProgress(mDownloadItem,fileSize,downloadedSize);

                            }catch (Throwable throwable){
                                VolleyLog.e("callback downloadProgress  error "+throwable.getMessage());
                            }
                        }
                        VolleyLog.d("recv: "+ downloadedSize + " total: "+fileSize + " offset "+ offset);
                    }
                    VolleyLog.d("recv: "+ downloadedSize + " total: "+fileSize + " offset "+ offset);

                }
        } catch (Throwable throwable) {
                throw  throwable;
        } finally {

            try {
                // Close the InputStream and release the resources by "consuming the content".
                if (in!= null) in.close();
            } catch (Throwable e) {
                // This can happen if there was an exception above that left the entity in
                // an invalid state.
                VolleyLog.v("Error occured when calling consumingContent");
            }

            if(tmpFileRaf != null) tmpFileRaf.close();
        }

        // If server-side support range download, we seek to last point of the temporary file.
        return new byte[0];


    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }

    @Override
    public void cancel() {
        super.cancel();
        synchronized (mLock) {
            mListener = null;
        }
    }

}

