package com.mercadopago.android.px.internal.datasource;

import android.support.annotation.NonNull;
import com.mercadopago.android.px.internal.repository.PaymentRepository;
import com.mercadopago.android.px.model.Payment;
import com.mercadopago.android.px.services.adapters.MPCall;
import java.util.Map;

public class PaymentService implements PaymentRepository {


    private PaymentApi paymentApi;

    public PaymentService(@NonNull final PaymentApi paymentApi) {
        this.paymentApi = paymentApi;
    }

    @NonNull
    @Override
    public MPCall<Payment> doPayment() {
        //TODO ir validando que tengo lo necesario para hacer el pago

        //TODO ver si el pago se tiene que hacer con procesadora


        //Al final hacer el pago normal
        return paymentApi.doPayment();
    }

    @NonNull
    @Override
    public String getTransactionId() {
        return null;
    }

    @NonNull
    @Override
    public Map<String, Object> getPayload() {
        return null;
    }

    @NonNull
    @Override
    public Map<String, String> getQueryParams() {
        return null;
    }
}
