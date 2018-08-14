package com.mercadopago.android.px.internal.driver;

import com.mercadopago.android.px.model.PaymentMethods;
import java.util.ArrayList;
import java.util.List;

public class GuessingCardDriver {

    private final boolean isOnlyGuessing;

    public GuessingCardDriver(final boolean isOnlyGuessing) {

        this.isOnlyGuessing = isOnlyGuessing;
    }

    public List<PaymentMethods> getPaymentMethods() {
        //si es uno o el otro llama distinto.
        return new ArrayList<>();
    }

    public void drive(final Handler handler) {
        if (isOnlyGuessing) {
            handler.onGuessingCardFinished();
        } else {
            handler.onIssuerRequiered();
        }
        //Aca hay q elegir bien si va para q lao.
    }

    public interface Handler {
        void onGuessingCardFinished();

        void onIssuerRequiered();
    }
}
