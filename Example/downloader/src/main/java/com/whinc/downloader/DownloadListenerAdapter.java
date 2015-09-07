package com.whinc.downloader;

import android.net.Uri;

/**
 * Created by wuhui on 9/6/15.
 */
public class DownloadListenerAdapter implements DownloadListener {
    @Override
    public void onCompleted() {

    }

    @Override
    public void onSuccessful(Uri downloadedFile) {

    }

    @Override
    public void onFailed(int reasonCode, String reason) {

    }

    @Override
    public void onPending() {

    }

    @Override
    public void onRunning(int downloadedBytes, int totalBytes) {

    }

    @Override
    public void onPaused(int reasonCode, String reason) {

    }
}
