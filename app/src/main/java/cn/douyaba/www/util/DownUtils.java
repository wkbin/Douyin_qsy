package cn.douyaba.www.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * Author: 王克斌
 * Date: 2019 年 08 月 22 日 上午 10:39
 * Description:
 */
public class DownUtils {
    public interface OnDownloadListener{
        void downStart();
        void load(int progress);
        void downEnd();
        void downFailure();
    }
    public OnDownloadListener onDownloadListener;

    public void setOnDownloadListener(OnDownloadListener onDownloadListener) {
        this.onDownloadListener = onDownloadListener;
    }

    //下载
    public  void download(final Context context, final String url) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                new Handler(context.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        onDownloadListener.downFailure();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final long startTime = System.currentTimeMillis();
                Log.i("DOWNLOAD", "startTime=" + startTime);
                sendOkHttpRequest(url, new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                        new Handler(context.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                onDownloadListener.downFailure();
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        InputStream is = null;
                        FileOutputStream fos = null;
                        int len = 0;
                        byte[] buf = new byte[1024];
                        BufferedSink bufferedSink = null;
                        try {
                            String mSDCardPath = Environment.getExternalStorageDirectory().getAbsolutePath();//SD卡路径
                            File dest = new File(mSDCardPath, url.substring(url.lastIndexOf("/") + 1));
                            is = response.body().byteStream();
                            long total = response.body().contentLength();
                            fos = new FileOutputStream(dest);
                            long sum = 0;
                            while ((len = is.read(buf)) != -1) {
                                fos.write(buf, 0, len);
                                sum += len;
                                int progress = (int) (sum * 1.0f / total * 100);
                                onDownloadListener.load(progress);
                            }
                            fos.flush();



                            Log.i("DOWNLOAD", "download success");
                            Log.i("DOWNLOAD", "totalTime=" + (System.currentTimeMillis() - startTime));


                            new Handler(context.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    onDownloadListener.downEnd();
                                }
                            });

                            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(String.valueOf(dest))));
                            //获取ContentResolve对象，来操作插入视频
                            ContentResolver localContentResolver = context.getContentResolver();
                            //ContentValues：用于储存一些基本类型的键值对
                            ContentValues localContentValues = getVideoContentValues(context,dest, System.currentTimeMillis());
                            //insert语句负责插入一条新的纪录，如果插入成功则会返回这条记录的id，如果插入失败会返回-1。
                            localContentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, localContentValues);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.i("DOWNLOAD", "download failed");
                            onDownloadListener.downFailure();
                        } finally {
                            if (bufferedSink != null) {
                                bufferedSink.close();
                            }
                        }
                    }
                });
            }
        });
    }
    /**
     * 视频存在本地
     *
     * @param paramContext
     * @param paramFile
     * @param paramLong
     * @return
     */
    public static ContentValues getVideoContentValues(Context paramContext, File paramFile, long paramLong) {
        ContentValues localContentValues = new ContentValues();
        localContentValues.put("title", paramFile.getName());
        localContentValues.put("_display_name", paramFile.getName());
        localContentValues.put("mime_type", "video/mp4");
        localContentValues.put("datetaken", Long.valueOf(paramLong));
        localContentValues.put("date_modified", Long.valueOf(paramLong));
        localContentValues.put("date_added", Long.valueOf(paramLong));
        localContentValues.put("_data", paramFile.getAbsolutePath());
        localContentValues.put("_size", Long.valueOf(paramFile.length()));
        return localContentValues;
    }

    public void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }
}
