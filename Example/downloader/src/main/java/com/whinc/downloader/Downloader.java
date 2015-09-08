package com.whinc.downloader;

import java.io.File;
import java.io.IOException;

/**
 * Created by wuhui on 9/6/15.
 */
public interface Downloader<T extends Downloader> {
    T setDownloadListener(DownloadListener l);
    T setTitle(String title);
    T setDescription(String description);
    T setNotificationVisible(boolean b);

    /**
     * download specified resource and save to specified local sdcard
     * @param url resource location
     * @param savePath save path
     * @throws IOException if sdcard cannot access or broken or space is insufficient etc.
     * @throws IllegalArgumentException if url is a illegal format
     */
    void download(String url, File savePath) throws IOException, IllegalArgumentException;
}
