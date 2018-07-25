package com.mercadopago.android.px.internal.datasource;

import android.support.annotation.NonNull;
import com.mercadopago.android.px.internal.repository.PaymentRepository;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.services.CheckoutService;
import com.mercadopago.android.px.services.adapters.MPCall;
import retrofit2.Retrofit;

public class PaymentApi {

    //TODO
    private static final String MP_PAYMENT_URI = "";

    @NonNull private final CustomService customService;
    @NonNull private final PaymentRepository paymentRepository;

    public PaymentApi(@NonNull final Retrofit retrofit,
        @NonNull final PaymentRepository paymentRepository) {
        customService = retrofit.create(CustomService.class);
        this.paymentRepository = paymentRepository;
    }

    public MPCall<Payment> doPayment() {
        return customService.createPayment(paymentRepository.getTransactionId(), MP_PAYMENT_URI,
            paymentRepository.getPayload(), paymentRepository.getQueryParams());
    }
}
