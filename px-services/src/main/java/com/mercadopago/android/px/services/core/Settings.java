package com.mercadopago.android.px.services.core;

import okhttp3.logging.HttpLoggingInterceptor;

public class Settings {
    public static final HttpLoggingInterceptor.Level OKHTTP_LOGGING = HttpLoggingInterceptor.Level.NONE;
    public static String servicesVersion = "v1";
    public static final String PAYMENT_RESULT_API_VERSION = "1.4";
    public static final String PAYMENT_METHODS_OPTIONS_API_VERSION = "1.6";

    @Deprecated
    public static void enableBetaServices() {
        servicesVersion = "beta";
    }
}