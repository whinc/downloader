package com.whinc.downloader;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

/**
 * Created by wuhui on 9/2/15.
 */
public class StorageUtils {

    /** Is external storage readable and writable */
    public static boolean isExternalStorageMounted() {
        // Note: must use equals() method instead of == to verify state, because
        // state is a String type.
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    /**
     * Create a directory on external storage if it is not exists.<br>
     *
     * input "/dir1/dir2/" or "dir1/dir2/" or "dir1/dir2" will
     * return "path_to_external_dir/dir1/dir2/"<br>
     *
     * @param relativePath directory path relative to external storage directory
     * @return created dir absolute path.
     */
    public static String createDir(String relativePath) throws IOException {
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        StringBuilder builder = new StringBuilder(rootPath);
        if (rootPath.endsWith(File.separator)) {
            builder.deleteCharAt(builder.length() - 1);
        }
        if (!relativePath.startsWith(File.separator)) {
            builder.append(File.separator);
        }
        builder.append(relativePath);
        if (!relativePath.endsWith(File.separator)) {
            builder.append(File.separator);
        }

        String absolutePath = builder.toString();
        File dir = new File(absolutePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create dir!");
        }
        return absolutePath;
    }
}
