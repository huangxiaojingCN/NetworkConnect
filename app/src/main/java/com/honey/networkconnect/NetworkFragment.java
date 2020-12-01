package com.honey.networkconnect;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class NetworkFragment extends Fragment {

    private static final String TAG = "NetworkFragment";
    private static final String URL_KEY = "UrlKey";

    private String mUrlString;
    private DownloadCallback mCallback;
    private DownloadTask mDownloadTask;

    public static NetworkFragment getInstance(FragmentManager fragmentManager, String url) {
        NetworkFragment networkFragment = (NetworkFragment) fragmentManager
                .findFragmentByTag(NetworkFragment.TAG);
        if (networkFragment == null) {
            networkFragment = new NetworkFragment();
            Bundle args = new Bundle();
            args.putString(URL_KEY, url);
            networkFragment.setArguments(args);
            fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        }
        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mUrlString = getArguments().getString(URL_KEY);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mCallback = (DownloadCallback) context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallback = null;
    }

    @Override
    public void onDestroy() {
        cancleDownload();
        super.onDestroy();
    }

    public void startDownload() {
        cancleDownload();
        mDownloadTask = new DownloadTask();
        mDownloadTask.execute(mUrlString);
    }

    public void cancleDownload() {
        if (mDownloadTask != null) {
            mDownloadTask.cancel(true);
            mDownloadTask = null;
        }
    }

    private class DownloadTask extends AsyncTask<String, Integer, DownloadTask.Result> {

        @Override
        protected void onPreExecute() {
            if (mCallback != null) {
                NetworkInfo netwrokInfo = mCallback.getActiveNetwrokInfo();
                if (netwrokInfo == null || !netwrokInfo.isConnected() ||
                        (netwrokInfo.getType() != ConnectivityManager.TYPE_WIFI
                        && netwrokInfo.getType() !=  ConnectivityManager.TYPE_MOBILE)) {
                    mCallback.updateFromDownload(null);
                    cancel(true);
                }
            }
        }

        @Override
        protected Result doInBackground(String... urls) {
            Result result = null;
            if (!isCancelled() && urls != null && urls.length > 0) {
                String urlString = urls[0];
                try {
                    URL url = new URL(urlString);
                    String resultString = downloadUrl(url);
                    if (resultString != null) {
                        result = new Result(resultString);
                    } else {
                        throw new IOException("No response received.");
                    }
                } catch (Exception e) {
                    result = new Result(e);
                }
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (values.length >= 2) {
                mCallback.onProgressUpdate(values[0], values[1]);
            }
        }

        @Override
        protected void onPostExecute(Result result) {
            if (result != null && mCallback != null) {
                if (result.mException != null) {
                    mCallback.updateFromDownload(result.mException.getMessage());
                } else if (result.mResultValue != null) {
                    mCallback.updateFromDownload(result.mResultValue);
                }
                mCallback.finishDownloading();
            }
        }

        private String downloadUrl(URL url) throws IOException {
            InputStream is = null;
            HttpURLConnection connection = null;
            String result = null;
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5000);
                connection.setConnectTimeout(5000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                // 通知连接成功
                publishProgress(DownloadCallback.Progress.CONNECT_SUCCESS, 0);
                int responseCode = connection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("Http error code: " + responseCode);
                }

                is = connection.getInputStream();
                publishProgress(DownloadCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
                if (is != null) {
                    result = readStream(is);
                    publishProgress(DownloadCallback.Progress.PROGRESS_INPUT_STREAM_SUCCESS, 0);
                }
            } finally {
                if (is != null) {
                    is.close();
                }
                if (connection != null) {
                    connection.disconnect();                }
            }

            return result;
        }

        private String readStream(InputStream is) throws IOException {
            String result = null;

            InputStreamReader streamReader = new InputStreamReader(is, "UTF-8");
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuffer buffer = new StringBuffer();
            try {
                int progress = 0;
                while ((result = reader.readLine()) != null) {
                    buffer.append(result);
                    progress++;
                    onProgressUpdate(DownloadCallback.Progress.PROGRESS_INPUT_STREAM_IN_PROGRESS, progress);
                }   
            } finally {
                reader.close();
            }
            
            return buffer.toString();
        }

        public class Result {
            public String mResultValue;
            public Exception mException;

            public Result(String resultValue) {
                this.mResultValue = resultValue;
            }

            public Result(Exception e) {
                this.mException = e;
            }
        }
    }
}
