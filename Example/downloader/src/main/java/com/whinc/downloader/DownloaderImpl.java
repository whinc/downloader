package com.whinc.downloader;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;

import static android.app.DownloadManager.ACTION_DOWNLOAD_COMPLETE;
import static android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR;
import static android.app.DownloadManager.COLUMN_REASON;
import static android.app.DownloadManager.COLUMN_STATUS;
import static android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES;
import static android.app.DownloadManager.ERROR_CANNOT_RESUME;
import static android.app.DownloadManager.ERROR_DEVICE_NOT_FOUND;
import static android.app.DownloadManager.ERROR_FILE_ALREADY_EXISTS;
import static android.app.DownloadManager.ERROR_FILE_ERROR;
import static android.app.DownloadManager.ERROR_HTTP_DATA_ERROR;
import static android.app.DownloadManager.ERROR_INSUFFICIENT_SPACE;
import static android.app.DownloadManager.ERROR_TOO_MANY_REDIRECTS;
import static android.app.DownloadManager.ERROR_UNHANDLED_HTTP_CODE;
import static android.app.DownloadManager.ERROR_UNKNOWN;
import static android.app.DownloadManager.EXTRA_DOWNLOAD_ID;
import static android.app.DownloadManager.PAUSED_QUEUED_FOR_WIFI;
import static android.app.DownloadManager.PAUSED_UNKNOWN;
import static android.app.DownloadManager.PAUSED_WAITING_FOR_NETWORK;
import static android.app.DownloadManager.PAUSED_WAITING_TO_RETRY;
import static android.app.DownloadManager.Query;
import static android.app.DownloadManager.Request;
import static android.app.DownloadManager.STATUS_FAILED;
import static android.app.DownloadManager.STATUS_PAUSED;
import static android.app.DownloadManager.STATUS_PENDING;
import static android.app.DownloadManager.STATUS_RUNNING;
import static android.app.DownloadManager.STATUS_SUCCESSFUL;

/**
 * 利用Android系统下载管理器 {@link DownloadManager} 实现 {@link Downloader} 接口<br>
 *     <br>
 * 使用时需要添加权限"android.permission.INTERNET" 和 "android.permission.WRITE_EXTERNAL_STORAGE"
 */
public class DownloaderImpl implements Downloader<DownloaderImpl> {

    private final Context mContext;
    private long mDownloadId;
    private DownloadListener mDownloadListener;
    private String mTitle;
    private String mDescription;
    private boolean mNotificationVisible;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1);
            if (mDownloadId == downloadId && mDownloadListener != null) {
                mDownloadListener.onCompleted();

                unregisterReceiver();
                unregisterContentObserver();
            }
        }
    };

    private ContentObserver mObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (mDownloadListener == null) {
                return;
            }

            DownloadManager downloadMgr = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
            Query query = new Query().setFilterById(mDownloadId);
            Cursor cursor = downloadMgr.query(query);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    dispatchStatus(cursor);       // 更新下载状态
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    };

    DownloaderImpl(Context context) {
        mContext = context;

        registerReceiver();         // 注册下载完成广播
        registerContentObserver();  // 注册下载进度监听
    }

    private void registerContentObserver() {
        Uri uri = Uri.parse("content://downloads/my_downloads");
        mContext.getContentResolver().registerContentObserver(uri, true, mObserver);
    }

    private void unregisterContentObserver() {
        mContext.getContentResolver().unregisterContentObserver(mObserver);
    }

    public void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter(ACTION_DOWNLOAD_COMPLETE);
        mContext.registerReceiver(mReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        mContext.unregisterReceiver(mReceiver);
    }

    /** 分发下载状态 */
    private void dispatchStatus(Cursor cursor) {
        int state = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
        int reasonCode = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_REASON));
        switch (state) {
            case STATUS_PENDING:
                mDownloadListener.onPending();
                break;
            case STATUS_RUNNING:
                int totalBytes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_SIZE_BYTES));
                int downloadedBytes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_BYTES_DOWNLOADED_SO_FAR));
                mDownloadListener.onRunning(downloadedBytes, totalBytes);
                break;
            case STATUS_PAUSED:
                mDownloadListener.onPaused(reasonCode, getReasonString(reasonCode));
                break;
            case STATUS_FAILED:
                mDownloadListener.onFailed(reasonCode, getReasonString(reasonCode));
                break;
            case STATUS_SUCCESSFUL:
                // 通过下面方法获取的是用户传入的保存路径
//                Uri downloadedFile = Uri.parse(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCAL_URI)));
                // 下面方法获取的是实际保存路径(如果用户指定文件已经存在,DownloadManager会自动重命名文件,这里获取重命名后的文件)
                DownloadManager downloadMgr = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
                Uri downloadedFile = downloadMgr.getUriForDownloadedFile(mDownloadId);
                mDownloadListener.onSuccessful(downloadedFile);
                break;
        }
    }

    public DownloaderImpl setDownloadListener(DownloadListener l) {
        mDownloadListener = l;
        return this;
    }

    @Override
    public DownloaderImpl setTitle(String title) {
        mTitle = title;
        return this;
    }

    @Override
    public DownloaderImpl setDescription(String description) {
        mDescription = description;
        return this;
    }

    @Override
    public DownloaderImpl setNotificationVisible(boolean b) {
        mNotificationVisible = b;
        return this;
    }

    public void download(String url, File saveFile) throws IOException, IllegalArgumentException {
        DownloadManager downloadMgr = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        Request request;
        request = new Request(Uri.parse(url));
        request.setDestinationUri(Uri.fromFile(saveFile));
        request.setTitle(mTitle);
        request.setDescription(mDescription);
        if (mNotificationVisible) {
            request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        } else {
            request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
        }
        mDownloadId = downloadMgr.enqueue(request);
    }

    private String getReasonString(int reason) {
        String result = "";
        switch (reason) {
            case ERROR_CANNOT_RESUME:
                result = "ERROR_CANNOT_RESUME";
                break;
            case ERROR_DEVICE_NOT_FOUND:
                result = "ERROR_DEVICE_NOT_FOUND";
                break;
            case ERROR_FILE_ALREADY_EXISTS:
                result = "ERROR_FILE_ALREADY_EXISTS";
                break;
            case ERROR_FILE_ERROR:
                result = "ERROR_FILE_ERROR";
                break;
            case ERROR_HTTP_DATA_ERROR:
                result = "ERROR_HTTP_DATA_ERROR";
                break;
            case ERROR_INSUFFICIENT_SPACE:
                result = "ERROR_INSUFFICIENT_SPACE";
                break;
            case ERROR_TOO_MANY_REDIRECTS:
                result = "ERROR_TOO_MANY_REDIRECTS";
                break;
            case ERROR_UNHANDLED_HTTP_CODE:
                result = "ERROR_UNHANDLED_HTTP_CODE";
                break;
            case ERROR_UNKNOWN:
                result = "ERROR_UNKNOWN";
                break;
            case PAUSED_QUEUED_FOR_WIFI:
                result = "PAUSED_QUEUED_FOR_WIFI";
                break;
            case PAUSED_UNKNOWN:
                result = "PAUSED_UNKNOWN";
                break;
            case PAUSED_WAITING_FOR_NETWORK:
                result = "PAUSED_WAITING_FOR_NETWORK";
                break;
            case PAUSED_WAITING_TO_RETRY:
                result = "PAUSED_WAITING_TO_RETRY";
                break;
        }
        return result;
    }

}
