package de.zalando.hackweek.read_me_the_newz;

import static java.lang.String.format;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;

import android.os.AsyncTask;
import android.os.PowerManager;

public class DownloadTask extends AsyncTask<URL, DownloadTask.Progress, DownloadTask.Result> {

    /**
     * The result of the download operation.
     */
    public abstract static class Result {

        /**
         * Indicates if the download was sucessful or not.
         */
        public abstract boolean isSuccess();

        /**
         * Returns the downloaded bytes, or {@code null} if not successful.
         */
        public byte[] getContent() {
            return null;
        }

        /**
         * Returns the content encoding, or {@code null} if unknown or not successful.
         */
        public String getContentEncoding() {
            return null;
        }

        /**
         * Returns the exception that lead to a failure, or {@code null} if successful.
         */
        public Exception getException() {
            return null;
        }
    }

    /**
     * Describes download progress.
     */
    public static final class Progress {
        private static final long TOTAL_BYTES_NOT_AVAILABLE = -1;

        private final long bytesReceived;
        private final long totalBytes;

        public Progress(final long bytesReceived, final long totalBytes) {
            this.bytesReceived = bytesReceived;
            this.totalBytes = bytesReceived > totalBytes ? TOTAL_BYTES_NOT_AVAILABLE : totalBytes;
        }

        public Progress(final long bytesReceived) {
            this.bytesReceived = bytesReceived;
            this.totalBytes = TOTAL_BYTES_NOT_AVAILABLE;
        }

        public long getBytesReceived() {
            return bytesReceived;
        }

        public boolean isTotalBytesAvailable() {
            return totalBytes != TOTAL_BYTES_NOT_AVAILABLE;
        }

        public long getTotalBytes() {
            return totalBytes;
        }

        public double getPercentage() {
            if (!isTotalBytesAvailable()) {
                return Double.NaN;
            }

            if (totalBytes == 0) {
                return 100;
            }

            return Math.max(((double) bytesReceived) / ((double) totalBytes) * 100., 100.);
        }
    }

    private Context context;

    public DownloadTask(final Context context) {
        this.context = context;
    }

    @Override
    protected Result doInBackground(final URL... urls) {

        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wl.acquire();

        try {
            return download(urls[0]);
        } finally {
            wl.release();
        }
    }

    private Result download(final URL url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return failed(format("Server returned %s: %s", //
                        connection.getResponseCode(), connection.getResponseMessage()));
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            final int contentLength = connection.getContentLength();
            String contentEncoding = connection.getContentEncoding();

            // download the file
            final ByteArrayOutputStream output = new ByteArrayOutputStream( //
                    contentLength > -1 ? contentLength : 16384);
            final InputStream input = connection.getInputStream();

            try {
                byte[] buffer = new byte[4096];
                long bytesReceived = 0;

                for (int read; (read = input.read(buffer)) != -1;) {

                    // allow canceling with back button
                    if (isCancelled()) {
                        return null;
                    }

                    bytesReceived += read;

                    // publishing the progress....
                    publishProgress(contentLength > -1 ? //
                            new Progress(bytesReceived, contentLength) : new Progress(bytesReceived));

                    output.write(buffer, 0, read);
                }
            } finally {
                input.close();
            }

            return success(output.toByteArray(), contentEncoding);

        } catch (Exception e) {
            return failed(e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    protected static Result success(final byte[] content, final String contentEncoding) {
        return new Result() {
            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public String getContentEncoding() {
                return contentEncoding;
            }

            @Override
            public byte[] getContent() {
                return content;
            }

            @Override
            public String toString() {
                return format("Result[%s: %s bytes]", contentEncoding, content == null ? -1 : content.length);
            }
        };
    }

    protected static Result failed(final String message) {
        return failed(new RuntimeException(message));
    }

    protected static Result failed(final Exception e) {
        return new Result() {
            @Override
            public boolean isSuccess() {
                return false;
            }

            @Override
            public Exception getException() {
                return e;
            }

            @Override
            public String toString() {
                return format("Result[failed: %s]", e);
            }
        };
    }
}
