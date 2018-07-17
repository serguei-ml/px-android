package com.mercadopago.android.px.tracking.utils;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import com.mercadopago.android.px.tracking.core.Settings;
import com.mercadopago.android.px.tracking.core.TLSSocketFactory;
import java.io.File;
import java.util.concurrent.TimeUnit;
import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class HttpClientUtil {

    private static final int TIMEOUT = 20;
    private static final int TIMEOUT_WRITE = 20;
    private static final int TIMEOUT_READ = 20;
    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final String CACHE_DIR_NAME = "PX_OKHTTP_CACHE_TRACKING";

    private static OkHttpClient client;

    private HttpClientUtil() {
    }

    public static synchronized OkHttpClient getClient(final Context context) {
        if (client == null) {
            client = createClient(context);
        }
        return client;
    }

    @NonNull
    private static OkHttpClient createClient(@NonNull final Context context) {
        // Set log info
        final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(Settings.OKHTTP_LOGGING);

        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_WRITE, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_READ, TimeUnit.SECONDS)
            .addInterceptor(interceptor);

        try {
            final Cache cache =
                new okhttp3.Cache(new File(String.format("%s%s", context.getCacheDir().getPath(), CACHE_DIR_NAME)),
                    CACHE_SIZE);
            builder.cache(cache);
        } catch (final Exception e) {
            // do nothing
        }

        // Set client
        client = builder.build();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            try {
                client = TLSSocketFactory.enforceTls(client);
            } catch (final Exception e) {
                // Do nothing
            }
        }

        return client;
    }
}
