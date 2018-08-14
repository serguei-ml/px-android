package com.mercadopago.android.px.core;

import android.app.Activity;
import com.mercadopago.android.px.GuessingCardActivity;

public class CardFlow {

    public CardFlow(/*los campos obligatorios para decidir site, y toda la*/) {

    }

    public void start(final Activity activity, final int resCode) {
        //Aca preseteas toda la en cache.
        GuessingCardActivity.start(activity, resCode);
    }
}
