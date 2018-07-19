package com.mercadopago.android.px.callbacks;

import android.support.annotation.NonNull;

import com.mercadopago.android.px.model.Discount;

public interface OnViewUpdated {
    void onSuccess(@NonNull final Discount discount);

    void onFailure();
}
