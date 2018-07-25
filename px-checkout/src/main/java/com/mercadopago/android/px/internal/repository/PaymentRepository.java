package com.mercadopago.android.px.internal.repository;

import android.support.annotation.NonNull;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.services.adapters.MPCall;
import java.util.Map;

public interface PaymentRepository {

    @NonNull
    MPCall<Payment> doPayment();

    @NonNull
    String getTransactionId();

    @NonNull
    Map<String, Object> getPayload();

    @NonNull
    Map<String,String> getQueryParams();
}
