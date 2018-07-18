package com.mercadopago.android.px.tracking.internal.services;

import android.content.Context;
import com.mercadopago.android.px.tracking.internal.model.EventTrackIntent;
import com.mercadopago.android.px.tracking.internal.model.PaymentIntent;
import com.mercadopago.android.px.tracking.internal.model.TrackingIntent;
import retrofit2.Callback;

/**
 * Created by vaserber on 7/3/17.
 */

public interface MPTrackingService {

    void trackToken(TrackingIntent trackingIntent, Context context);

    void trackPaymentId(PaymentIntent paymentIntent, Context context);

    void trackEvents(String publicKey, EventTrackIntent eventTrackIntent, Context context);

    void trackEvents(String publicKey, EventTrackIntent eventTrackIntent, Context context, Callback<Void> callback);
}
