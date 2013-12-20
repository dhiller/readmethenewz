package de.zalando.hackweek.read_me_the_newz;

import android.content.Context;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.util.Log;
import com.google.common.collect.ImmutableSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Downloads a URL and returns the downloaded content as a byte array. Subclasses have to override
 * {@link #getUrl getUrl()} in order to provide an URL for an item of type {@code T}.
 *
 * @param  <T>  item type to download
 */
public abstract class DownloadTask<T> extends AsyncTask<T, DownloadTask.Progress, DownloadTask.Result<T>> {

    /**
     * The result of the download operation.
     */
    public abstract static class Result<T> {

        private final T item;

        protected Result(final T item) {
            this.item = item;
        }

        /**
         * Returns the item for this result.
         */
        public T getItem() {
            return item;
        }

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
         * Returns the content type, or {@code null} if unknown or not successful.
         */
        public String getContentType() {
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

    private static final String ID = DownloadTask.class.getSimpleName();

    private Context context;

    private volatile int bufferSize = 8192;

    public DownloadTask(final Context context) {
        this.context = context;
    }

    /** Sets the internal buffer size to use for the download. */
    public DownloadTask<T> withBufferSize(final int bufferSize) {
        checkArgument(bufferSize > 0, "buffer size must be positive: %s", bufferSize);
        this.bufferSize = bufferSize;
        return this;
    }

    @Override
    protected Result<T> doInBackground(final T... items) {

        if (items == null || items.length < 1) {
            return null;
        }

        if (items.length > 1) {
            Log.w(ID, "Ignoring all download items except the first.");
        }

        // take CPU lock to prevent CPU from going off if the user
        // presses the power button during download
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());

        wl.acquire();
        try {
            return download(items[0]);
        } finally {
            wl.release();
        }
    }

    /**
     * Provides the download URL for a given {@code item}.
     */
    protected abstract URL getUrl(T item);

    private Result<T> download(final T item) {
        HttpURLConnection connection = null;
        try {
            final URL url = getUrl(item);
            connection = resolveRedirections(url);

            // expect HTTP 200 OK, so we don't mistakenly save error report
            // instead of the file
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return failed(item, format("Server returned %s: %s (%s)",
                        connection.getResponseCode(), connection.getResponseMessage(), url));
            }

            // this will be useful to display download percentage
            // might be -1: server did not report the length
            final int contentLength = connection.getContentLength();
            final String contentType= connection.getContentType();

            Log.d(ID, "Starting to download " + contentLength + " bytes (" + connection.getHeaderFields() + ") from " + url);

            // download the file
            final ByteArrayOutputStream output = new ByteArrayOutputStream( //
                    contentLength > -1 ? Math.max(contentLength, bufferSize): bufferSize);
            final InputStream input = connection.getInputStream();

            try {
                // hack to detect transparent compression
                boolean transparentlyDecompressing = isTransparentlyDecompressing(input);
                boolean totalBytesAvailable = !transparentlyDecompressing && contentLength > -1;

                byte[] buffer = new byte[bufferSize];
                long bytesReceived = 0;

                for (int read; (read = input.read(buffer)) != -1;) {

                    // allow canceling with back button
                    if (isCancelled()) {
                        return null;
                    }

                    bytesReceived += read;

                    // publishing the progress....
                    publishProgress(totalBytesAvailable ?
                            new Progress(bytesReceived, contentLength) : new Progress(bytesReceived));

                    output.write(buffer, 0, read);
                }
            } finally {
                input.close();
            }

            return success(item, output.toByteArray(), contentType);

        } catch (Exception e) {
            return failed(item, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static HttpURLConnection resolveRedirections(final URL url) throws IOException {
        return resolveRedirections(url, Collections.<String>emptySet());
    }

    private static HttpURLConnection resolveRedirections(final URL url, final Set<String> locations) throws IOException {
        boolean disconnect = true;

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            Log.d(ID, "Connecting to " + url);
            connection.setInstanceFollowRedirects(false);
            connection.connect();

            switch (connection.getResponseCode()) {
                case HttpURLConnection.HTTP_OK: 
                    disconnect = false;
                    return connection;
                case HttpURLConnection.HTTP_MOVED_PERM:
                case HttpURLConnection.HTTP_MOVED_TEMP:
                case HttpURLConnection.HTTP_SEE_OTHER:
                case 307: // temporary redirect
                    break;
                
                default:
                    throw new IOException(format("Server returned %s: %s", //
                            connection.getResponseCode(), connection.getResponseMessage()));                    
            }

            final String location = checkNotNull(connection.getHeaderField("Location"));

            Log.d(ID, url + " redirects to " + location);
            connection.disconnect();
            disconnect = false;

            if (locations.contains(location)) {
                throw new IOException("Cyclic redirects: " + locations);
            }

            return resolveRedirections(new URL(location),
                    ImmutableSet.<String>builder().addAll(locations).add(location).build());
        } finally {
            if (disconnect) {
                connection.disconnect();
            }
        }
    }

    /**
     * Hack to determine if it's a transparently decompressing {@code InputStream}.
     */
    private static boolean isTransparentlyDecompressing(final InputStream input) {
        final String name = input.getClass().getName();
        return name.contains("GZIP") || name.contains("Deflate");
    }

    protected static <T> Result<T> success(final T item, final byte[] content, final String contentType) {
        return new Result<T>(item) {
            @Override
            public boolean isSuccess() {
                return true;
            }

            @Override
            public String getContentType() {
                return contentType;
            }

            @Override
            public byte[] getContent() {
                return content;
            }

            @Override
            public String toString() {
                return format("Result[%s: %s bytes]", contentType, content == null ? -1 : content.length);
            }
        };
    }

    protected static <T> Result<T> failed(final T item, final String message ) {
        return failed(item, new RuntimeException(message));
    }

    protected static <T> Result<T> failed(final T item, final Exception e) {
        return new Result<T>(item) {
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
