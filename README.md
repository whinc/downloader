# downloader

完整使用示例:
```
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
```