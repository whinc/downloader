package com.whinc.example;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.whinc.downloader.DownloadListenerAdapter;
import com.whinc.downloader.Downloader;
import com.whinc.downloader.DownloaderFactory;

import java.io.File;
import java.io.IOException;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by wuhui on 9/6/15.
 */

public class DownloadActivity extends AppCompatActivity {
    public static final String TAG = DownloadActivity.class.getSimpleName();
    @Bind(R.id.download_url_editText)
    EditText mDownloadUrlEditText;
    @Bind(R.id.download_button)
    Button mDownloadButton;
    @Bind(R.id.progressBar)
    ProgressBar mProgressBar;
    @Bind(R.id.percent_textView)
    TextView mPercentTextView;

    public static void startActivity(Context context) {
        Intent intent = new Intent(context, DownloadActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);
        ButterKnife.bind(this);
        CrashHandler.with(this).setOnExitingListener(new CrashHandler.OnExitingListener() {
            @Override
            public void handle(String savePath, String errLog) {
                Log.i(TAG, errLog);
            }
        });
    }

    @OnClick(R.id.download_button)
    void download() {
        String url = mDownloadUrlEditText.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            return;
        }

        if (StorageUtils.isExternalStorageMounted()) {
            processDownload(url);
        } else {
            Toast.makeText(this, "sdcard is not accessible", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateProgress(float percent) {
        mPercentTextView.setText(String.format("%.2f%%", percent * 100.0f));
        mProgressBar.setProgress((int) (percent * mProgressBar.getMax()));
    }

    private void processDownload(String url) {
        Downloader downloader = DownloaderFactory.create(this);
        downloader.setDownloadListener(new DownloadListenerAdapter() {
            @Override
            public void onCompleted() {
                super.onCompleted();
                Log.i(TAG, "completed");
            }

            @Override
            public void onSuccessful(Uri downloadedFile) {
                Log.i(TAG, "downloaded file:" + downloadedFile);
                Toast.makeText(DownloadActivity.this, "Download Completed", Toast.LENGTH_SHORT).show();
                updateProgress(1.0f);
            }

            @Override
            public void onFailed(int reasonCode, String reason) {
                super.onFailed(reasonCode, reason);
                Log.i(TAG, "failed reason:" + reason);
            }

            @Override
            public void onPaused(int reasonCode, String reason) {
                super.onPaused(reasonCode, reason);
                Log.i(TAG, "pause reason:" + reason);
            }

            @Override
            public void onPending() {
                super.onPending();
                Log.i(TAG, "pending");
            }

            @Override
            public void onRunning(int downloadedBytes, int totalBytes) {
                super.onRunning(downloadedBytes, totalBytes);
                float percent = downloadedBytes * 1.0f / totalBytes;
                Log.i(TAG, String.format("%d/%d(%.2f)", downloadedBytes, totalBytes, percent));
                updateProgress(percent);
            }
        });
        try {
            File saveFile = StorageUtils.createRelativeFile("whinc/download/data.app");
            downloader.setTitle("test title")
                    .setDescription("test description")
                    .setNotificationVisible(false)
                    .download(url, saveFile);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "sdcard cannot access", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "cannot download url:" + url, Toast.LENGTH_SHORT).show();
        }
    }
}
