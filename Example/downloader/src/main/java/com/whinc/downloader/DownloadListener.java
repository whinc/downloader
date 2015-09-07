package com.whinc.downloader;

import android.net.Uri;

/**
 * Created by wuhui on 9/6/15.
 */
public interface DownloadListener {
    /**
     * Called on complete download task, it may be successful or failed.
     */
    void onCompleted();

    void onSuccessful(Uri downloadedFile);

    void onFailed(int reasonCode, String reason);

    void onPending();

    void onRunning(int downloadedBytes, int totalBytes);

    void onPaused(int reasonCode, String reason);
}
