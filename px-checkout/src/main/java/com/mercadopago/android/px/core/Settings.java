package com.mercadopago.android.px.core;

import com.mercadopago.android.px.tracking.internal.constants.TrackingEnvironments;

public class Settings {

    private static String trackingEnvironment = TrackingEnvironments.PRODUCTION;
    public static String eventsTrackingVersion = "2";
    public static String servicesVersion = "v1";

    public static void setTrackingEnvironment(final String mode) {
        trackingEnvironment = mode;
    }

    public static String getTrackingEnvironment() {
        return trackingEnvironment;
    }

    public static void enableBetaServices() {
        servicesVersion = "beta";
    }

}