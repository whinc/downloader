package com.whinc.example;

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
     * Create a directory which is relative to the external storage public directory
     * if it is not exists. equal to {@code }<br>
     *     <pre>Environment.getExternalStorageDirectory().getAbsolutePath() + ['/'] + relativePath</pre>
     *
     * input relative path will add correct prefix '/' if it is needed
     *
     * @param relativePath directory path relative to external storage directory
     * @return created dir absolute path.
     */
    public static String createRelativeDir(String relativePath) throws IOException {
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        StringBuilder builder = new StringBuilder(rootPath);
        if (rootPath.endsWith(File.separator)) {
            builder.deleteCharAt(builder.length() - 1);
        }
        if (!relativePath.startsWith(File.separator)) {
            builder.append(File.separator);
        }
        builder.append(relativePath);

        String absolutePath = builder.toString();
        File dir = new File(absolutePath);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create dir!");
        }
        return absolutePath;
    }

    /** Create a file in external storage
     * @param relativePath path relative to <pre>Environment.getExternalStorageDirectory().getAbsolutePath()</pre>
     * */
    public static File createRelativeFile(String relativePath) throws IOException {
        int lastSlashPos = relativePath.lastIndexOf(File.separator);
        String relativeDir = relativePath.substring(0, lastSlashPos + 1);
        String filename = relativePath.substring(lastSlashPos + 1, relativePath.length());

        File relativeFile =  new File(createRelativeDir(relativeDir) + filename);
        if (!relativeFile.exists() && !relativeFile.createNewFile()) {
            throw new IOException("Cannot create file!");
        }
        return relativeFile;
    }
}
