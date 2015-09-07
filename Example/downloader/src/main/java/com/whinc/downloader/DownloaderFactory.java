package com.whinc.downloader;

import android.content.Context;

/**
 * Created by wuhui on 9/6/15.
 */
public class DownloaderFactory {

    /** create a new {@link Downloader} instance */
    public static Downloader create(Context context) {
        return new DownloaderImpl(context);
    }
}
