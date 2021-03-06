package com.honey.networkconnect;

import android.net.NetworkInfo;

public interface DownloadCallback {

    interface Progress {
        int ERROR = -1;
        int CONNECT_SUCCESS = 0;
        int GET_INPUT_STREAM_SUCCESS = 1;
        int PROGRESS_INPUT_STREAM_IN_PROGRESS = 2;
        int PROGRESS_INPUT_STREAM_SUCCESS = 3;
    }

    void updateFromDownload(String result);

    NetworkInfo getActiveNetwrokInfo();

    void onProgressUpdate(int progressCode, int percentComplete);

    void finishDownloading();
}
