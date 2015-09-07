package com.whinc.example;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * App崩溃异常捕获和处理<br>
 *
 *     使用示例:
 *     <pre>
 *         CrashHandler.with(context).setLogDir("path/to/log/").setOnExitingListener(l);
 *     </pre>
 */
public class CrashHandler implements Thread.UncaughtExceptionHandler{
    private static final String LOG_TAG = "CrashHandler";

    private static final CrashHandler singleton = new CrashHandler();
    private Context mContext = null;
    private String mLogDir = null;        // 日志默认保存目录

    private static Thread.UncaughtExceptionHandler mDefaultHandler = null; // 程序未捕获异常的默认处理器
    private OnExitingListener mOnExitingListener = null;
    private StringBuilder mCustomInfo = new StringBuilder();

    // 防止实例化
    private CrashHandler() {}

    /** 初始化并安装CrashHandler，返回CrashHandler实例，可通过返回的CrashHandler对象设置各种属性 */
    public static CrashHandler with(Context c) {
        singleton.mContext = c;
        String defaultLogDir = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + c.getString(c.getApplicationInfo().labelRes)
                + File.separator
                + "log"
                + File.separator;
        singleton.mLogDir = defaultLogDir;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(singleton);
        return singleton;
    }

    /** 添加自定义信息，该信息会保存到日志文件的开头 */
    public CrashHandler addCustomInfo(CharSequence info) {
        mCustomInfo.append(info);
        return this;
    }

    /** 设置监听器，CrashHandler完成日志收集保存之后退出App之前执行该监听器方法 */
    public CrashHandler setOnExitingListener(OnExitingListener l) {
        mOnExitingListener = l;
        return this;
    }

    /** 设置日志保存目录
     * @param logDir 日志保存目录的绝对路径
     * */
    public CrashHandler setLogDir(String logDir) {
        mLogDir = logDir;
        return this;
    }

    /** 设置日志保存目录
     * @param relativeLogDir 相对外部存储器根目录的路径
     * */
    public CrashHandler setRelativeLogDir(String relativeLogDir) {
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        if (rootPath.endsWith(File.separator)) {
            rootPath = rootPath.substring(0, rootPath.length() - 1);
        }
        if (relativeLogDir.startsWith(File.separator)) {
            relativeLogDir = relativeLogDir.substring(1, relativeLogDir.length());
        }
        return setLogDir(rootPath + File.separator + relativeLogDir);
    }

    /** 获取日志保存目录 */
    public String getLogDir() {
        return mLogDir;
    }

    /**
     * 获取日志目录保存的所有日志文件
     * @return 返回所有日志文件
     */
    public File[] logFileList() {
        File dir = new File(mLogDir);
        if (!dir.exists()) {
            return new File[0];
        }
        return dir.listFiles();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }


    /**
     * 处理异常
     * @param ex {@link Throwable}
     * @return 如果用户处理了异常返回true，否则返回false
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }

        StringBuilder builder = new StringBuilder();
        // 自定义信息
        builder.append(mCustomInfo).append("\n");
        // 堆栈信息
        String stackTrace = getStackTrace(ex);
        builder.append(stackTrace);
        // 设备信息
        Map<String, String> deviceInfo = collectDeviceInfo(mContext);
        for (String key : deviceInfo.keySet()) {
            builder.append(key).append(":").append(deviceInfo.get(key)).append("\n");
        }
        // 保存日志
        String errLog = builder.toString();
        String savePath = saveCrashLog(errLog);
        if (mOnExitingListener != null) {
            mOnExitingListener.handle(savePath, errLog);
        }
        Log.d(LOG_TAG, "save path:" + savePath);

        return true;
    }

    /**
     * 搜集设备相关信息
     * @param appContext {@link Context}
     * @return 以键-值对形式返回设备参数信息
     */
    private Map<String, String> collectDeviceInfo(Context appContext) {
        Map<String, String> deviceInfo = new HashMap<String, String>();
        PackageManager packageManager = appContext.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(appContext.getPackageName(), PackageManager.GET_ACTIVITIES);
            deviceInfo.put("version name", packageInfo.versionName);
            deviceInfo.put("version code", String.valueOf(packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        Field[] fields= Build.class.getFields();
        for (Field f : fields) {
            try {
                deviceInfo.put(f.getName(), f.get(null).toString());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return deviceInfo;
    }

    /**
     * 获取异常堆栈信息
     * @param ex {@link Throwable}
     * @return 返回堆栈信息
     */
    private String getStackTrace(Throwable ex) {
        StringWriter strWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(strWriter);
        ex.printStackTrace(printWriter);
        printWriter.close();
        return strWriter.toString();
    }

    /**
     * 保存崩溃日志到外部存储器
     * @param errLog 错误日志
     * @return 返回日志保存路径，如果保存失败返回长度为0的字符串("")
     */
    private String saveCrashLog(String errLog) {
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String filename = String.format("crash-%s.log", formatter.format(date));

        // 判断SD卡是否存在并可写入
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(mLogDir);
            if (!dir.exists() && !dir.mkdirs()) {
                new Exception("Log directory is not exist and cannot be created: "
                        + mLogDir).printStackTrace();
                return "";
            }

            File file = new File(dir.getAbsolutePath() + File.separator + filename);
            try {
                Writer writer = new PrintWriter(file);
                writer.write(errLog);
                writer.close();
                return file.getAbsolutePath();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d(LOG_TAG, "external storage can not access!");
        }

        return "";
    }
        /**
     * 如果参数 path 指定的路径中的目录不存在就创建指定目录，确保path路径的父目录存在
     *
     * @param path 绝对路径（包含文件名，例如 '/sdcard/storage/download/test.apk'）
     * @return 如果成功创建目录返回true，否则返回false
     */
    private boolean createDirIfAbsent(String path) {
        String[] array = path.trim().split(File.separator);
        List<String> dirNames = Arrays.asList(array).subList(1, array.length - 1);
        StringBuilder pathBuilder = new StringBuilder(File.separator);
        for (String d : dirNames) {
            pathBuilder.append(d);
            File f = new File(pathBuilder.toString());
            if (!f.exists() && !f.mkdir()) {
                return false;
            }
            pathBuilder.append(File.separator);
        }
        return true;
    }

    /**
     * 指定时间后重启App
     * @param delayMillis 延迟时间，单位毫秒
     * @param cls 重启后跳转到的Activity，显示指定
     */
    public void scheduleRestart(long delayMillis, Class<?> cls) {
        Intent intent = new Intent(mContext, cls);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent,
                Intent.FLAG_ACTIVITY_NEW_TASK);

        AlarmManager alarmManager = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + delayMillis, pendingIntent);
    }

    public interface OnExitingListener {
        void handle(String savePath, String errLog);
    }
}
